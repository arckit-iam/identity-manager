# Identity Manager

Service de gestion des **identités humaines** et de leurs **comptes utilisateurs** (comptes nominatifs, administrateurs...) de la plateforme [Instant Access Provider](https://github.com/arckit-iam/InstantAccessPublisher).

Chaque compte est corrélé à une identité humaine (matricule) grâce à une regex portée par son **type de compte**. Les comptes arrivent par l'API (un **ordre** de provisioning est alors déposé dans la file `arc.orders` consommée par [arc-publisher](https://github.com/arckit-iam/arc-publisher), ou envoyé à son API) ou par **découverte** à partir des événements consolidés (`arc.sync.identity`). Le pendant pour les comptes non utilisateurs (comptes de service) est [product-manager](https://github.com/arckit-iam/product-manager).

```text
Client ──► POST /accounts ──► identity-manager (PostgreSQL)
                                   │ ordre IDENTITY / CREATE (Kafka arc.orders, ou POST /orders)
                                   ▼
                             arc-publisher ──► Kafka ──► agent ──► annuaire

arc-consolidator ──► Kafka arc.sync.identity ──► identity-manager (découverte, regex)
```

## Stack

- Java 25, Spring Boot 3.5
- PostgreSQL, migrations Flyway, métadonnées en `JSONB`
- Spring Security (jetons JWT émis par arc-auth), spring-kafka
- Maven, Docker

## Architecture hexagonale

```text
secop.idm1/
  domain/
    model/      Identity, Account, AccountType, AccountKind, MetadataField...
    port/       IdentityProvider, AccountProvider, OrderPublisher, AccountTypeCatalog
    service/    IdentityManager, AccountManager (logique métier)
    exception/  exceptions métier
  adapters/
    expose/     contrôleurs REST, DTO, mapper, sécurité
    provide/    persistance JPA et implémentations des services du domaine
    publish/    KafkaOrderPublisher (canal par défaut, file arc.orders) et ArcPublisherOrderAdapter (canal API)
    config/     types de compte définis par la configuration du déploiement
    consume/    AccountDiscoveryListener : découverte via Kafka (arc.sync.identity)
```

Les adapters implémentent les ports définis dans le domaine ; le domaine ne dépend d'aucun framework.

## API REST

Toutes les routes exigent un jeton `Authorization: Bearer <jwt>` émis par [arc-auth](https://github.com/arckit-iam/arc-auth), sauf `/actuator/health` (public, sondes). Sans jeton valide : `401`. Avec un rôle insuffisant : `403`. Le détail est dans [docs/API.md](docs/API.md).

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| `GET` | `/identities` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Liste les identités |
| `GET` | `/identities/{employeeId}` | idem | Retourne une identité |
| `GET` | `/identities/{employeeId}/accounts` | idem | Comptes d'une identité |
| `POST` | `/identities` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Crée une identité (prénom, nom, e-mail) ; le matricule `I<DDDDDD>` est généré |
| `PUT` | `/identities/{employeeId}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Met à jour nom, prénom, e-mail |
| `DELETE` | `/identities/{employeeId}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Supprime l'identité (ses comptes deviennent non corrélés) |
| `GET` | `/contract-types` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Types de contrat configurés |
| `GET` | `/contracts` | idem | Liste (filtres `identity`, `type`) |
| `GET` | `/identities/{employeeId}/contracts` | idem | Contrats d'une identité |
| `POST` | `/identities/{employeeId}/contracts` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Ajoute un contrat (`type`, `startDate`, `endDate`, `metadata`) |
| `PUT` | `/identities/{employeeId}/contracts/{id}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Met à jour un contrat |
| `DELETE` | `/identities/{employeeId}/contracts/{id}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Supprime un contrat |
| `GET` | `/account-types` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Types de compte utilisateur configurés |
| `GET` | `/accounts` | idem | Liste (filtres `identity`, `type`, `uncorrelated=true`) |
| `GET` | `/accounts/{identifier}` | idem | Retourne un compte |
| `POST` | `/accounts` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Crée un compte (ordre CREATE, différé à `startDate` si elle est future) ; identifiant facultatif si `type` et `identity` sont fournis (généré par gabarit) |
| `PUT` | `/accounts/{identifier}/dates` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Modifie `startDate` / `endDate` (dernier jour inclus) ; l'ordre en attente est annulé puis replanifié |
| `POST` | `/accounts/{identifier}/cancel-pending` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Annule l'ordre planifié non encore exécuté (`409` si aucun n'est en attente) |
| `PUT` | `/accounts/{identifier}/metadata` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Remplace les métadonnées |
| `PUT` | `/accounts/{identifier}/identity` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Corrèle manuellement le compte à une identité |
| `POST` | `/accounts/correlate` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Rejoue les regex sur les comptes non corrélés |
| `DELETE` | `/accounts/{identifier}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Supprime le compte (ordre DELETE, planifié au lendemain de `endDate` si elle est définie ; un ordre en attente est annulé) |

Tout autre chemin est refusé.

## Comptes utilisateurs

Un **type de compte** est défini par le déploiement (Helm/variables d'environnement), sans modifier le code :

- `name` : nom du type ;
- `kind` : `USER` (géré ici) ou `NON_USER` (géré par product-manager, ignoré ici) ;
- `pattern` : regex à correspondance complète ; le groupe nommé `identity` désigne directement le matricule ;
- `identityTemplate` (facultatif) : matricule déduit des groupes capturés, ex. `I{number}` ;
- `identifierTemplate` (facultatif) : identifiant généré à la création d'un compte pour une identité, ex. `A{number}` (`{identity}` = matricule, `{number}` = ses chiffres) ;
- `metadata` : champs autorisés (`name`, `type` = `STRING`/`NUMBER`/`BOOLEAN`, `required`).

Les métadonnées sont stockées en `JSONB` (colonne `account.metadata`, index GIN) : ajouter un champ ne nécessite ni migration ni changement de modèle ; un champ inconnu, manquant (obligatoire) ou mal typé est refusé (`400`).

Le **matricule** est généré à la création d'une identité (`I` + 6 chiffres, séquence PostgreSQL ; préfixe `ARC_IDENTITY_EMPLOYEE_ID_PREFIX`). Exemple : un compte `standard` porte le matricule (`I000042`), un compte `admin` porte `A000042` et est corrélé à l'identité `I000042`.

```yaml
ARC_ACCOUNTS_TYPES_0_NAME: standard
ARC_ACCOUNTS_TYPES_0_KIND: USER
ARC_ACCOUNTS_TYPES_0_PATTERN: '(?<identity>I\d{6})'
ARC_ACCOUNTS_TYPES_0_IDENTIFIERTEMPLATE: '{identity}'
ARC_ACCOUNTS_TYPES_1_NAME: admin
ARC_ACCOUNTS_TYPES_1_KIND: USER
ARC_ACCOUNTS_TYPES_1_PATTERN: 'A(?<number>\d{6})'
ARC_ACCOUNTS_TYPES_1_IDENTITYTEMPLATE: 'I{number}'
ARC_ACCOUNTS_TYPES_1_IDENTIFIERTEMPLATE: 'A{number}'
```

## Contrats

Une identité porte des **contrats** (prestataire, RH, stagiaire...). Les types de contrat sont configurables, avec des métadonnées typées stockées en `JSONB` et validées comme celles des comptes :

```yaml
ARC_CONTRACTS_TYPES_0_NAME: contractor
ARC_CONTRACTS_TYPES_0_METADATA_0_NAME: company
ARC_CONTRACTS_TYPES_0_METADATA_0_TYPE: STRING
ARC_CONTRACTS_TYPES_0_METADATA_0_REQUIRED: 'true'
```

Un compte découvert dont l'identité n'existe pas encore reste non corrélé jusqu'à `POST /accounts/correlate`.

## Configuration

| Variable | Description |
|---|---|
| `SERVER_PORT` | Port HTTP (défaut `8083`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (défaut) ou `prod` |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Connexion PostgreSQL |
| `SPRING_FLYWAY_URL` / `_USER` / `_PASSWORD` / `_ENABLED` | Migrations Flyway (désactivées par défaut en `dev`) |
| `ARC_SECURITY_ISSUER_URI` | Valeur attendue du claim `iss` (obligatoire ; égale à `ARC_AUTH_JWT_ISSUER` d'arc-auth) |
| `ARC_SECURITY_JWK_SET_URI` | Clés publiques d'arc-auth (défaut `http://arc-auth:8082/.well-known/jwks.json`) |
| `ARC_SECURITY_AUDIENCE` | Valeur attendue du claim `aud` (défaut `arc`) |
| `ARC_AUTH_URL` | URL d'arc-auth (défaut `http://arc-auth:8082`) |
| `ARC_AUTH_USERNAME` / `_PASSWORD` | Compte de service (membre de `arc-publishers`) avec lequel identity-manager appelle arc-publisher (canal `api` uniquement) |
| `ARC_ORDERS_CHANNEL` | Canal des ordres : `kafka` (défaut, file `arc.orders`) ou `api` (REST vers arc-publisher) |
| `ARC_ORDERS_TOPIC` | Topic des ordres (défaut `arc.orders`) |
| `ARC_ORDERS_ZONE` | Fuseau qui définit les jours de `startDate` / `endDate` (défaut `UTC`) |
| `ARC_PUBLISHER_URL` | URL d'arc-publisher, canal `api` uniquement (défaut `http://arc-publisher:8081`) |
| `ARC_PUBLISHER_TARGET_SYSTEM` | Système cible des ordres (défaut `ad://DC01`) |
| `ARC_PUBLISHER_SOURCE` | Valeur du champ `source` des ordres (défaut `identity-manager`) |
| `ARC_IDENTITY_EMPLOYEE_ID_PREFIX` | Préfixe des matricules générés (défaut `I`) |
| `ARC_ACCOUNTS_TYPES_<n>_*` | Types de compte (voir ci-dessus) |
| `ARC_CONTRACTS_TYPES_<n>_*` | Types de contrat (voir ci-dessus) |
| `ARC_DISCOVERY_ENABLED` | Active la découverte Kafka (défaut `false`) |
| `ARC_DISCOVERY_TOPIC` | Topic des identités consolidées (défaut `arc.sync.identity`) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka (défaut `localhost:9092`) |

Aucune valeur sensible n'est versionnée : copier [.env.example](.env.example) vers `.env` et adapter les valeurs.

## Démarrage local

Prérequis : Docker.

```bash
cp .env.example .env     # puis éditer les mots de passe
docker compose up --build
```

L'API est exposée sur `http://localhost:8083`. En canal `kafka`, les ordres attendent dans `arc.orders` si `arc-publisher` est arrêté ; en canal `api`, la création d'un compte échoue tant qu'il n'est pas joignable.

## Développement

Prérequis : JDK 25 et Maven 3.9 (ou `./mvnw`).

```bash
mvn test                           # tests unitaires
mvn clean test jacoco:report       # avec rapport de couverture (target/site/jacoco)
mvn verify                         # contrôle de couverture (80 % de lignes par classe)
```

## Déploiement

Le déploiement GitOps (Helm, FluxCD) est géré dans un dépôt privé. L'intégration et la mise en production de la plateforme sont proposées par Dédale Solutions en **service payant** : pour en discuter, contactez-nous à [etiennsalle@gmail.com](mailto:etiennsalle@gmail.com). Les workflows GitHub Actions (`build.yml`, `promote.yml`) construisent et publient l'image sur GHCR.

## Contribuer et sécurité

Voir [CONTRIBUTING.md](CONTRIBUTING.md) et [SECURITY.md](SECURITY.md).

## Licence

Copyright © 2026 Dédale Solutions

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
