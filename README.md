# LifeAsGame Backend

> 현실의 행동을 기록하고, 퀘스트와 보상으로 연결해 일상의 성장을 게임처럼 체감하게 만드는 백엔드입니다.

[한국어](#korean) · [English](#english)

<a id="korean"></a>

## 한국어

### 제품 소개

LifeAsGame은 운동, 미디어 감상, 수집과 같은 현실의 경험을 기록하고 그 기록을 퀘스트 진행, 보상, 캐릭터 성장으로 연결하는 서비스입니다. 이 저장소는 해당 제품의 Spring Boot 기반 모듈러 모놀리스 백엔드입니다.

현재 저장소는 **중간 개발 시점의 스냅샷**입니다. 핵심 성장 루프는 실제 코드와 데이터베이스 계약으로 연결되어 있지만, 모든 bounded context가 최종 제품 구조에 도달한 것은 아닙니다. 아래에서는 구현된 기능과 발전 중인 영역을 구분합니다.

### 핵심 제품 루프

```text
Collection / Exercise / Media 또는 Quick Record
        ↓
물리 Source + canonical LifeLogRecord
        ↓
LifeLogRecorded durable Fact + transactional outbox
        ↓
Quest signal receipt → 진행 → 완료
        ↓
RewardSettlement (EXP / ITEM line)
        ├── EXP  → Character 성장 및 성장 변경 ledger
        └── ITEM → Inventory mailbox 및 delivery receipt
```

이 루프의 correctness는 프로세스 메모리나 Redis만으로 보장하지 않습니다. 데이터베이스 unique constraint, pessimistic lock, receipt, settlement/ledger, 그리고 transactional outbox가 중복 처리와 재시도 경계를 담당합니다.

### 현재 구현 범위

| Bounded context | 상태 | 현재 구현 |
|---|---|---|
| User / Auth | 구현됨 | 회원 등록, 인증, JWT 발급·재발급, 사용자·설정 command/query, provider-owned `UserAuthApi` |
| Character | 구현됨 | Player onboarding, 캐릭터 조회·변경, EXP/레벨, 능력치, 장비, 칭호, 취미, 자격, 업적 foundation |
| Role / Person | persistence foundation 구현됨 | 플레이어 소유 Role과 Person CRUD, RoleRelation 생성·수정·보관·재활성화 |
| LifeLog | 구현됨 | Collection/Exercise/Media source, command/query 분리, canonical record, Quick Record |
| Quest | 핵심 루프 구현됨 | 정의·수락·진행·완료, LifeLog trigger, signal receipt와 replay 방어, 반복/기간 계약 |
| Reward | 핵심 루프 구현됨 | reward profile/definition, settlement와 line, EXP/ITEM 처리, 실패 기록과 재시도 준비 |
| Inventory | foundation 구현됨 | item catalog, inventory/mailbox, stack/carry policy, ITEM reward delivery receipt |
| Social | 발전 중 | party, guild, follow, chat 및 realtime 코드가 존재하지만 최종 아키텍처로 간주하지 않음 |
| Economy | 발전 중 | wallet, shop, marketplace, trade, payment 실험 코드가 존재하지만 최종 아키텍처로 간주하지 않음 |
| Skill / Logs | 초기 foundation | 일부 domain model만 존재하며 핵심 제품 루프의 완료된 context로 소개하지 않음 |

### LifeLog 모델

LifeLog는 하나의 범용 aggregate로 모든 기록을 합치지 않습니다.

- **Physical Source** — `CollectionLog`, `ExerciseLog`, `MediaLog`가 각 기록의 상세 데이터와 개인 텍스트를 소유합니다.
- **Canonical Header** — `LifeLogRecord`가 source identity, subtype, entry mode, reflection metadata, period key, occurred time을 표준화합니다.
- **Durable Fact** — `LifeLogRecorded`는 Quest가 소비할 최소한의 버전된 사실입니다. 새 Fact에는 물리 source의 private text나 source type을 노출하지 않습니다.
- **Quick Record** — `QuickRecordService`가 DB receipt를 예약·잠근 뒤 source, header, Fact, receipt completion을 하나의 transaction 흐름으로 조정합니다. 같은 idempotency key와 같은 payload는 저장된 결과를 replay하고, 충돌 payload는 거부합니다.

Source 저장, canonical header 등록, `LifeLogRecorded` publication은 의도된 동일 transaction 안에 있습니다. `LifeLogRecord`의 `primaryRoleId`는 현재 `null`만 허용하며 Role 연계는 아직 활성화하지 않았습니다.

### Role, Person, RoleRelation

- **Role**은 플레이어가 소유하는 이름 있는 역할입니다. role type, 이름, 설명, active/archive lifecycle을 가집니다.
- **Person**은 플레이어가 관리하는 개인 인물 기록입니다. display name과 선택적 notes, birthday, contact를 가지며 다른 context의 User entity를 직접 참조하지 않습니다.
- **RoleRelation**은 같은 플레이어가 소유한 Role과 Person을 ID로 연결하고 relation type과 role-specific notes를 보관합니다. active/archive/reactivation lifecycle을 가집니다.

Role/Person persistence와 API는 구현되어 있습니다. **RoleEvent, QuestRoute, LifeLog-to-Role linkage는 아직 구현 완료 상태가 아니며 planned/evolving 영역입니다.**

### 아키텍처

이 저장소는 **pragmatic DDD + reduced hexagonal boundaries + layered modular monolith**를 사용합니다.

```text
API → Application → Domain
          ↓           ↑
     provider APIs   Infrastructure adapters
```

- `api`는 HTTP/WebSocket 계약, validation, web DTO mapping을 담당합니다.
- `application`은 use case, orchestration, identity resolution, transaction boundary를 담당합니다.
- `domain`은 aggregate, entity, value object, domain policy, repository contract를 소유합니다. 실용적인 선택으로 JPA mapping은 domain model에 함께 존재합니다.
- `infra`는 Spring Data JPA, QueryDSL, Redis, JWT, realtime 및 외부 시스템 adapter를 제공합니다.
- `platform`은 outbox, web/security, persistence와 같은 횡단 기능을 제공합니다.

Reduced hexagonal은 모든 내부 호출에 Port/Adapter를 기계적으로 추가한다는 뜻이 아닙니다. 실제 infrastructure boundary 또는 bounded-context boundary가 있을 때만 작은 contract를 둡니다.

#### Responsibility-driven naming

클래스 이름은 패턴이 아니라 실제 책임에서 결정합니다.

- 단순 aggregate 조회·영속화에는 `Reader` / `Writer`가 적합할 수 있습니다.
- read use case에는 `QueryService`를 사용할 수 있습니다.
- cross-context reference에는 `LookupApi` / `LookupService`가 적합합니다.
- 실제 책임에 따라 `Provider`, `Resolver`, `Registrar`, `Processor`, `Factory`, `Policy`, `Issuer`, `Hasher`, `Supporter` 등의 이름을 우선합니다.

`Reader`, `Writer`, `Service`, `Provider` 같은 접미사를 맞추기 위해 추상화를 만들지 않습니다. 이름 자체가 transaction propagation을 결정하지도 않습니다.

#### Identity와 Facade

- self use case의 `CurrentUserAccessor` / `CurrentPlayerAccessor`는 실제 Application Service 또는 QueryService 내부에서 resolve합니다.
- Admin use case는 explicit `userId` / `playerId` 경로를 유지합니다.
- identity를 resolve한 뒤 하나의 service로 그대로 전달하는 simple Facade는 권장하지 않습니다.
- Facade는 여러 provider와 service를 조합해 실제 workflow를 완성할 때만 사용합니다. 현재 `AuthFacade`는 User 인증, Player lookup, token 발급을 조정하며, `PlayerFacade`는 player onboarding, token 발급, equipment 초기화를 조정합니다.

#### Bounded-context ownership

Cross-context consumer는 다른 context의 Entity나 Repository에 직접 접근하지 않습니다. Provider context가 소유한 최소 `InternalApi`를 제공하고 ID, immutable reference, result snapshot을 교환합니다.

현재 예시는 다음과 같습니다.

- User-owned `UserAuthApi`
- Character-owned `PlayerLookupApi`, `PlayerGrowthApi`
- Auth-owned `AuthTokenApi`
- Person-owned `PersonLookupApi`
- Reward-owned `RewardProfileLookupApi`
- Inventory-owned `ItemLookupApi`, `InventoryRewardDeliveryApi`

#### Transaction과 correctness

- Write use case의 transaction boundary는 Application Service에서 시작합니다.
- 단순 command-side Reader는 호출 계약에 따라 `SUPPORTS/readOnly`를 사용할 수 있습니다.
- 기존 write transaction 안에서만 안전한 Writer/Registrar는 `MANDATORY`를 사용할 수 있습니다.
- QueryService는 일반적으로 `readOnly = true`입니다.
- DB constraint와 lock이 authoritative correctness를 보장합니다. Redis는 cache, realtime, 제한된 coordination 용도이며 correctness의 유일한 source가 아닙니다.
- Transactional outbox는 commit된 Fact를 relay하며 lease, retry, attempt 상태를 DB에 기록합니다.

#### Flyway

Flyway migration은 append-only입니다.

- 적용된 migration을 수정하거나 재정렬하지 않습니다.
- schema 변경에는 다음 버전의 새 migration을 추가합니다.
- Hibernate schema auto-update는 사용하지 않습니다. local profile은 Flyway 적용 후 `ddl-auto: validate`로 mapping을 검증합니다.

### 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4, Spring Security, WebSocket/STOMP |
| Persistence | MySQL 8, Spring Data JPA, QueryDSL 5.0, Flyway |
| Supporting infrastructure | Redis 7, transactional outbox |
| API / Auth | SpringDoc OpenAPI 2.8.11, JWT (`jjwt` 0.12.6) |
| Test | JUnit 5, Testcontainers 1.21.4, MySQL, H2 |
| CI/CD | GitHub Actions |

### 로컬 실행

필수 환경:

- Java 21
- Docker와 Docker Compose, 또는 직접 실행한 MySQL 8 / Redis 7

```bash
docker compose -f docker-compose/docker-compose.local.yml up -d

export DB_URL=jdbc:mysql://localhost:3306/lifeasgame
export DB_USERNAME=root
export DB_PASSWORD=root

./gradlew bootRun
```

Local profile은 기본값이며 Flyway migration을 실행합니다.

### API 문서와 테스트

애플리케이션 실행 후:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

테스트 실행:

```bash
./gradlew test
```

### 대표 구현 위치

- User/Auth: [`user/application`](src/main/java/online/lifeasgame/user/application), [`auth/application/AuthFacade.java`](src/main/java/online/lifeasgame/auth/application/AuthFacade.java)
- Character: [`character/application`](src/main/java/online/lifeasgame/character/application)
- Role/Person: [`role/domain`](src/main/java/online/lifeasgame/role/domain), [`person/domain/Person.java`](src/main/java/online/lifeasgame/person/domain/Person.java)
- LifeLog: [`lifelog/application`](src/main/java/online/lifeasgame/lifelog/application), [`LifeLogRecord.java`](src/main/java/online/lifeasgame/lifelog/domain/record/LifeLogRecord.java), [`LifeLogRecorded.java`](src/main/java/online/lifeasgame/lifelog/domain/event/LifeLogRecorded.java)
- Quest: [`quest/application`](src/main/java/online/lifeasgame/quest/application)
- Reward: [`reward/application`](src/main/java/online/lifeasgame/reward/application)
- Inventory: [`inventory/application`](src/main/java/online/lifeasgame/inventory/application)
- Outbox: [`platform/outbox`](src/main/java/online/lifeasgame/platform/outbox)

### 발전 중인 영역

다음 항목은 현재 최종 아키텍처나 완료된 제품 기능으로 주장하지 않습니다.

- LifeLog와 Role의 연결, RoleEvent, QuestRoute
- Social/Economy context의 추가 architecture alignment와 운영 검증
- Skill/Logs context의 application/API 완성
- 범용 timeline/journal 경험과 후속 제품 기능

---

<a id="english"></a>

## English

### Product

LifeAsGame turns real-life experiences—such as exercise, media consumption, and collecting—into quest progress, rewards, and visible character growth. This repository contains the Spring Boot modular-monolith backend for that product.

The repository is a **midpoint development snapshot**. The core growth loop is connected through application code and database contracts, while several bounded contexts are still evolving. The sections below distinguish implemented capabilities from planned or non-final areas.

### Core product loop

```text
Collection / Exercise / Media or Quick Record
        ↓
Physical Source + canonical LifeLogRecord
        ↓
LifeLogRecorded durable Fact + transactional outbox
        ↓
Quest signal receipt → progress → completion
        ↓
RewardSettlement (EXP / ITEM line)
        ├── EXP  → Character growth and growth-change ledger
        └── ITEM → Inventory mailbox and delivery receipt
```

Correctness is not based only on process memory or Redis. Database unique constraints, pessimistic locks, receipts, settlements/ledgers, and the transactional outbox define duplicate-processing and retry boundaries.

### Implemented capabilities

| Bounded context | Status | Current capability |
|---|---|---|
| User / Auth | Implemented | Registration, authentication, JWT issue/refresh, user/settings commands and queries, provider-owned `UserAuthApi` |
| Character | Implemented | Player onboarding, character reads and mutations, EXP/levels, stats, equipment, titles, hobbies, certifications, and achievement foundations |
| Role / Person | Persistence foundation implemented | Player-owned Role and Person CRUD, plus RoleRelation create/update/archive/reactivation |
| LifeLog | Implemented | Collection/Exercise/Media sources, command/query separation, canonical records, and Quick Record |
| Quest | Core loop implemented | Definitions, acceptances, progress, completion, LifeLog triggers, signal receipts and replay protection, repeat/period contracts |
| Reward | Core loop implemented | Reward profiles/definitions, settlements and lines, EXP/ITEM processing, failure recording, and retry preparation |
| Inventory | Foundation implemented | Item catalog, inventory/mailbox, stack/carry policies, and ITEM reward-delivery receipts |
| Social | Evolving | Party, guild, follow, chat, and realtime code exists, but is not presented as the final architecture |
| Economy | Evolving | Wallet, shop, marketplace, trade, and payment experiment code exists, but is not presented as the final architecture |
| Skill / Logs | Early foundation | Partial domain models exist; these are not presented as completed core-loop contexts |

### LifeLog model

LifeLog does not collapse every record into one generic aggregate.

- **Physical Source** — `CollectionLog`, `ExerciseLog`, and `MediaLog` own detailed source data and private text.
- **Canonical Header** — `LifeLogRecord` standardizes source identity, subtype, entry mode, reflection metadata, period key, and occurrence time.
- **Durable Fact** — `LifeLogRecorded` is the minimal versioned fact consumed by Quest. New facts do not expose private source text or the physical source type.
- **Quick Record** — `QuickRecordService` reserves and locks a DB receipt, then coordinates source creation, the header, the Fact, and receipt completion in one transaction flow. The same idempotency key and payload replay the stored result; a conflicting payload is rejected.

Source persistence, canonical-header registration, and `LifeLogRecorded` publication remain in the intended transaction. `LifeLogRecord.primaryRoleId` currently accepts only `null`; Role linkage is not active yet.

### Role, Person, and RoleRelation

- **Role** is a named role owned by a player, with a role type, name, description, and active/archive lifecycle.
- **Person** is a private person record managed by a player, with a display name and optional notes, birthday, and contact. It does not directly reference another context's User entity.
- **RoleRelation** links a Role and Person owned by the same player by ID. It stores a relation type and role-specific notes, with active/archive/reactivation lifecycle.

Role/Person persistence and APIs are implemented. **RoleEvent, QuestRoute, and LifeLog-to-Role linkage are planned/evolving, not completed capabilities.**

### Architecture

The repository uses **pragmatic DDD + reduced hexagonal boundaries + a layered modular monolith**.

```text
API → Application → Domain
          ↓           ↑
     provider APIs   Infrastructure adapters
```

- `api` owns HTTP/WebSocket contracts, validation, and web DTO mapping.
- `application` owns use cases, orchestration, identity resolution, and transaction boundaries.
- `domain` owns aggregates, entities, value objects, domain policies, and repository contracts. As a pragmatic choice, JPA mappings are colocated with the domain model.
- `infra` provides Spring Data JPA, QueryDSL, Redis, JWT, realtime, and external-system adapters.
- `platform` provides cross-cutting outbox, web/security, and persistence facilities.

Reduced hexagonal does not mean adding mechanical Port/Adapter pairs to every internal call. A small contract is introduced where there is a real infrastructure or bounded-context boundary.

#### Responsibility-driven naming

Class names follow actual responsibility rather than a mandatory pattern.

- `Reader` / `Writer` may fit simple aggregate reads and persistence.
- `QueryService` may own read use cases.
- `LookupApi` / `LookupService` may provide cross-context references.
- `Provider`, `Resolver`, `Registrar`, `Processor`, `Factory`, `Policy`, `Issuer`, `Hasher`, and `Supporter` are preferred when those names describe the real responsibility.

No abstraction is created merely to align suffixes such as `Reader`, `Writer`, `Service`, or `Provider`. A class name does not determine transaction propagation.

#### Identity and Facades

- Self-service use cases resolve `CurrentUserAccessor` / `CurrentPlayerAccessor` inside the actual Application Service or QueryService.
- Admin use cases retain explicit `userId` / `playerId` paths.
- A simple Facade that resolves identity and forwards to one service is not recommended.
- A Facade is reserved for genuine orchestration across multiple providers or services. `AuthFacade` coordinates User authentication, Player lookup, and token issue; `PlayerFacade` coordinates player onboarding, token issue, and equipment initialization.

#### Bounded-context ownership

A cross-context consumer does not access another context's Entity or Repository directly. The provider context owns a minimal `InternalApi`, and contexts exchange IDs, immutable references, or result snapshots.

Current examples include:

- User-owned `UserAuthApi`
- Character-owned `PlayerLookupApi` and `PlayerGrowthApi`
- Auth-owned `AuthTokenApi`
- Person-owned `PersonLookupApi`
- Reward-owned `RewardProfileLookupApi`
- Inventory-owned `ItemLookupApi` and `InventoryRewardDeliveryApi`

#### Transactions and correctness

- Write-use-case transaction boundaries begin in Application Services.
- A simple command-side Reader may use `SUPPORTS/readOnly` when that matches its call contract.
- A Writer or Registrar that is safe only inside an existing write transaction may use `MANDATORY`.
- QueryServices are generally `readOnly = true`.
- Database constraints and locks provide authoritative correctness. Redis supports caching, realtime, and limited coordination; it is not the only source of correctness.
- The transactional outbox relays committed Facts and stores leases, retries, and attempt state in the database.

#### Flyway

Flyway migrations are append-only.

- Do not edit or reorder an applied migration.
- Add the next migration version for every schema change.
- Hibernate schema auto-update is disabled. The local profile applies Flyway and then validates mappings with `ddl-auto: validate`.

### Technology

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4, Spring Security, WebSocket/STOMP |
| Persistence | MySQL 8, Spring Data JPA, QueryDSL 5.0, Flyway |
| Supporting infrastructure | Redis 7, transactional outbox |
| API / Auth | SpringDoc OpenAPI 2.8.11, JWT (`jjwt` 0.12.6) |
| Test | JUnit 5, Testcontainers 1.21.4, MySQL, H2 |
| CI/CD | GitHub Actions |

### Local development

Requirements:

- Java 21
- Docker and Docker Compose, or independently running MySQL 8 and Redis 7

```bash
docker compose -f docker-compose/docker-compose.local.yml up -d

export DB_URL=jdbc:mysql://localhost:3306/lifeasgame
export DB_USERNAME=root
export DB_PASSWORD=root

./gradlew bootRun
```

The local profile is the default and runs Flyway migrations.

### API documentation and tests

After starting the application:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Run tests with:

```bash
./gradlew test
```

### Representative implementation references

- User/Auth: [`user/application`](src/main/java/online/lifeasgame/user/application), [`auth/application/AuthFacade.java`](src/main/java/online/lifeasgame/auth/application/AuthFacade.java)
- Character: [`character/application`](src/main/java/online/lifeasgame/character/application)
- Role/Person: [`role/domain`](src/main/java/online/lifeasgame/role/domain), [`person/domain/Person.java`](src/main/java/online/lifeasgame/person/domain/Person.java)
- LifeLog: [`lifelog/application`](src/main/java/online/lifeasgame/lifelog/application), [`LifeLogRecord.java`](src/main/java/online/lifeasgame/lifelog/domain/record/LifeLogRecord.java), [`LifeLogRecorded.java`](src/main/java/online/lifeasgame/lifelog/domain/event/LifeLogRecorded.java)
- Quest: [`quest/application`](src/main/java/online/lifeasgame/quest/application)
- Reward: [`reward/application`](src/main/java/online/lifeasgame/reward/application)
- Inventory: [`inventory/application`](src/main/java/online/lifeasgame/inventory/application)
- Outbox: [`platform/outbox`](src/main/java/online/lifeasgame/platform/outbox)

### Evolving areas

The following are not presented as final architecture or completed product capabilities:

- LifeLog-to-Role linkage, RoleEvent, and QuestRoute
- Further architecture alignment and operational validation for Social/Economy
- Complete application/API layers for Skill/Logs
- A generic timeline/journal experience and later product features
