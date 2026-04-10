# Life As Game — Backend

> 인생의 경험을 기록하고, 게임 형태의 성장 지표로 시각화해주는 서비스  
> 레이어드부터 DDD까지 직접 겪으며 쌓은 설계 이해도를 실제 구조로 구현해본 개인 프로젝트입니다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4, Spring Security, Spring WebSocket |
| Database | MySQL 8.x, Redis 7.x |
| ORM | Spring Data JPA (Hibernate 6), QueryDSL 5 |
| Migration | Flyway |
| Infra | GitHub Actions |
| Docs | SpringDoc OpenAPI (Swagger) |


---

## 아키텍처

### 모듈러 모놀리스

컨텍스트 간 의존을 ID 참조로만 유지해 향후 서비스 분리가 가능한 구조로 설계했습니다.

```
online.lifeasgame
├── character     # 플레이어 / 스탯 / 레벨 / 타이틀 / 장착 슬롯
├── skill         # 스킬 / 스킬 트리 / 포인트
├── inventory     # 아이템 / 장비 / 인벤토리 / 메일박스
├── quest         # 퀘스트 / 수락 / 진행 / 보상 / 반복
├── social        # 파티 / 길드 / 팔로우 / 채팅
├── economy       # 지갑 / 상점 / 거래
├── lifelog       # 운동 / 미디어 / 수집 기록
├── logs          # 경험치 / 스탯 변화 / 퀘스트 클리어 로그
├── user          # 계정 / 설정 / 권한
├── platform      # 횡단 관심사 (이벤트 / 멱등성 / 트레이싱 / 보안)
└── core          # 공통 유틸 / Guard / 에러 정의
```

### 레이어 의존 방향

```
api → application → domain ← infra
```

### 레이어별 역할

**[API]**
- 요청/응답 DTO 변환과 유효성 검증만 담당
- `Controller → Spec 인터페이스` 분리로 명세와 구현 분리

**[Application]**
- `Service` — 유스케이스 오케스트레이션 및 트랜잭션 경계
- `Reader` — `@Transactional(readonly = true, propagation = SUPPORTS)` 적용
  - 상위 트랜잭션 존재 시 합류, 없으면 비트랜잭션 실행
  - SimpleJpaRepository 기본 트랜잭션(REQUIRED)을 우회해 불필요한 오버헤드 제거
- `Writer` — `@Transactional(propagation = MANDATORY)` + `package-private`
  - 상위 트랜잭션 없이 호출 불가 강제 → 트랜잭션 경계 외부 호출 차단

**[Domain]**
- 엔티티, VO, 도메인 서비스 등 순수 도메인 모델만 포함
- JPA, Redis 등 인프라 기술에 의존하지 않고 인터페이스만 정의

**[Infra]**
- Domain에서 정의한 인터페이스를 구현
- Adapter 패턴으로 내부 구체 기술을 주입·교체 가능한 구조

---

## 설계 포인트

### Guard 유틸리티
불변식을 생성 시점에 강제해 null과 범위 위반을 런타임 이전에 차단합니다.

```java
Guard.notBlank(key, "idempotencyKey");
Guard.minValue(value, 0, "exp");
```

### 도메인 이벤트
애그리거트 내부 리스트에 이벤트를 수집하고 트랜잭션 완료 후 일괄 발행합니다.  
도메인이 발행 방식(Spring Event, Kafka 등)에 의존하지 않는 구조입니다.

### 멱등성 처리
Redis 기반 `IdempotencyKeyStore`로 중복 요청을 방지합니다.

```java
boolean acquired = idempotencyKeyStore.acquire(key, ttl);
```

### 요청 추적 (X-Trace-Id)
`TraceIdFilter`로 모든 요청에 Trace ID를 부여하고 MDC에 전파합니다.  
비동기 처리 시 `MdcTaskDecorator`로 스레드 간 컨텍스트 유실을 방지합니다.

### PII / 보안 마스킹
`ErrorCode`에 `PII / SECRET / PCI` 민감도 등급을 부여하고,  
에러 응답과 로그에서 JWT, 카드번호, 이메일을 자동 마스킹합니다.

```java
// PiiScrubber가 Sensitivity 등급에 따라 자동 처리
Sensitivity.SECRET → "**secret**"
Sensitivity.PCI    → "****-****-****-****"
Sensitivity.PII    → "u***@***"
```

### 환경별 설정 분리
에러 상세 노출 여부·로그 마스킹 정책을 `ConfigurationProperties`로 분리해  
`dev / prod` 환경별로 다르게 제어합니다.

---

## 로컬 실행

### 사전 요구사항
- Java 21
- MySQL 8.x
- Redis 7.x

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### 주요 환경변수

```
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/lifeasgame
SPRING_DATASOURCE_USERNAME=lifeasgame
SPRING_DATASOURCE_PASSWORD=lifeasgame
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
APP_JWT_SECRET=your-secret
```

---

## API 문서

서버 실행 후 아래 주소에서 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 테스트

```bash
./gradlew test
```

JUnit 5 + Testcontainers 기반으로 작성했습니다.

---

## 개발 규칙

- 교차 컨텍스트 참조는 ID로만
- 조회는 `Reader`, 상태 변경은 `Writer`로 역할 분리 유지
- 외부/내부 호출은 `Port(인터페이스)`로 끊고 `infra`에서 구현
- 멱등성·락·이벤트는 `platform` 레이어에서 횡단 처리
