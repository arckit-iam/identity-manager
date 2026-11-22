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
{ "employeeId": "e1234", "firstName": "Alice", "lastName": "Martin", "email": "alice@example.org" }
```

| Endpoint | Corps | Réponse |
|---|---|---|
| `GET /identities` | | `200` liste |
| `GET /identities/{employeeId}` | | `200`, `404` |
| `GET /identities/{employeeId}/accounts` | | `200` comptes de l'identité, `404` |
| `POST /identities` | identité | `201` + `Location`, `400` (matricule vide), `409` (matricule existant) |
| `PUT /identities/{employeeId}` | identité | `200` (le matricule n'est pas modifiable), `404` |
| `DELETE /identities/{employeeId}` | | `204`, `404` ; les comptes liés deviennent non corrélés |

## Types de compte

`GET /account-types` : types `USER` configurés.

```json
[{ "name": "employee", "kind": "USER", "pattern": "(?<identity>[a-z]+[0-9]+)(-adm)?",
   "metadata": [{ "name": "department", "type": "STRING", "required": true }] }]
```

## Comptes

Représentation :

```json
{ "identifier": "e1234-adm", "type": "employee", "identity": "e1234",
  "source": "API", "metadata": { "department": "IT" }, "jobId": "…", "createdAt": "…" }
```

`identity` est `null` pour un compte non corrélé ; `source` vaut `API` ou `DISCOVERED`.

| Endpoint | Corps | Réponse |
|---|---|---|
| `GET /accounts?identity=&type=&uncorrelated=` | | `200` |
| `GET /accounts/{identifier}` | | `200`, `404` |
| `POST /accounts` | `{ "identifier", "type"?, "identity"?, "metadata"? }` | `201` ; le type et l'identité sont déduits par la regex si absents ; job `CREATE` publié ; `400` (aucun type ne correspond, métadonnées invalides), `404` (identité inconnue), `409` |
| `PUT /accounts/{identifier}/metadata` | `{ "<champ>": valeur }` | `200`, `400`, `404` |
| `PUT /accounts/{identifier}/identity` | `{ "identity": "e1234" }` | `200`, `404` |
| `POST /accounts/correlate` | | `200` `{ "correlated": n }` |
| `DELETE /accounts/{identifier}` | | `204` (job `DELETE` publié), `404` |

## Job de provisioning

`POST /jobs` d'arc-publisher : `targetType=IDENTITY`, `action=CREATE|DELETE`, `identityId=<identifier>`, `source=identity-manager`, `attributes` : `accountType`, `employeeId` (si corrélé), `metadata.<champ>`.
