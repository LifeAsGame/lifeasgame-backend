# 단일 EC2 운영과 배포

구조는 **인터넷 → Nginx(80/443) → app:8080 → 기존 RDS + Redis**다.
앱과 Redis 포트는 호스트에 공개하지 않는다. DB는 Compose에서 만들지 않는다.
제품 후보 `ddfac6c4`의 Java·migration은 변경하지 않았다. 이 문서는 운영 설치 증거가 아니다.

## 먼저 읽을 순서

1. 이 문서: 구조, 일반 배포, 실패 처리.
2. `compose.yml` → `images.env.example`: 서비스와 호스트 입력.
3. `.github/workflows/cd.yml` → `rollout.sh`: main부터 앱 교체까지.
4. `nginx/https.conf.template` → `nginx/proxy.conf`: 공개 경로와 전달 헤더.
5. **첫 도입에만** [FIRST_INSTALL.md](FIRST_INSTALL.md): 조사·백업·복원·호스트 전환.

## 일반 배포 — 최초 준비 완료 후

`main 반영 → Actions의 해당 SHA 테스트/빌드 → ECR push → digest 확정 →
같은 SHA의 운영 설정 전송 → rollout.sh → 내부 health + 공개 HTTPS 확인` 한 경로다.
수동 운영자가 앱을 교체할 때도 같은 `rollout.sh`만 사용한다. CD에 교체 로직을 복사하지 않는다.
기존 IMAGE_TAG/.env 수정, localhost:8080 검사, latest 이동, image prune, workflow_dispatch는 제거했다.
현재 main에는 여전히 과거 CD가 있다. 이 PR을 main에 반영하기 전에는 과거 CD를 실행하면 안 된다.

- Actions는 **main의 정확한 SHA**에서 `clean test build`를 실행한다. PR CI와 merge SHA를 혼동하지 않는다.
- 태그는 `SHA-runID-attempt`이고 실행 기준은 오직 `repository@sha256:digest`다.
  이미지 revision label, Actions summary, 호스트 `deployed.txt`에 SHA↔digest를 기록한다.
  기존 local image ID는 이 새 registry digest의 검증 증거가 아니다.
- `git archive SHA`로 ops와 migration을 `/opt/lifeasgame/releases/SHA-runID-attempt/`의
  **새 디렉터리**에 보낸다. 기존 파일/비밀값/legacy checkout은 덮어쓰지 않는다.
- GitHub concurrency는 단일 production 그룹, `cancel-in-progress:false`다.
  대기 실행은 GitHub 정책에 따라 교체될 수 있지만 진행 중인 배포를 새 push가 취소하지 않는다.
  호스트 `flock`도 수동 교체 및 인증서 갱신과 충돌을 막는다.
- 스크립트는 기존 Redis가 healthy이고 image/volume/project가 보존됐는지 확인한다.
  Redis 교체/새 volume 생성은 일반 앱 배포에 포함하지 않는다.
- 이전 컨테이너 설정·image archive/hash를 보관한 후 검증된 digest를 pull한다.
  앱만 교체하고 Compose health가 통과하면 같은 release의 Nginx 설정을 적용한다.
- 공개 `/actuator/health`, `/ws/info`의 HTTPS 성공과 `/admin/v1/users` 404를 확인한다.
  성공한 release만 `current`로 지정한다. 실제 로그인/CORS/STOMP 의미 검증은 공개 QA에서 별도로 한다.

## 배포를 여는 조건

2026-10-14 읽기 조회: GitHub Environment **0개**, repository variables **0개**.
조회한 사용자는 admin 권한이 있지만 **설정은 변경하지 않았다**. main/develop ruleset에는
PR 요구/삭제·force-push 제한이 있고 required status-check 규칙은 반환되지 않았다.
이 프로젝트의 CI 완료 정책은 계속 적용한다. CodeRabbit의 Draft skip은 코드 승인 증거가 아니다.

운영자가 후속 배포 승인 아래 다음을 준비하고 다시 읽어 확인해야 한다.

1. `production` Environment를 만들고 main만 허용한다. 이 공개 저장소에서 지원되는 required
   reviewer를 지정하고 self-review/bypass 허용 여부를 결정·기록한다. 단일 운영자가 직접 승인해야
   한다면 실제 가능한 reviewer 정책을 먼저 정한다. 이름만 만들면 승인 보호가 생기는 것이 아니다.
2. 기존 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_USER`,
   `EC2_SSH_KEY`를 적절한 Environment secret으로 이관/범위 확인한다. `EC2_KNOWN_HOSTS`는
   신뢰한 console/기존 host-key 근거에서 준비한다. ssh-keyscan만으로 신뢰하지 않는다.
3. 실제 backup/restore/JPA/이전 image 호환과 호스트 전환을 끝낸 뒤 `shared/host-ready`에
   `backup-restore-host-approved-v1`을 기록한다. 증거 위치·승인자·날짜는 private 작업 기록에 남긴다.
4. 승인된 운영 schema에 대응하는 정확한 migration 파일 manifest를
   `shared/approved-migrations.sha256`에 둔다. 새 migration 또는 기존 bytes 변경은 CD를 중단시킨다.
5. 마지막으로 repository variable `PRODUCTION_DEPLOYMENT_ENABLED=true`를 설정한다.
   미설정/false이면 **빌드·ECR push·배포 모두 skip**한다. 이 값은 준비 완료 증명 자체가 아니다.

호스트의 `shared`는 0700, `host.env`/app 비밀/증거는 0600이고 승인된 운영자만 관리한다.
실제 `APP_ENV_FILE`은 DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET 네 키만 허용한다.
Compose `format:raw`로 비밀의 `$`를 보존한다. env 파일을 shell source하지 않는다.
기존 Redis에 ACL/password가 있으면 이 무인증 내부 Redis 구성으로 대체하지 말고
실제 설정을 반영한 별도 작은 변경을 검토한다. 모르는 volume/project를 추정하지 않는다.

## 재시작, migration, 로그 정책

| 항목 | 정책과 한계 |
|---|---|
| 조사/격리 복원 | `restart:no`; 자동 writer를 차단하고 사건 발생 시 조사 중단 |
| 상시 app/Redis/Nginx | `unless-stopped`; Docker 서비스가 부팅 시 시작되도록 확인. 수동 정지한 앱은 재부팅에도 정지 유지 |
| Redis 대기 | Compose health + 앱 depends_on. daemon 재부팅에서는 Compose 의존 순서를 보장하지 않으므로 앱 시작 실패는 restart로 재시도; 실제 host reboot 검증은 설치 후 필요 |
| 앱 장애 | 프로세스 exit는 Docker가 재시작. 살아 있지만 unhealthy인 프로세스는 자동 재시작하지 않음; 사용자가 상태/로그를 확인하고 통제된 재시작 |
| Flyway | **별도 승인된 migration 단계만** 사용. 앱과 CD는 항상 OFF/JPA validate. 실제 이력·복원 검증 전에 운영 migrate 없음 |
| DB pool | 임의 4/2 override 삭제. 기존 prod 20/10과 공통 timeout을 그대로 사용. t3.micro 용량 적합성은 실측 전 미확인 |
| 요청 크기/timeout | 임의 2MiB, 전체 API 1시간/send timeout, 전체 buffering OFF 삭제. Nginx 기본값(본문 1MiB, 보통 60초)을 사용; 긴 `/ws` 연결만 read timeout 3600초 유지 |
| 진단 로그 | 시간/method/status/bytes/응답시간/upstream status만 access log. URI/query/IP/인증 header/body는 기록하지 않음. 요청 원문을 포함할 수 있는 Nginx raw error log는 억제; 설정 오류는 `nginx -t` private 결과, 502/504는 status/timing와 Docker/앱 로그로 진단 |
| 보관 | Docker json-file 10MiB×3은 8GiB host 디스크 고갈 방지. 앱 prod의 마스킹/SQL bind OFF 유지; private 원본 로그는 공유 전 검토. 모든 앱 로그의 무민감성/전체 보안검증을 주장하지 않음 |

인증서 갱신은 systemd timer가 하루 두 번 확인하고 성공 후 `nginx -t`/reload한다.
별도 shell wrapper 없이 service에 세 명령만 있다. Certbot에 Docker socket을 주지 않는다.
실패하면 service는 failed이고 reload하지 않아 기존 인증서/앱을 유지한다.
비공개 운영 승인 기록에 지정한 사용자가 `systemctl status lifeasgame-cert-renew.service`, `journalctl -u ...`,
`systemctl list-timers`, 인증서 만료일을 확인한다. 최소 매주 및 배포 후 점검하고
잔여 30일 이하/renew 실패는 그날 수정·dry-run한다. 외부 알림/24시간 자동 대응은 구현하지 않았다.

## 공개 경로와 FE 영향

Nginx 공식 entrypoint가 `${BE_DOMAIN}` 하나만 치환한다(`NGINX_ENVSUBST_FILTER=^BE_DOMAIN$`).
도메인은 guard가 검증하며 `$host`, `$request_uri`, `$http_upgrade` 등은 그대로 남는다.
Python renderer는 삭제했다. forwarded header는 client 값을 덮어써 위조를 막는다.
API `/api/v1/*`, `/errors/*`, SockJS `/ws`·`/ws/*`, status-only health만 전달한다.
HTTPS/자동 갱신, Origin/Authorization, HTTP fallback와 WebSocket Upgrade를 유지한다.
CORS는 Nginx에서 중복 구현하지 않고 BE의 정확한 FE origin allowlist가 소유한다.

**`/admin/v1/**`는 공개 404다.** FE `/admin` 화면이 열려도 관리자 catalog/definition API는
동작하지 않는다. 일반 사용자 API와 별개다. 현재 운영 목표에 공개 관리자 기능은 포함하지 않으며,
필요 시 인증된 사설 경로 또는 별도 검토된 ADMIN 공개 정책을 먼저 정한다. Mock fallback은 없다.

## 실패 후 무엇이 남는가

- 입력/schema/Redis/pull/설정 검사 실패: 이전 앱을 건드리기 전에 종료한다.
- 앱 교체 이후 health·공개 경로 실패 또는 신호: candidate restart를 no로 바꾸고 stop한다.
  이전 image로 자동 재실행하지 않는다. stop 요청/실제 exited 상태는 다르므로 반드시 inspect한다.
  원격 단절/강제 kill/호스트 장애까지 자동 복구를 보장하지 않는다.
- `current`는 이전 성공 release, 실패 release와 recovery 디렉터리도 남는다.
  DB/Redis volume과 인증서는 삭제하지 않는다. 이미지 prune/down -v 없음.
- Nginx 적용 후 실패라면 proxy는 새 설정일 수 있다. previous release의 설정 경로가 남으므로
  호환성 확인 뒤 함께 복구한다. 단일 app 교체는 잠깐 중단될 수 있으며 무중단이 아니다.
- MySQL DDL은 일부 commit될 수 있다. migration 실패 시 쓰기를 차단하고 실제 schema를 조사한다.
  이전 image가 변경 schema에서 검증됐을 때만 보관한 `previous-compose.json`으로 app/필요 proxy를
  복구한다. 저장 image를 load하면 RepoDigest가 없을 수 있어 기록한 image ID를 사용한다.
- 비호환이면 검증된 DB backup + 맞는 image/config를 별도 DB 복원 승인 아래 함께 복구한다.
  **백업 이후 쓰기 손실**을 설명하고 동의를 받는다. Redis 전체 flush는 하지 않는다.

## 파일별 역할

| 파일 | 실행 주체·시점 / 변경 대상 | 유지 이유 |
|---|---|---|
| README.md | 운영자, 평소 / 없음 | 구조·반복 배포의 첫 안내 |
| FIRST_INSTALL.md | 운영자, 최초·schema 변경 / 승인 단계별 | 긴 조사/복원을 일상 절차에서 분리 |
| compose.yml | rollout/Docker, 운영 / app·proxy, 최초 Redis | 단일 서비스 정의 |
| images.env.example | 운영자, 최초 / private host.env 입력 | digest·경로 예시, 비밀 없음 |
| nginx/https.conf.template | Nginx entrypoint, 시작 / proxy 설정 | 도메인만 표준 치환 |
| nginx/bootstrap.conf.template | 운영자, 최초 ACME / HTTP만 | 인증서 없는 첫 발급에만 필요 |
| nginx/proxy.conf | Nginx, 요청 / 전달 헤더·upstream | 중복 location 설정 공유 |
| lifeasgame-cert-renew.service | systemd, timer / cert와 reload | 짧은 갱신 절차; wrapper 통합 |
| lifeasgame-cert-renew.timer | systemd, 매일 / service 실행 | 자동 갱신 일정 |
| validate-config.py | rollout/검증자, 시작 전 / 없음 | 비밀 override·공개 포트·mutable image·도메인 주입 거부; env 허용키는 Compose에서 읽음 |
| rollout.sh | Actions/승인된 운영자, 매 배포 / 앱·proxy·release 기록 | 유일한 교체/실패 처리 |
| backup.sh | 승인된 운영자, writer freeze 후 / 새 private dump | 실패 감지·gzip/hash·완료명 구분 |
| sql/inspect.sql | backup/운영자, 최초 / 읽기 | engine·table·event·history 유무 |
| sql/flyway-history.sql | backup/운영자, history 있을 때 / 읽기 | 없는 테이블 조회 실패와 이력 이상 구분 |
| rehearsal.yml | 운영자, 백업 후 / 새 로컬 MySQL·Redis | internal network·port 없음·restart no |
| init-rehearsal.py | 운영자, 복원 시작 / 새 private 자격 | 운영 비밀 재사용·덮어쓰기 방지 |
| test_preparation.py | CI/개발자, 변경 / 임시 입력·가짜 client | 설정·실패 backup 회귀 |
| test_proxy.py | 개발자, proxy 변경 / 전용 Docker·테스트 TLS | 실제 HTTPS·Upgrade·교체·로그 검사 |
| .github/workflows/ci.yml | GitHub, PR / 테스트 산출물 | 제품/운영 회귀, 운영 배포 없음 |
| .github/workflows/cd.yml | GitHub, 승인 gate 이후 main / image·release | 빌드/전달만, 교체는 rollout 호출 |

`render-nginx.py`와 `renew-certificates.sh`는 표준 template/systemd에 통합해 삭제했다.
검증·백업·배포를 거대한 shell 한 파일에 합치지 않았다. 최초 운영 복구 증거는 아직 없다.
