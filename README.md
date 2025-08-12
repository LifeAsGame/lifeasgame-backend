## TL;DR

- **가치:** 현실 활동을 RPG 메커닉(레벨·스탯·퀘스트)으로 시각화하여 지속 동기 제공
- **아키텍처:** 모듈러 모놀리식 · DDD-lite · CQRS-lite · Ports(교차 컨텍스트/외부)
- **스택:** Java 21 · Spring Boot 3.5.4 · MySQL 8.0.2 · Redis 7.x · JPA(H6) · Flyway
---

## 1) 프로젝트 개요

현실의 활동 기록이 곧 캐릭터의 스탯/경험치가 되는 자기계발 플랫폼.

20~30대 전환기 사용자의 “잘 하고 있는가?”를 **수치/성장 연출**로 답합니다.

**주요 기능(MVP)**

- 스탯: 근성·집중력·지속력·자기통제력·체력·지식·감정지능·소통력
- 활동 기록 → 경험치/스탯 반영 (초기 rule-based, 이후 ML 확장)
- 디지털 디톡스(잠금/집중 모드) → 통제력/포인트
- 퀘스트/미션(일·주 반복) → EXP/스탯/아이템 보상
- 성장 로그(Exp/Stat/Quest) · 시스템 알림

---

## 2) 아키텍처

```
presentation → application → domain → infra     (단방향)
```

- **Service(오케스트레이터):** 흐름/트랜잭션 경계
- **Reader/Writer:**
    - Reader = 조회 전용(프로젝션, `readOnly=true`)
    - Writer = 상태 전이/불변식/멱등/락/도메인 이벤트(`@Transactional`)
- **Ports:** 교차 컨텍스트/외부 API는 interface 의존, 구현은 infra 어댑터
- **원칙:** 애그리거트 외부는 **ID 참조**, 같은 컨텍스트는 JPA 직접

**폴더 스켈레톤**

```
lifeasgame/<context>/
 ├─ presentation/        # Controller, Request, Response
 ├─ application/
 │   ├─ service/         # ex. QuestService, UserService
 │   ├─ reader/          # ex. QuestProgressFinder
 │   ├─ writer/          # ex. CompleteQuest, RegisterUser
 │   └─ port/            # ex. PlayerRewardPort, MailSender
 ├─ domain/              # @Entity, VO, DomainService, Repository(인터페이스)
 └─ infra/
     ├─ jpa/             # Spring Data로 도메인 Repo 구현
     ├─ query/           # Jdbc/Native/QueryDSL 프로젝션
     ├─ redis/           # 캐시/락/멱등
     └─ external|internal/ # 외부 API/내부 Facade 어댑터

```

---

## 3) 기술 스택 (버전 & 이유)

| 영역 | 선택 | 이유 |
| --- | --- | --- |
| Language | **Java 21 (LTS)** | 최신 기능 + LTS 운영 안정성 |
| Framework | **Spring Boot 3.5.4** | Jakarta EE9/H6/AOT, Actuator/Config 최신 |
| DB | **MySQL 8.0.2** | JSON·윈도우·CTE → 정규화+유연성 |
| Cache/Lock | **Redis 7.x** | 캐시-어사이드·분산락·멱등키·레이트리밋 |
| ORM | **Spring Data JPA (Hibernate 6)** | 쓰기 모델 일관성/도메인 중심 |
| Migration | **Flyway** | 스키마 이력/재현성 |
| Auth | **Spring Security (JWT)** | 표준 필터체인/소유권 가드 |
| Test | **JUnit5, Testcontainers** | 실 DB/Redis 통합 테스트 |
| Obs | **Actuator** | `/health` `/metrics` `/info` |