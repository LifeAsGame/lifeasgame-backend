#!/usr/bin/env bash
# One deployment path. Migrations are separately reviewed; this never migrates/restores/prunes.
set -Eeuo pipefail
umask 077
[[ $# == 2 && $1 =~ ^[0-9a-f]{40}$ && $2 =~ ^[a-z0-9.-]+/[a-z0-9/_-]+@sha256:[0-9a-f]{64}$ ]] || exit 2
sha=$1 image=$2
bundle=$(cd -- "$(dirname -- "$0")/../.." && pwd)
root=$(cd -- "$bundle/../.." && pwd)
[[ $bundle == "$root/releases/$sha-"* ]] || exit 2
exec 9>"$root/deploy.lock"
flock -n 9 || { echo 'Another deployment or renewal is running' >&2; exit 1; }
[[ $(cat "$root/shared/host-ready") == backup-restore-host-approved-v1 ]] || exit 2
[[ $(cat "$bundle/SOURCE_SHA") == "$sha" ]] || exit 2
# Changed migration bytes require a new production-copy rehearsal and separate DB approval.
(cd "$bundle" && shasum -a 256 src/main/resources/db/migration/*.sql) > "$bundle/migrations.sha256"
cmp -s "$bundle/migrations.sha256" "$root/shared/approved-migrations.sha256" || { echo 'Schema gate closed' >&2; exit 1; }
[[ ! -e $bundle/release.env && ! -e $bundle/recovery ]] || exit 2
mkdir -m 700 "$bundle/recovery"
recovery=$bundle/recovery
# Never source env files as shell code, and never edit the shared secrets or legacy files.
python3 - "$root/shared/host.env" "$bundle/release.env" "$image" <<'PY'
import pathlib, stat, sys
source, target, image = sys.argv[1:]
assert stat.S_IMODE(pathlib.Path(source).stat().st_mode) == 0o600
lines = [s for s in pathlib.Path(source).read_text().splitlines() if not s.startswith('APP_IMAGE=')]
pathlib.Path(target).write_text('\n'.join(lines) + '\nAPP_IMAGE=' + image + '\n')
PY
python3 "$bundle/ops/production/validate-config.py" "$bundle/release.env" "$bundle/compose.json"
# An inherited APP_IMAGE must never override the digest verified by CD.
configured_image=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["services"]["app"]["image"])' "$bundle/compose.json")
[[ $configured_image == "$image" ]] || { echo 'Resolved image differs from the verified digest' >&2; exit 1; }
compose=(docker compose -f "$bundle/compose.json")
domain=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["services"]["nginx"]["environment"]["BE_DOMAIN"])' "$bundle/compose.json")
# Existing Redis is never replaced by an app release; volume/image changes need host review.
redis=$("${compose[@]}" ps -q redis)
[[ -n $redis && $(docker inspect -f '{{.State.Health.Status}}' "$redis") == healthy ]] || { echo 'Redis must already be healthy' >&2; exit 1; }
redis_image=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["services"]["redis"]["image"])' "$bundle/compose.json")
[[ $(docker inspect -f '{{.Config.Image}}' "$redis") == "$redis_image" ]] || exit 1
old=$("${compose[@]}" ps -aq app)
if [[ -L $root/current ]]; then
  cp "$root/current/compose.json" "$recovery/previous-compose.json"
  readlink "$root/current" > "$recovery/previous-release"
  [[ -n $old ]] || exit 1
else
  # First adoption is an inspected, stopped legacy stack with a verified image/config archive.
  [[ -s $root/shared/legacy-recovery/SHA256SUMS && -z $old ]] || exit 1
  (cd "$root/shared/legacy-recovery" && shasum -a 256 -c SHA256SUMS) > "$recovery/legacy-check.log"
fi
if [[ -n $old ]]; then
  docker inspect "$old" > "$recovery/previous-container.json"
  python3 - "$bundle/compose.json" "$recovery/previous-compose.json" "$recovery/previous-container.json" <<'PYCODE'
import json, sys
new, old, container = [json.load(open(p)) for p in sys.argv[1:]]
assert old['name'] == new['name']
assert old['volumes'] == new['volumes'], 'Volume change requires host adoption review'
assert old['services']['redis'] == new['services']['redis'], 'Redis change requires separate maintenance'
assert container[0]['Config']['Image'] == old['services']['app']['image'], 'Previous runtime drift'
PYCODE
  old_image=$(docker inspect -f '{{.Image}}' "$old")
  docker image save "$old_image" | gzip > "$recovery/previous-image.tar.gz"
  gzip -t "$recovery/previous-image.tar.gz"
  (cd "$recovery" && shasum -a 256 previous-image.tar.gz > SHA256SUMS)
fi
# Host uses its existing ECR read-only role; no long-lived AWS keys are copied here.
registry=${image%%/*}
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "$registry" > "$recovery/registry.log" 2>&1
docker pull "$image" >> "$recovery/registry.log" 2>&1
[[ $(docker image inspect -f '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$image") == "$sha" ]] || exit 1
"${compose[@]}" pull nginx certbot >> "$recovery/registry.log" 2>&1
"${compose[@]}" run --rm --no-deps nginx nginx -t > "$recovery/nginx-check.log" 2>&1
# From this point, any failure/signal leaves app stopped, including after host reboot.
failed() {
  trap - ERR INT TERM HUP
  app=$("${compose[@]}" ps -aq app)
  if [[ -n $app ]]; then
    docker update --restart=no "$app" >> "$recovery/rollout.log" 2>&1 || true
  fi
  "${compose[@]}" stop app >> "$recovery/rollout.log" 2>&1 || echo 'STOP FAILED: operator must stop app immediately' >&2
  echo 'Deployment failed; app stop requested. Inspect state/private logs; no automatic rollback.' >&2
  exit 1
}
trap failed ERR INT TERM HUP
"${compose[@]}" up -d --no-deps --pull never --wait --wait-timeout 180 app > "$recovery/rollout.log" 2>&1
"${compose[@]}" up -d --no-deps --pull never nginx >> "$recovery/rollout.log" 2>&1
# Verify the public TLS route, never host localhost:8080. No credentials or bodies in logs.
for path in /actuator/health /ws/info; do
  curl --fail --silent --show-error --retry 5 --retry-all-errors --retry-delay 2 --max-time 10 \
    "https://$domain$path" -o /dev/null 2>> "$recovery/rollout.log"
done
code=$(curl --silent --show-error --max-time 10 "https://$domain/admin/v1/users" -o /dev/null -w '%{http_code}')
[[ $code == 404 ]]
printf '%s\n%s\n' "$sha" "$image" > "$bundle/deployed.txt"
ln -s "$bundle" "$root/current.next"
mv -Tf "$root/current.next" "$root/current"
trap - ERR INT TERM HUP
echo "Deployed $sha at $image; application auth/CORS/STOMP checks remain in the release checklist."
