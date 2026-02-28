# Life As Game Backend

현실의 활동 기록이 곧 캐릭터의 스탯/경험치가 되는 **자기계발 RPG 플랫폼** 백엔드입니다.  
“잘 하고 있는가?”를 **수치/성장 연출**로 답하고, 퀘스트/기록/경제/소셜로 확장합니다.

---

## TL;DR
- **가치:** 현실 활동을 RPG 메커닉(레벨·스탯·퀘스트)으로 시각화하여 지속 동기 제공
- **아키텍처:** 모듈러 모놀리식 · DDD-lite · CQRS-lite · Ports(교차 컨텍스트/외부) 지향
- **스택:** Java 21 · Spring Boot 3.5.4 · MySQL 8.x · Redis 7.x · Spring Data JPA(Hibernate 6) · Flyway · QueryDSL · Kafka · Spring Security(JWT)

---

## 주요 기능 (MVP)
- **스탯(예):** 근성·집중력·지속력·자기통제력·체력·지식·감정지능·소통력
- 활동 기록 → 경험치/스탯 반영 (초기 rule-based, 이후 확장)
- 디지털 디톡스(잠금/집중 모드) → 통제력/포인트
- 퀘스트/미션(일·주 반복) → EXP/스탯/아이템 보상
- 성장 로그(Exp/Stat/Quest) · 시스템 알림

---

## 아키텍처

### 레이어 규칙
단방향 의존:

~~~text
api → application → domain → infra
~~~

- **application**
  - `service`: 유스케이스 오케스트레이션 / 트랜잭션 경계
  - `reader`: 조회 전용(Read model, readOnly 트랜잭션)
  - `writer`: 상태 전이/불변식/멱등/락/도메인 이벤트 처리
  - `port`: 외부/교차 컨텍스트 호출 인터페이스(Ports & Adapters)
- **domain**
  - 엔티티/VO/도메인 서비스/도메인 레포지토리(인터페이스)
- **infra**
  - JPA/QueryDSL/Redis/Kafka 등 구현 어댑터

> 프로젝트(zip 기준) 패키지 구조는 `online.lifeasgame.<context>.{api|application|domain|infra}` 형태를 따릅니다.

---

## 컨텍스트(도메인) 구성

zip 기준 최상위 컨텍스트(도메인/기능)들:

- `character` : 플레이어/스탯/레벨/타이틀/장착 슬롯
- `skill` : 스킬/스킬 트리/포인트
- `inventory` : 아이템/장비/인벤토리/메일박스
- `quest` : 퀘스트/수락/진행/보상/반복
- `social` : 파티/길드/팔로우(친구)/채팅
- `economy` : 지갑/상점/거래/리스팅
- `lifelog` : 운동/미디어/수집 기록
- `logs` : 경험치/스탯 변화/퀘스트 클리어 등 “게임 로그”
- `user` : 계정/설정/권한 관련

공통/플랫폼:
- `core` : 공통 유틸/에러/응답/보안/시간 등 베이스 레이어
- `platform` : 이벤트/멱등성/영속/실시간/보안/웹 등 횡단 관심사
- `system.bootstrap` : 에러 문서/에러 핸들링/부트스트랩 설정

---

## 실행 방법 (Local)

### 사전 요구사항
- Java 21
- Docker (로컬 인프라용)
- (선택) MySQL / Redis / Kafka

### 1) 의존 인프라 실행 (Docker Compose 예시)
프로젝트에 compose가 없다면 아래 예시로 `docker-compose.yml`을 만들어 사용하세요.

~~~yaml
version: "3.9"
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: lifeasgame
      MYSQL_USER: lifeasgame
      MYSQL_PASSWORD: lifeasgame
    ports: ["3306:3306"]

  redis:
    image: redis:7
    ports: ["6379:6379"]

  kafka:
    image: bitnami/kafka:3
    environment:
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=broker,controller
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - ALLOW_PLAINTEXT_LISTENER=yes
    ports: ["9092:9092"]
~~~

실행:

~~~bash
docker compose up -d
~~~

### 2) 애플리케이션 실행
~~~bash
./gradlew bootRun
~~~

> 스키마 마이그레이션(Flyway), JPA/Redis/Kafka 설정은 `application-*.yml` 또는 환경변수 기반 구성을 권장합니다.  
> (zip에는 `application.yml`이 포함되어 있지 않을 수 있으므로, 실제 레포의 설정 정책에 맞게 추가하세요.)

---

## 설정(Configuration) 가이드

### 권장: 환경변수/프로필 분리
예시(원하는 형태로 조정):

- `SPRING_PROFILES_ACTIVE=local`
- `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/lifeasgame`
- `SPRING_DATASOURCE_USERNAME=lifeasgame`
- `SPRING_DATASOURCE_PASSWORD=lifeasgame`
- `SPRING_DATA_REDIS_HOST=localhost`
- `SPRING_DATA_REDIS_PORT=6379`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- JWT:
  - `APP_JWT_SECRET=...`

---

## API 문서(Swagger)
SpringDoc OpenAPI UI를 사용합니다(프로젝트 설정에 따라 경로/포트는 다를 수 있음).

- `/swagger-ui/index.html`
- `/v3/api-docs`

---

## 테스트
JUnit5 + (필요 시) Testcontainers 기반.

~~~bash
./gradlew test
~~~

---

## 개발 규칙 (추천)
- **Reader/Writer 분리 유지**
  - 조회는 reader로, 상태 변경은 writer로
- **교차 컨텍스트 참조는 ID로만**
- **외부/내부 호출은 Port(인터페이스)로 끊고 infra에서 구현**
- “멱등성/락/이벤트”는 `platform` 레이어에서 횡단 처리

---

## Roadmap (예시)
- Rule-based 활동 점수 → ML 기반 추천/보상(후순위)
- 경제/거래 기능 고도화(예약/검증/거래 안전장치)
- 소셜(파티/길드/채팅) 실시간 이벤트 확장(WebSocket/Kafka)
- SAO 스타일 프론트 UI(orb nav + panel stack)와 API 계약 확정

---

## License
(TODO)
