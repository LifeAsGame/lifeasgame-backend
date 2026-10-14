# 최초 도입: 조사 → 복원 → 호스트 전환

아래는 실행 준비이며 운영 승인/운영 검증 결과가 아니다. 도메인이 없어도 조사·로컬 복원 준비는 진행한다.
ALB/NAT/새 EC2/Kubernetes/관리 서버는 추가하지 않는다. 기존 19081·13005와 DB/Redis를 보존한다.

## 1. 조사창 — 아직 승인되지 않음

대상 계정·리전·EC2/RDS 식별자와 최신 읽기 조회 결과는 비공개 운영 승인 기록에서 확인한다.
공개 저장소에는 실제 자원 ID, 개인 경로, 키 파일 위치를 기록하지 않는다.
자동 재시작 시각, 엔진·지원 상태, 예약 작업은 승인 직전에 다시 확인한다.
OS system-update 알림만으로 엔진 변경 여부를 판단하지 않는다.

기존 1h EC2/2h RDS 안은 host 조사 후 RDS 시작·덤프·중지 완료를 보장하지 못한다.
**수정 제안: EC2 목표 최대3h, RDS 목표 최대2h**, start 요청부터 시간 기록.
정상 순서: EC2 시작→20분 내 host 통제→RDS 시작→30분 내 available→30분 이내 read/dump/전송→
RDS 시작 후60분 이내 stop 요청→실제 stopped 확인→EC2 stop 및 stopped 확인.
EC2 150분부터 무조건 종료, 180분 내 stopped 목표다. RDS 기동이 30분을 넘으면 SQL을 포기하고
가능한 즉시 stop한다. starting 상태는 stop이 거부될 수 있다. AWS 기동/중지 지연 때문에
시간·과금 한도를 기술적으로 강제할 수 없으며, 지연 시 새 작업 없이 중지 재시도/상태 확인만 지속한다.
RDS가 멈추지 않아도 EC2를 계속 켜 두지 말고 격리를 유지하며 EC2 종료 요청을 진행한다.

**누가 감시하는가:** 승인된 창에서 Codex가 기존 SDK/CLI 또는 기존 AWS 콘솔로 실행·30초 간격
Describe 조회·UTC/KST 기록을 맡는다. 작업을 승인한 사용자는 시작 전에 휴대전화 알람(EC2 +75/+150/+180분,
RDS +30/+60/+120분)을 설정하고 독립된 휴대전화 AWS 콘솔 로그인/해당 두 자원의 Stop 권한을 확인한다.
Mac/대화 연결이 끊기면 해당 사용자가 RDS→EC2 중지와 실제 stopped를 확인한다. API 응답의 `stopping`을
완료로 기록하지 않는다. 실명·연락 수단·기한은 비공개 승인 기록에 지정한다.
이 인계/권한 확인을 하지 못하면 창을 시작하지 않는다.

기존 같은 날 공식 요율을 재사용한 compute/IP 추정은
`3×($0.013+$0.005)+2×$0.026 = $0.106`(기존 $0.070에서 증가).
여유 포함 **세전 $1 이내 예상**은 강제 상한이 아니다. 최소 과금, 기동·중지 지연, CPU credits,
다른 AZ 전송/인터넷 dump 전송, 로그, 계속 부과되는 EBS/RDS/ECR 보관료를 포함해 실제 청구는 달라진다.
엔진/지원 등록 변경·스냅샷 생성·새 유료 자원은 제외한다. 예상 초과 시 작업을 중단한다.
[AWS RDS 시작/중지 한계](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_StopInstance.html).

### 기동 전 확인 순서

1. 기존 자원/엔진/예약 작업과 stop 수단을 재확인한다. MySQL8 지원 종료 후 disabled enrollment의
   자동 major 변경 위험은 AWS instance-specific 이벤트/예약/지원 답변으로 해소해야 한다.
   현재 DescribeDBEngineVersions/DescribeEvents/DescribePendingMaintenanceActions 권한 거부는
   “변경 없음”이 아니다. 콘솔을 보완 조회하되 위험을 해소하지 못하면 **EC2 조사만**, RDS는 시작하지 않는다.
2. RDS 자체 event_scheduler startup 값 또는 enabled event 부재를 기존 운영 증거/parameter group으로
   확인한다. DB를 켠 뒤 SQL로 확인하는 것은 startup 쓰기 예방을 대체하지 못한다.
   값이 Engine default로만 나오고 이력이 없으면 **RDS gate 미충족**이다. 임의 parameter/엔진 변경 없음.
3. 비공개 승인 기록에 지정한 EC2 SG의 TCP80/443/8080 세 규칙과 RDS SG의
   TCP3306 from 해당 EC2 SG 한 규칙만 원본 source/description/ports와 함께 private JSON으로 보관한다.
   승인 후 이 네 규칙만 회수/readback. SG 전체 교체/SSH22 차단은 하지 않는다. 다른 writer 경로가 있으면 중단.
4. RDS stopped 상태에서 EC2만 기동한다. 새 public IP와 신뢰한 SSH host key를 확인한다.
   비공개 승인 기록에서 확인한 기존 SSH 개인키(0400)만 사용하며 StrictHostKeyChecking=yes.
5. systemd enabled services/timers, root/ubuntu cron, cloud-init 이력/rc.local, Docker 모든 컨테이너의
   image/restart/mount/port, Java 프로세스, 조사한 기존 설치 경로의 Compose, proxy/인증서/Redis를 조사한다.
   `.env`, inspect, compose config 원문은 출력하지 않고 0700/0600로 보관한다.
6. 식별된 app/job만 가역 stop/mask/cron 중단/restart=no 처리한다. PID/포트/timer/재시작을 재확인한다.
   old main은 Hibernate update, candidate도 Outbox/예약 만료/조건부 bootstrap writer가 있다.
   demo OFF/Flyway OFF만으로 안전하지 않다. boot의 Redis/file 쓰기나 외부 송신까지 차단했다고 주장하지 않는다.
7. 엔진/DB event/host writer gate 모두 통과할 때만 RDS를 기동한다. engine/available을 재확인하고
   원래 3306 규칙만 복구하여 SQL client에 연결한다. 공용 80/443/8080은 계속 차단한다.
   시작 후 engine 변화·예상 밖 event/writer/DDL을 발견하면 SQL/백업을 중단한다.
8. 완료/실패/기한에 writer 통제를 유지하고 RDS→EC2 stop, **각 stopped 도달**을 기록한다.
   양쪽 중지 후 원본 네 규칙과 설정을 안전하게 복구한다. 다음 자동 기동이 writer를 실행할 우려가
   남으면 격리를 유지하고 차이를 보고한다. 안전하지 않은 원복을 완료로 표시하지 않는다.

## 2. 실제 운영 백업

`sql/inspect.sql`과 history가 있을 때 `sql/flyway-history.sql`만 먼저 읽는다.
missing/failed/checksum mismatch/unmanaged schema는 migration 차단이며, 일관된 백업 자체는 가능하다.
V1 강제 실행/baseline/repair/clean 금지. 모든 table InnoDB, app/DB event/DDL writer freeze,
SELECT/SHOW VIEW/TRIGGER/EVENT/routine 권한과 정확한 범위를 확인한다. 통제 불가면 안전한 백업이라고 추정하지 않는다.

기존 MySQL8.0 client와 private defaults file(0600)에 RDS hostname, 기존 자격, CA 경로,
`ssl-mode=VERIFY_IDENTITY`를 넣는다. localhost 터널 때문에 hostname 검증을 끄지 않는다.
호스트 private 경로에서:

```sh
WRITERS_AND_DDL_FROZEN=yes bash ops/production/backup.sh /secure/read.cnf /secure/backup-new
```

script의 pipefail·partial·gzip/hash 검사를 유지한다. 크기/소요시간을 관찰하고 논리 dump **1GiB 이하**에서만
진행한다. 경고/누락 객체는 성공으로 취급하지 않는다. 정확한 row count, routines/triggers/events/FK/unique
inventory와 engine/history/UTC 시각을 private 기록에 남긴다. TABLE_ROWS는 추정치다.
전송 후 `gzip -t`, `shasum -a 256 -c SHA256SUMS`를 다시 실행한다.
목적지는 FileVault를 확인한 새 비공개 로컬 백업 디렉터리(0700/0600)다. 실제 경로는 비공개 기록에만 남긴다.
Git/PR/공유 로그에 dump를 올리지 않는다. 검증 전 유일한 사본을 삭제하지 않는다.

## 3. 로컬 격리 복원과 migration/JPA/이전 image

실제 운영 백업을 얻은 뒤 AWS는 stopped로 두고 진행한다. 이 단계는 현재 NOT RUN이다.
`images.env.example`의 MySQL/Flyway digest는 도구 예시다. 실제 engine이 다르면 먼저 일치시킨다.
새 private rehearsal env에 사용 이력 없는 REHEARSAL_PROJECT, REHEARSAL_DIR, 정확한 CANDIDATE_DIR, 이미지들을 지정하고 필요한 image를 미리 pull한다.

```sh
umask 077
python3 ops/production/init-rehearsal.py "$REHEARSAL_DIR"
rc=(docker compose --env-file "$REHEARSAL_ENV" -f ops/production/rehearsal.yml)
"${rc[@]}" up -d --wait mysql redis
# inspect: 새 volume/network, internal=true, host ports 없음, event_scheduler OFF
set -o pipefail
gzip -dc "$VERIFIED_PRODUCTION_DUMP" | "${rc[@]}" exec -T mysql mysql --defaults-extra-file=/run/secrets/client.cnf lifeasgame
"${rc[@]}" run --rm --no-deps flyway info > "$REHEARSAL_DIR/info.log" 2>&1
"${rc[@]}" run --rm --no-deps flyway validate > "$REHEARSAL_DIR/validate.log" 2>&1
# 실제 pending의 DDL/backfill/충돌/lock/시간 검토 후 이 로컬 사본에만 실행
"${rc[@]}" run --rm --no-deps flyway migrate > "$REHEARSAL_DIR/migrate.log" 2>&1
"${rc[@]}" run --rm --no-deps flyway validate > "$REHEARSAL_DIR/validate-after.log" 2>&1
"${rc[@]}" run --rm --no-deps flyway info > "$REHEARSAL_DIR/info-after.log" 2>&1
"${rc[@]}" run --rm --no-deps jpa > "$REHEARSAL_DIR/jpa.log" 2>&1
```

복원 직후 backup 당시 exact counts/객체/FK/unique/history와 비교한다. migration 후 pending/failed=0,
JPA validate 성공/정상 종료가 필요하다. 180초 이상 JPA 도구가 남으면 정지·조사한다.
`prod,catalog-import`는 scheduler 제외용 로컬 profile이고 import-file 없음, bootstrap/outbox/pubsub/demo OFF다.
실제 service profile은 prod만 사용한다. 과거 합성 1행/빈 DB52개 pending 검증은 운영 증거가 아니다.

업그레이드한 DB를 **두 번째 새 project/volume**에 복원해 실제 호스트에서 보관한 이전 image를 검증한다.
Hibernate validate/Flyway OFF/egress 차단 아래 schema 전후 비교·일반 인증/소유 조회·필요 domain smoke를 한다.
이전 image의 writer 억제 기능이 없으면 그 사본 안 쓰기만 허용하며 원래 운영/19081에는 연결하지 않는다.
실패하면 image-only rollback을 금지하고 DB+image/config 복원 및 backup 이후 손실 계획을 명시한다.

콘텐츠는 `docs/contracts/content-fill-authority-20261013.md`의 승인 stable-code 카탈로그와
기존 checksum이 기록된 HRDK manifest만 별도 dry-run/apply 승인 대상으로 검토한다.
19081 전체 데이터/Users/Players/지갑/소유/기록/검증 계정/fixture/시연 기록을 복사하지 않는다.
V52 기존 Player reconciliation도 별도 명시적 승인이다. bootstrap은 계속 OFF다.

## 4. 최초 호스트 전환 — 조사 승인과 별도

실제 backup/restore/호환 검증, 공개 HTTPS 입력, migration/ECR/배포 승인 이후에만 한다.
입력은 한 묶음: **소유 BE hostname, DNS 공급자·레코드를 관리할 사람, 인증서 email**.
DNS 구매/변경은 이 준비 작업에서 하지 않는다. 실제 A/AAAA/CAA와 EC2 새 주소를 확인한다.
기존 proxy/Redis가 발견되면 설정을 조사하고 재사용 계획을 반영한 뒤 전환한다.
이 Compose를 정체 모를 legacy 파일 위에 덮어쓰지 않는다.

1. 기존 project명, volume명, Redis version/ACL/설정, image/config/env/proxy/cert/restart를 보관한다.
   `/opt/lifeasgame/shared/legacy-recovery/`에 image archive 및 원본 config의 SHA256SUMS를 만든다.
   검증된 DB backup과 손실창도 기록한다. disk는 새 이미지+이전 archive+dump 여유가 있어야 한다.
2. 새 `/opt/lifeasgame/{shared,releases}`를 명시적으로 만들고 권한/운영자 소유를 정한다.
   `host.env`는 images 예시에서 준비하며 APP_IMAGE는 최초 승인 digest, project/volume은 **조사한 값**이다.
   기존 Redis를 stop한 후에만 같은 external volume을 연결한다. 두 Redis process에 동시 attach 금지.
   기존 네트워크/project 충돌은 여기서 해결하고 legacy writer container는 보존 archive 확인 후 제거/이관한다.
3. 정확한 SHA archive를 `releases/SHA-first/`에 풀고 `SOURCE_SHA`를 기록한다. CA truststore는 공식
   [RDS CA bundle](https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem)의 검증한 public cert를
   PKCS12로 import한다. DB_URL에 실제 RDS hostname, `sslMode=VERIFY_IDENTITY`,
   `trustCertificateKeyStoreUrl=file:/run/rds-truststore.p12`, type PKCS12/password changeit를 지정한다.
   truststore에는 DB 비밀이 없다. DB 자격은 별도 app.env에만 두고 readonly mount한다.
4. 공개 승인 범위에서 인증서를 최초 발급한다. 아래 환경값은 host.env와 대조한 공개 경로·digest다.
   bootstrap은 app 없이 HTTP ACME만 열며 발급 후 제거한다. 기존 proxy가 80을 사용하면 먼저 계획을 조정한다.

```sh
docker run -d --name lifeasgame-acme-bootstrap -p 80:80 \
  -e BE_DOMAIN="$BE_DOMAIN" -e 'NGINX_ENVSUBST_FILTER=^BE_DOMAIN$' \
  -v "$PWD/ops/production/nginx/bootstrap.conf.template:/etc/nginx/templates/default.conf.template:ro" \
  -v "$TLS_STATE_DIR/webroot:/var/www/certbot:ro" "$NGINX_IMAGE"
pc=(docker compose --env-file /opt/lifeasgame/shared/host.env -f ops/production/compose.yml)
"${pc[@]}" run --rm --no-deps certbot certonly --webroot -w /var/www/certbot \
  --domain "$BE_DOMAIN" --email "$CERTIFICATE_EMAIL" --agree-tos --non-interactive
docker stop lifeasgame-acme-bootstrap
docker rm lifeasgame-acme-bootstrap
```

5. 필요한 실제 pending migration은 writer freeze/fresh backup 후 **별도 Flyway11.7.2 단계**로만 적용한다.
   승인된 SQL 디렉터리와 private production flyway.conf(readonly, RDS TLS hostname 검증)를 도구에 mount하고
   info/validate→승인 pending 재대조→migrate→validate/info를 기록한다. app bootstrap으로 실행하지 않는다.
   migration 없는 배포는 이 단계를 생략한다. 실패 시 repair/retry 자동화 없이 실제 DDL 상태를 조사한다.
6. 검증된 schema에 해당하는 source 루트에서 `shasum -a 256 src/main/resources/db/migration/*.sql`을
   `shared/approved-migrations.sha256`으로 보관한다. backup/restore/호스트 증거가 모두 있을 때만
   `shared/host-ready`를 기록한다. 이는 이번에 생성/활성화하지 않는다.
7. 조사한 external Redis volume으로 `"${pc[@]}" up -d --wait redis` 후 기존 writer가 없는지 확인한다.
   이후 `bash ops/production/rollout.sh "$SOURCE_SHA" "$APP_IMAGE"`로 첫 앱을 올린다.
   이후 main CD도 같은 script만 사용한다. first install용 영구 수동 배포 경로는 없다.
8. `current` 성공 후 service/timer를 `/etc/systemd/system`에 설치하고 Docker executable 경로를 확인한다.
   `systemd-analyze verify`→daemon-reload→timer enable, `certbot renew --dry-run`과 nginx reload를 검증한다.
   인증서 경로는 release 밖 `TLS_STATE_DIR`이므로 앱 교체에 지워지지 않는다.
9. 공개 TLS chain/hostname/만료·HTTP redirect·health·CORS 허용/거부·ordinary login/refresh·demo OFF·
   관리자 차단·SockJS HTTP fallback·WSS101/STOMP CONNECTED/MESSAGE/reconnect/타인 channel 거부를 검증한다.
   FE 채팅 전송은 REST다. 결과가 있어야 verified BE origin을 FE에 인계한다.
10. 이후에만 README의 Environment/활성화 gate를 열고 정상 main 배포를 사용한다.

GitHub settings, ECR push, 실제 migration/catalog/DB restore, DNS/TLS 공개, main 반영/CD 실행,
FE 승격과 상시 운영은 **이번 조사창 승인과 별도**다. 아직 productionBackupReady/restoreReady/releaseReady=false.
