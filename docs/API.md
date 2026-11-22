<!--
SPDX-FileCopyrightText: 2026 Dédale Solutions
SPDX-License-Identifier: EUPL-1.2
-->

# API REST d'identity-manager

Base : `http://localhost:8083`. Authentification : `Authorization: Bearer <jwt>` (jeton émis par arc-auth, claim `roles`). Lecture : `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` ; écriture : `ARC_PUBLISHER` ou `ARC_ADMIN`.

Codes d'erreur communs : `401` (jeton absent ou invalide), `403` (rôle insuffisant), `404` (ressource inconnue), `400` (requête invalide), `409` (doublon).

## Identités

Représentation :

```json
{ "employeeId": "I000042", "firstName": "Alice", "lastName": "Martin", "email": "alice@example.org" }
```

| Endpoint | Corps | Réponse |
|---|---|---|
| `GET /identities` | | `200` liste |
| `GET /identities/{employeeId}` | | `200`, `404` |
| `GET /identities/{employeeId}/accounts` | | `200` comptes de l'identité, `404` |
| `POST /identities` | `firstName`, `lastName`, `email` (le matricule `I<DDDDDD>` est généré) | `201` + `Location` + identité avec son matricule |
| `PUT /identities/{employeeId}` | identité | `200` (le matricule n'est pas modifiable), `404` |
| `DELETE /identities/{employeeId}` | | `204`, `404` ; les comptes liés deviennent non corrélés |

## Types de compte

`GET /account-types` : types `USER` configurés.

```json
[{ "name": "admin", "kind": "USER", "pattern": "A(?<number>\\d{6})",
   "metadata": [{ "name": "department", "type": "STRING", "required": false }],
   "identityTemplate": "I{number}", "identifierTemplate": "A{number}" }]
```

## Contrats

`GET /contract-types` : types de contrat configurés (`name`, `metadata`).

| Endpoint | Corps | Réponse |
|---|---|---|
| `GET /contracts` | filtres `identity`, `type` | `200` liste |
| `GET /identities/{employeeId}/contracts` | | `200`, `404` |
| `POST /identities/{employeeId}/contracts` | `type`, `startDate`, `endDate`, `metadata` | `201`, `400` (type ou métadonnées invalides), `404` |
| `PUT /identities/{employeeId}/contracts/{id}` | idem | `200`, `400`, `404` |
| `DELETE /identities/{employeeId}/contracts/{id}` | | `204`, `404` |

## Comptes

Représentation :

```json
{ "identifier": "A000042", "type": "admin", "identity": "I000042",
  "source": "API", "metadata": { "department": "IT" }, "startDate": null, "endDate": null, "orderId": "…", "endOrderId": null, "createdAt": "…" }
```

`identity` est `null` pour un compte non corrélé ; `source` vaut `API` ou `DISCOVERED`.

| Endpoint | Corps | Réponse |
|---|---|---|
| `GET /accounts?identity=&type=&uncorrelated=` | | `200` |
| `GET /accounts/{identifier}` | | `200`, `404` |
| `POST /accounts` | `{ "identifier", "type"?, "identity"?, "metadata"?, "startDate"?, "endDate"? }` | `201` ; le type et l'identité sont déduits par la regex si absents ; ordre `CREATE` publié (différé à `startDate` si elle est future) ; `400` (aucun type ne correspond, métadonnées invalides), `404` (identité inconnue), `409` |
| `PUT /accounts/{identifier}/metadata` | `{ "<champ>": valeur }` | `200`, `400`, `404` |
| `PUT /accounts/{identifier}/identity` | `{ "identity": "e1234" }` | `200`, `404` |
| `POST /accounts/correlate` | | `200` `{ "correlated": n }` |
| `PUT /accounts/{identifier}/dates` | `{ "startDate"?, "endDate"? }` (dates `AAAA-MM-JJ`) | `200`, `400`, `404` ; l'ordre en attente est annulé puis replanifié |
| `POST /accounts/{identifier}/cancel-pending` | | `200`, `404`, `409` (aucun ordre en attente) |
| `DELETE /accounts/{identifier}` | | `204` (ordre `DELETE` publié, ou ordre en attente annulé), `404` |

## Ordre de provisioning

Message de la file `arc.orders` (clé = identifiant de l'ordre) ou `POST /orders` d'arc-publisher : `targetType=IDENTITY`, `action=CREATE|DELETE`, `identityId=<identifier>`, `accountType`, `source=identity-manager`, `attributes` : `employeeId` (si corrélé), `metadata.<champ>`.

Temporalité : `CREATE` est exécuté à `startDate` (immédiatement si absente ou atteinte), `DELETE` au lendemain de `endDate`. Un ordre non encore exécuté est annulé par un message `CANCEL` (ou `DELETE /orders/{id}`).
