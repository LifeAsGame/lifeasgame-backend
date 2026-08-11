# LifeAsGame Backend

> 보이지 않던 삶의 성장과 관계의 흔적을 기록하고, 목표와 성취를 게임처럼 체감할 수 있게 만드는 Life RPG 플랫폼의 백엔드입니다.

[한국어](#korean) · [English](#english)

<a id="korean"></a>

## 한국어

### 제품 소개

LifeAsGame은 **내가 무엇을 해왔는지, 지금 어디까지 왔는지, 앞으로 무엇을 해야 하는지를 눈에 보이게 만드는 Life RPG 플랫폼**입니다.

취준생, 수험생, 직장인처럼 오랜 시간 노력하고 있어도 결과가 바로 보이지 않는 사람은 쉽게 이런 감각을 잃습니다.

- 나는 요즘 제대로 살고 있는가
- 지금까지 무엇을 해왔는가
- 실제로 성장하고 있는가
- 내 목표는 무엇인가
- 지금 무엇을 해야 하는가
- 예전에 중요하게 생각했던 것은 무엇이었는가

공부, 운동, 프로젝트, 취업 준비, 독서, 취미, 인간관계처럼 현실의 중요한 활동은 대부분 하나의 숫자로 즉시 보상되지 않습니다.  
시간이 지나면 자신이 쌓아온 과정조차 잊기 쉽고, 눈앞의 결과만으로 자신을 평가하면서 불안, 무기력, 방향 상실을 느끼기도 합니다.

LifeAsGame은 이러한 **보이지 않는 성장과 경험의 흔적을 기록 가능한 상태로 만들고**, 그것을 Quest, EXP, Reward, Character Progression과 연결해 사용자가 자신의 삶을 하나의 긴 성장 과정으로 바라볼 수 있게 하는 것을 목표로 합니다.

```text
내가 해온 것
    ↓
LifeLog / Quest / Achievement
    ↓
눈에 보이는 성장과 누적
    ↓
현재 위치 이해
    ↓
다음 목표와 행동 선택
```

단순히 해야 할 일을 체크하는 서비스가 아니라,

- 내가 어떤 노력을 해왔는지
- 어떤 경험을 쌓았는지
- 어떤 목표를 향하고 있는지
- 무엇을 좋아하고 중요하게 생각하는지
- 어느 영역에서 성장하고 있는지

를 한곳에서 계속 축적하고 되돌아볼 수 있게 하는 것이 핵심입니다.

이를 통해 사용자가 자신의 삶을 결과 한 번으로 평가하기보다 **과정과 누적된 성장을 확인하고, 다음 행동의 방향을 찾을 수 있는 경험**을 만드는 것을 지향합니다.

### 관계와 기억도 삶의 일부로 기록

LifeAsGame에서 성장은 개인의 능력치만을 의미하지 않습니다.

사람과의 관계, 함께 보낸 시간, 기억하고 싶은 대화와 약속 역시 삶을 구성하는 중요한 자산으로 봅니다.

장기적으로는 다음과 같은 관계의 맥락도 기록하고 이어갈 수 있도록 설계합니다.

- 이 사람은 나와 어떤 관계인가
- 어떤 Role에서 만난 사람인가
- 예전에 어떤 중요한 이야기를 나눴는가
- 무엇을 좋아하고 싫어하는가
- 함께 무엇을 했는가
- 어떤 추억이 있었는가
- 다음에 무엇을 하기로 했는가
- 어떤 약속이나 계획이 남아 있는가

이 기능의 목적은 사람을 CRM처럼 관리하는 것이 아닙니다.

시간이 지나며 사라지기 쉬운 **관계의 맥락과 기억을 보존하고**, 내가 중요하게 생각하는 사람들과의 연결을 더 오래 이어갈 수 있도록 돕는 개인적인 Life Memory System을 지향합니다.

이를 위해 LifeAsGame은 다음 개념을 구분합니다.

- **Role** — 내가 살아가며 가지는 역할과 정체성의 맥락
- **Person** — 실제 한 사람에 대한 개인적인 Master Record
- **RoleRelation** — 특정 Role 안에서 그 Person과 맺고 있는 관계의 맥락

같은 Person이라도 친구, 동아리 선배, 직장 동료처럼 서로 다른 Role 안에서 다른 관계의 의미를 가질 수 있습니다.

향후 `RoleEvent`, LifeLog, Person/RoleRelation이 연결되면 **누구와 어떤 일을 했고, 어떤 경험을 남겼는지**를 삶의 흐름 안에서 기록하는 방향으로 확장합니다.

### LifeAsGame이 만들고 싶은 경험

LifeAsGame은 특정 생산성 앱 하나를 대체하는 서비스가 아닙니다.

단순 Todo, Diary, Workout Tracker, Finance Tracker, SNS 각각을 별도로 만드는 것이 아니라, 현실의 여러 영역을 하나의 성장 경험으로 연결하려고 합니다.

```text
Goal / Direction
        ↓
Quest
        ↓
Action / Experience
        ↓
LifeLog
        ↓
Reward / EXP / Item
        ↓
Character Growth
        ↓
Reflection / Next Goal
```

그리고 삶의 다른 축도 함께 연결합니다.

```text
Role
 ├── Quest
 ├── LifeLog
 ├── Person / RoleRelation
 ├── RoleEvent
 └── Growth / Memory
```

최종적으로는 사용자가 앱을 열었을 때 다음 질문에 답할 수 있는 경험을 목표로 합니다.

```text
나는 어떤 사람으로 살아가고 있는가?
나는 지금 무엇을 하고 있는가?
나는 지금까지 무엇을 해왔는가?
나는 어디에서 성장하고 있는가?
내가 중요하게 생각하는 사람들은 누구인가?
그들과 어떤 시간을 보냈는가?
다음에는 무엇을 해야 하는가?
```

현재 저장소는 이 장기 제품 비전을 구현하는 **중간 개발 시점의 백엔드 스냅샷**입니다.  
핵심 성장 루프는 실제 코드와 데이터베이스 계약으로 연결되어 있지만, 모든 bounded context가 최종 제품 구조에 도달한 것은 아닙니다.

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

### 프로덕션 웹 연동

프론트엔드 배포 환경:

```bash
NEXT_PUBLIC_USE_MOCK=false
NEXT_PUBLIC_API_URL=<backend URL>
```

백엔드 `prod` profile은 허용할 프론트엔드 origin을 필수 환경변수로 받습니다.

```bash
LIFEASGAME_WEB_ALLOWED_ORIGINS=<frontend production origin>
```

여러 origin은 공백 없는 쉼표 구분 형식으로 설정합니다.
예: `https://frontend.example,https://admin.example`. 각 값은 scheme과 host를 포함한 정확한
origin이어야 하며 wildcard는 허용하지 않습니다. 로컬 기본 origin은
`http://localhost:3000`입니다.

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

LifeAsGame is a **Life RPG platform designed to make invisible personal growth visible**.

Students preparing for exams, job seekers, and working professionals often spend months or years putting effort into things whose results are not immediately visible.

Over time, it becomes easy to lose track of questions such as:

- What have I actually done?
- Am I making progress?
- What was I trying to achieve?
- What should I do next?
- What used to matter to me?
- How much have I already built up?

Studying, exercise, projects, job preparation, reading, hobbies, and relationships rarely provide immediate feedback in a form that feels measurable. When the accumulated process becomes invisible, people can start evaluating themselves only by the latest result and lose their sense of progress or direction.

LifeAsGame aims to turn those **invisible traces of effort, experience, and growth into something that can be recorded, accumulated, revisited, and visualized**.

Real-life actions and experiences are connected to Quest, EXP, Reward, and Character progression so that a player can view life as one continuous progression rather than a sequence of disconnected outcomes.

```text
What I have done
        ↓
LifeLog / Quest / Achievement
        ↓
Visible accumulated progress
        ↓
Understand where I am
        ↓
Choose the next goal and action
```

The goal is not simply to check off tasks.

LifeAsGame is intended to help the player understand:

- what effort they have accumulated
- what experiences they have had
- what goals they are moving toward
- what they value and enjoy
- where they are growing
- what they should focus on next

The product is designed around making accumulated effort visible so that a person can evaluate their life through a longer timeline of progress rather than a single outcome.

### Relationships and memories are part of progression

Growth is not limited to personal stats.

People, relationships, shared memories, meaningful conversations, and promises are also part of a person's life.

LifeAsGame is designed to eventually preserve relationship context such as:

- who this person is to me
- which Role connects us
- what important conversations we have had
- what they like or dislike
- what we have done together
- what memories we share
- what we said we would do next
- which promises or plans are still open

This is not intended to turn people into CRM entries.

The goal is to preserve the **context and memories that naturally disappear over time**, helping the player keep meaningful relationships connected to the rest of their life history.

The model separates:

- **Role** — a real-life identity or context the player lives through
- **Person** — the player's private master record for a real person
- **RoleRelation** — the relationship context between a Role and that Person

The same Person may have different meanings in different Roles—for example, a friend, a club senior, and a coworker.

As `RoleEvent`, LifeLog, Person, and RoleRelation evolve, the platform is intended to preserve not only **what happened**, but also **who was there, what it meant, and what should be remembered next**.

### The experience LifeAsGame is trying to create

LifeAsGame is not intended to be a replacement for one productivity category.

It is not simply a Todo app, Diary, Workout Tracker, Finance Tracker, or Social Network.

The product aims to connect different parts of life into one progression system.

```text
Goal / Direction
        ↓
Quest
        ↓
Action / Experience
        ↓
LifeLog
        ↓
Reward / EXP / Item
        ↓
Character Growth
        ↓
Reflection / Next Goal
```

Other parts of life are connected through the same model.

```text
Role
 ├── Quest
 ├── LifeLog
 ├── Person / RoleRelation
 ├── RoleEvent
 └── Growth / Memory
```

The long-term experience is meant to help a player answer:

```text
Who am I becoming?
What am I doing now?
What have I done so far?
Where am I growing?
Who are the people that matter to me?
What have we experienced together?
What should I do next?
```

This repository is a **midpoint development snapshot** of the backend for that long-term product vision.

The core growth loop is already connected through application code and database contracts, while several bounded contexts are still evolving. The sections below distinguish implemented capabilities from planned or non-final areas.

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

### Production web integration

Frontend deployment environment:

```bash
NEXT_PUBLIC_USE_MOCK=false
NEXT_PUBLIC_API_URL=<backend URL>
```

The backend `prod` profile requires the frontend origin through an environment variable.

```bash
LIFEASGAME_WEB_ALLOWED_ORIGINS=<frontend production origin>
```

For multiple origins, use a comma-separated list without spaces, for example
`https://frontend.example,https://admin.example`. Each value must be an exact origin including
scheme and host; wildcards are rejected. The local default origin is
`http://localhost:3000`.

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
