#!/usr/bin/env bash
# Run on the inspected host only after write/DDL freeze and explicit authorization.
set -euo pipefail
umask 077
[[ $# == 2 ]] || { echo 'usage: backup.sh /absolute/read.cnf /absolute/new-backup-dir' >&2; exit 2; }
cnf=$1
output=$2
[[ $cnf == /* && $output == /* && -f $cnf && ! -L $cnf && ! -e $output ]] || exit 2
[[ ${WRITERS_AND_DDL_FROZEN:-} == yes ]] || { echo 'Writer/DDL freeze evidence required' >&2; exit 2; }
if ! python3 - "$cnf" 2>/dev/null <<'PY'
import configparser, os, stat, sys
p=sys.argv[1]
assert stat.S_IMODE(os.stat(p).st_mode) == 0o600, 'defaults file must be 0600'
c=configparser.ConfigParser(interpolation=None); c.read(p)
assert c['client']['ssl-mode'] == 'VERIFY_IDENTITY'
assert c['client']['host'].endswith('.rds.amazonaws.com')
assert c['client'].get('ssl-ca'), 'RDS CA path required'
PY
then
  echo 'Invalid private defaults file: require 0600, RDS hostname and VERIFY_IDENTITY with CA' >&2
  exit 2
fi
mysqldump --version | grep -Eq 'Ver 8\.0\.' || { echo 'Use MySQL 8.0 dump client for current RDS; reassess if engine changes' >&2; exit 2; }
mkdir -m 700 "$output"
exec 2>"$output/errors.log"
base=$(cd -- "$(dirname -- "$0")" && pwd)
mysql --defaults-extra-file="$cnf" --batch --skip-column-names --database=lifeasgame < "$base/sql/inspect.sql" > "$output/metadata.tsv"
non_transactional=$(mysql --defaults-extra-file="$cnf" --batch --skip-column-names --database=lifeasgame -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND engine <> 'InnoDB'")
[[ $non_transactional == 0 ]] || { echo 'Non-InnoDB table: consistency review required'; exit 1; }
history_present=$(mysql --defaults-extra-file="$cnf" --batch --skip-column-names --database=lifeasgame -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='flyway_schema_history'")
if [[ $history_present == 1 ]]; then
  mysql --defaults-extra-file="$cnf" --batch --database=lifeasgame < "$base/sql/flyway-history.sql" > "$output/history.tsv"
else
  printf '%s\n' 'MISSING: migration/baseline prohibited pending schema review' > "$output/history.tsv"
fi
# Pipefail and .partial prevent failed dumps from being mistaken for usable backups.
mysqldump --defaults-extra-file="$cnf" --single-transaction --quick --skip-lock-tables \
  --no-tablespaces --set-gtid-purged=OFF --column-statistics=0 --hex-blob \
  --routines --events --triggers lifeasgame | python3 -c '
import sys
size = 0
while chunk := sys.stdin.buffer.read(1024 * 1024):
    size += len(chunk)
    if size > 1024 ** 3:
        sys.exit("Logical backup exceeds the approved 1 GiB limit")
    sys.stdout.buffer.write(chunk)
' | gzip > "$output/production.sql.gz.partial"
[[ ! -s $output/errors.log ]] || { echo 'Client warnings/errors require review; backup remains partial'; exit 1; }
gzip -t "$output/production.sql.gz.partial"
mv "$output/production.sql.gz.partial" "$output/production.sql.gz"
(cd "$output" && shasum -a 256 production.sql.gz > SHA256SUMS)
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$output/completed-at.txt"
echo 'Backup complete; verify transferred checksum and perform isolated restore.'
