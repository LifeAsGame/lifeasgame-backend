# 운영 데이터베이스 Flyway baseline runbook

## 목적과 절대 원칙

`V1__baseline_current_schema.sql`은 빈 MySQL 8 데이터베이스를 위한 clean-install 전용 스키마이다. 기존 운영 데이터베이스에는 V1 SQL을 실행하지 않는다.

- Flyway만 local/prod schema 변경 권위를 가진다. Hibernate는 `ddl-auto=validate`로 mapping 일치 여부만 확인한다.
- 공통 설정은 `spring.flyway.enabled=false`, `locations=classpath:db/migration`, `baseline-on-migrate=false`이다.
- local은 Flyway가 활성화되고 prod는 `FLYWAY_ENABLED` 환경변수로 명시적으로 활성화한다.
- 운영 baseline은 `FLYWAY_ENABLED=false` 상태의 승인된 작업 시간에 운영자가 명시적으로 한 번 수행한다. 애플리케이션 코드가 baseline을 수행하지 않는다.
- `baselineOnMigrate`를 켜서 비어 있지 않은 스키마를 자동 baseline하지 않는다.
- V1과 운영 스키마가 의미상 다르면 baseline을 기록하지 않는다.
- local/prod에서 `ddl-auto=update`, `create`, `create-drop`을 사용하지 않는다.

## 실제 Profile 설정

| Profile | Flyway | Hibernate | 자동 baseline |
|---|---|---|---|
| 공통 | `enabled=false` | `ddl-auto=none` | 금지 |
| local | `enabled=true` | `ddl-auto=validate` | 금지 |
| prod | `enabled=${FLYWAY_ENABLED:false}` | `ddl-auto=validate` | 금지 |

모든 Profile의 Flyway location은 `classpath:db/migration`이며 `baseline-on-migrate=false`이다. 기존 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` datasource 환경변수 계약은 그대로 사용한다.

## Local 신규 DB

1. 빈 MySQL 8 database를 생성한다.
2. `local` Profile로 애플리케이션을 기동한다.
3. Flyway가 V1부터 최신 version까지 순서대로 적용한다.
4. Flyway 완료 후 Hibernate `validate`가 Entity mapping과 schema 일치를 확인한다.
5. `flyway_schema_history`의 version, checksum, success를 확인한다.

빈 DB가 아닌데 history가 없다면 local 기동은 실패해야 한다. 이 실패를 피하려고 `baseline-on-migrate=true`로 바꾸지 않는다.

## Local 기존 DB

다음 두 경로 중 하나만 선택한다.

### 경로 A: 백업 후 재생성

1. 필요한 local 데이터를 백업한다.
2. database를 재생성한다.
3. `local` Profile을 기동해 V1부터 최신 migration을 적용한다.
4. 필요한 데이터를 호환성을 확인하며 복원한다.

### 경로 B: semantic diff 후 명시적 baseline

1. 기존 local database를 백업한다.
2. 아래 운영 절차와 같은 기준으로 V1과 semantic diff를 수행한다.
3. V1과 의미상 동등한 경우에만 Flyway CLI/API의 명시적 `baseline`으로 version 1 marker를 만든다.
4. `local` Profile을 기동해 V2 이상을 적용하고 Hibernate `validate`를 통과시킨다.

애플리케이션 설정을 통한 자동 baseline이나 `flyway_schema_history` 직접 INSERT는 허용하지 않는다.

## Production Cutover 순서

다음 순서를 바꾸지 않는다.

```text
FLYWAY_ENABLED=false
→ backup 및 restore 검증
→ V1 semantic diff
→ version 1 explicit baseline
→ 격리 clone에서 V2+ migration 및 validate 검증
→ DBA/애플리케이션 담당자 승인
→ FLYWAY_ENABLED=true
```

## 운영 1. 변경 동결과 full backup

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

## 운영 2. schema dump 확보

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

## 운영 3. V1과 semantic diff

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

## 운영 4. diff 승인 기준

다음 조건을 모두 만족해야 baseline을 승인한다.

- V1이 요구하는 table/column/PK/unique/FK가 운영에 의미상 모두 존재한다.
- 타입 차이가 읽기·쓰기, 정렬, 범위, precision 또는 Hibernate `validate` 결과를 바꾸지 않는다.
- enum 값의 누락이나 호환 불가능한 값이 없다.
- 운영 전용 index/trigger 같은 추가 객체는 유지 사유와 소유자를 문서화했다.
- 운영 데이터가 V1의 NOT NULL, unique, FK 조건을 위반하지 않는다는 검증 쿼리가 통과했다.
- 복원 시험과 rollback 의사결정 기준이 승인됐다.
- DBA와 애플리케이션 담당자가 diff 결과에 공동 승인했다.

하나라도 충족하지 않으면 baseline을 기록하지 않는다. 먼저 별도 승인된 정합화 작업을 설계하며 V1을 운영에 실행해 차이를 덮지 않는다.

## 운영 5. version 1 수동 baseline 기록

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
- `baseline-on-migrate=false`와 `FLYWAY_ENABLED=false`가 그대로다.

## 운영 6. clone에서 V2 이상 검증

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

clone 검증에서는 V2부터 최신 version까지 적용된 뒤 운영과 같은 Hibernate `ddl-auto=validate`로 애플리케이션 Context가 정상 기동하는지 확인한다. Reward seed, unique/FK/check constraint와 Flyway history도 함께 검증한다.

## 운영 7. Production Flyway 활성화

1. backup/restore, semantic diff, explicit baseline, clone 검증 산출물의 최종 승인을 확인한다.
2. 운영 DB의 version 1 baseline marker와 checksum/history 상태를 다시 확인한다.
3. 배포 환경에 `FLYWAY_ENABLED=true`를 명시한다.
4. 애플리케이션 기동 시 Flyway가 V2부터 최신 version까지 적용하고, 완료 후 Hibernate `validate`가 실행되는지 로그와 history로 확인한다.
5. migration 또는 validation이 실패하면 애플리케이션 기동 실패를 정상 안전 동작으로 간주하고 우회 기동하지 않는다.

`FLYWAY_ENABLED`를 생략하면 기본값은 `false`이다. 애플리케이션이 baseline을 자동 생성하는 경로는 없으며, 운영자가 사전에 만든 version 1 marker만을 신뢰한다.

## 운영 8. 장애 및 rollback

baseline 기록 단계에서 V1은 실행되지 않으므로 애플리케이션 table rollback은 없어야 한다.

- baseline 명령 실패: 애플리케이션 재기동을 중지하고 history table 상태와 Flyway 로그를 보존한다. 부분 history를 임의 수정하거나 재시도하지 말고 DBA가 원인을 판정한다.
- 잘못된 DB에 기록: 즉시 작업을 중단하고 영향 범위를 확인한다. history row/table 삭제는 감사 기록과 승인 후에만 수행한다.
- baseline 또는 활성화 후 애플리케이션 장애: `FLYWAY_ENABLED=false`로 되돌리고 이전 애플리케이션 버전으로 복구한다. schema가 바뀌지 않았는지 checksum/schema dump로 확인한다.
- V2 이상 장애: migration별로 사전 승인된 compensating SQL을 사용하거나, 안전한 forward fix가 불가능하면 서비스를 중지하고 검증된 full backup 및 binlog/GTID 지점으로 복구한다. history row만 삭제해 성공을 가장하지 않는다.

복구 완료 후 schema dump, row count/정합성, Flyway history, 애플리케이션 smoke test를 다시 확인하고 incident 기록에 첨부한다.
