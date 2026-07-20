# 운영 데이터베이스 Flyway baseline runbook

## 목적과 절대 원칙

`V1__baseline_current_schema.sql`은 빈 MySQL 8 데이터베이스를 위한 clean-install 전용 스키마이다. 기존 운영 데이터베이스에는 V1 SQL을 실행하지 않는다.

- 애플리케이션 공통 설정은 `spring.flyway.enabled=false` 및 `spring.flyway.baseline-on-migrate=false`를 유지한다.
- 운영 baseline은 승인된 작업 시간에 운영자가 명시적으로 한 번 수행한다. 애플리케이션 시작 과정에서 baseline 또는 migration을 실행하지 않는다.
- `baselineOnMigrate`를 켜서 비어 있지 않은 스키마를 자동 baseline하지 않는다.
- V1과 운영 스키마가 의미상 다르면 baseline을 기록하지 않는다.
- 현재 `application-local.yml`과 `application-prod.yml`의 `ddl-auto`는 이 작업 범위에서 변경하지 않는다. 따라서 비교 작업 중에는 애플리케이션 인스턴스가 대상 복제본 스키마를 변경하지 않도록 격리한다.

## 1. 변경 동결과 full backup

1. DBA, 애플리케이션 담당자, 변경 승인자를 지정하고 작업 시간을 승인받는다.
2. schema 변경과 배포를 동결한다.
3. 운영 DB의 전체 물리 또는 논리 backup을 생성한다. 복구 지점, binlog 위치/GTID, 암호화 키 및 backup 보관 위치를 기록한다.
4. 별도 격리 환경에 backup을 복원해 실제 복구 가능 여부와 소요 시간을 검증한다. 생성만 하고 복원 검증하지 않은 backup은 승인 자료로 인정하지 않는다.

논리 backup 예시(환경의 보안 표준과 MySQL 버전에 맞게 옵션을 조정한다):

```bash
mysqldump --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --no-tablespaces \
  --databases <database> > <approved-secure-path>/before-flyway-baseline.sql
```

비밀번호는 명령행 인자로 전달하지 말고 승인된 option file 또는 secret 주입 방식을 사용한다.

## 2. schema dump 확보

데이터를 제외한 운영 schema를 별도 파일로 추출한다.

```bash
mysqldump --no-data --routines --triggers --events \
  --set-gtid-purged=OFF --no-tablespaces \
  <database> > <approved-secure-path>/production-schema.sql
```

다음 메타데이터도 보관한다.

```sql
SELECT VERSION();
SELECT @@character_set_server, @@collation_server, @@sql_mode, @@time_zone;
SHOW CREATE TABLE <each_application_table>;
SELECT *
FROM information_schema.statistics
WHERE table_schema = DATABASE()
ORDER BY table_name, index_name, seq_in_index;
SELECT *
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE()
ORDER BY table_name, constraint_name;
```

## 3. V1과 semantic diff

1. 같은 MySQL 8 minor/설정의 빈 임시 DB에 V1을 적용한다. 운영 DB에는 적용하지 않는다.
2. 임시 DB의 `SHOW CREATE TABLE`과 `information_schema` 결과를 운영 dump와 비교한다.
3. 포맷, 자동 생성 constraint 이름, `AUTO_INCREMENT` 현재값처럼 동작에 영향이 없는 차이와 의미 차이를 분리한다.
4. `docs/database/current_schema_inventory.md`를 대조표로 사용하되 실제 운영 dump를 최종 근거로 삼는다.

반드시 비교할 의미 항목:

- 모든 애플리케이션 table과 column의 존재 여부
- data type, 길이/precision/scale, unsigned 여부
- nullability와 database default
- primary key 및 `AUTO_INCREMENT`
- unique/index의 column 순서와 prefix, visibility
- foreign key의 참조 대상과 update/delete rule
- native enum 값과 순서
- character set/collation 및 JSON/TEXT 계열 타입
- generated column, trigger, view, routine처럼 JPA inventory에 드러나지 않는 운영 객체

## 4. diff 승인 기준

다음 조건을 모두 만족해야 baseline을 승인한다.

- V1이 요구하는 table/column/PK/unique/FK가 운영에 의미상 모두 존재한다.
- 타입 차이가 읽기·쓰기, 정렬, 범위, precision 또는 Hibernate `validate` 결과를 바꾸지 않는다.
- enum 값의 누락이나 호환 불가능한 값이 없다.
- 운영 전용 index/trigger 같은 추가 객체는 유지 사유와 소유자를 문서화했다.
- 운영 데이터가 V1의 NOT NULL, unique, FK 조건을 위반하지 않는다는 검증 쿼리가 통과했다.
- 복원 시험과 rollback 의사결정 기준이 승인됐다.
- DBA와 애플리케이션 담당자가 diff 결과에 공동 승인했다.

하나라도 충족하지 않으면 baseline을 기록하지 않는다. 먼저 별도 승인된 정합화 작업을 설계하며 V1을 운영에 실행해 차이를 덮지 않는다.

## 5. version 1 수동 baseline 기록

애플리케이션을 중지하거나 DB schema 작업이 발생하지 않도록 격리한 뒤, 승인된 Flyway CLI/운영 도구의 명시적 `baseline` 명령을 사용한다. 아래 값은 예시이며 URL과 credential은 secret 관리 경로에서 주입한다.

```bash
flyway \
  -url='jdbc:mysql://<host>:3306/<database>' \
  -user='<operator>' \
  -baselineVersion='1' \
  -baselineDescription='baseline current production schema' \
  baseline
```

이 명령은 V1 SQL을 실행하는 `migrate`가 아니다. `flyway_schema_history`에 version 1 baseline marker를 명시적으로 기록한다. 직접 `INSERT`로 history row를 만들지 않는다.

즉시 다음을 확인한다.

```sql
SELECT installed_rank, version, description, type, script, checksum, installed_by, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

승인 기준:

- version은 `1`이다.
- type은 baseline marker임을 나타낸다.
- success는 참이다.
- V1 DDL로 생성·변경된 운영 객체가 없다.
- `baseline-on-migrate=false`와 애플리케이션의 `spring.flyway.enabled=false`가 그대로다.

## 6. V2 이상 dry-run

V2 이상의 migration은 운영에 직접 시험하지 않는다.

1. 최신 운영 backup을 격리된 MySQL 8 복제 DB에 복원한다.
2. 운영과 동일한 version 1 baseline marker를 만든다.
3. `flyway validate`로 history와 migration 파일의 일관성을 확인한다.
4. 사용 중인 Flyway edition이 `dryRunOutput`을 지원하면 승인 산출물로 SQL을 생성한다. 지원하지 않으면 이를 우회하지 않고 복제 DB에서 `migrate`를 실행한 뒤 schema/data diff와 실행 시간을 기록한다.
5. DDL lock, table rebuild, disk 증가량, replica lag, timeout을 검토한다.
6. 생성 SQL 또는 복제 DB 결과를 DBA/애플리케이션 담당자가 승인한 후에만 별도 운영 migration 절차를 진행한다.

예시:

```bash
flyway -url='<clone-jdbc-url>' validate
flyway -url='<clone-jdbc-url>' -dryRunOutput='<approved-path>/V2-plus.sql' migrate
```

## 7. 장애 및 rollback

baseline 기록 단계에서 V1은 실행되지 않으므로 애플리케이션 table rollback은 없어야 한다.

- baseline 명령 실패: 애플리케이션 재기동을 중지하고 history table 상태와 Flyway 로그를 보존한다. 부분 history를 임의 수정하거나 재시도하지 말고 DBA가 원인을 판정한다.
- 잘못된 DB에 기록: 즉시 작업을 중단하고 영향 범위를 확인한다. history row/table 삭제는 감사 기록과 승인 후에만 수행한다.
- baseline 후 애플리케이션 장애: Flyway 자동 실행은 꺼진 상태를 유지하고 이전 애플리케이션 버전으로 되돌린다. schema가 바뀌지 않았는지 checksum/schema dump로 확인한다.
- V2 이상 장애: migration별로 사전 승인된 compensating SQL을 사용하거나, 안전한 forward fix가 불가능하면 서비스를 중지하고 검증된 full backup 및 binlog/GTID 지점으로 복구한다. history row만 삭제해 성공을 가장하지 않는다.

복구 완료 후 schema dump, row count/정합성, Flyway history, 애플리케이션 smoke test를 다시 확인하고 incident 기록에 첨부한다.
