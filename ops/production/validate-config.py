#!/usr/bin/env python3
"""Reject dangerous runtime inputs; resolved private config goes only to a file."""
import json
import pathlib
import re
import stat
import subprocess
import sys
from urllib.parse import urlsplit, parse_qs


def check(config, declared_keys):
    services = config['services']
    for name in ('app', 'redis', 'nginx', 'certbot'):
        assert re.fullmatch(r'[^\s@]+@sha256:[0-9a-f]{64}', services[name]['image']), 'Immutable images required'
    for name in ('app', 'redis'):
        assert not services[name].get('ports'), 'Internal ports must stay private'
    app = services['app']['environment']
    assert set(app) <= set(declared_keys) | {'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET'}, 'Unreviewed private environment key'
    assert app['SPRING_PROFILES_ACTIVE'] == 'prod'
    assert app['SPRING_JPA_HIBERNATE_DDL_AUTO'] == 'validate'
    assert app['SPRING_FLYWAY_ENABLED'] == app['FLYWAY_ENABLED'] == 'false'
    assert app['LIFEASGAME_PORTFOLIO_DEMO_ENABLED'] == 'false'
    for origin in app['LIFEASGAME_WEB_ALLOWED_ORIGINS'].split(','):
        u = urlsplit(origin)
        assert u.scheme == 'https' and u.hostname and '*' not in origin
        assert not (u.username or u.password or u.path or u.query or u.fragment)
    domain = services['nginx']['environment']['BE_DOMAIN']
    assert len(domain) <= 253 and re.fullmatch(r'(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}', domain), 'Invalid DNS hostname'
    assert not domain.endswith('.invalid'), 'Replace example domain'
    assert services['nginx']['environment']['NGINX_ENVSUBST_FILTER'] == '^BE_DOMAIN$$'  # Compose escapes dollars for re-reading its config output


def main():
    env = pathlib.Path(sys.argv[1]).resolve()
    assert stat.S_IMODE(env.stat().st_mode) == 0o600
    command = ['docker', 'compose', '--profile', 'certificate', '--env-file', str(env), '-f', str(pathlib.Path(__file__).with_name('compose.yml')), 'config', '--format', 'json']
    def resolve(*extra):
        result = subprocess.run(command + list(extra), capture_output=True, check=True)
        return json.loads(result.stdout)
    declared = resolve('--no-env-resolution')
    # Compose 2.x omits env_file from JSON when --no-env-resolution is used.
    inputs = subprocess.run(command[:-2] + ['--environment'], capture_output=True, check=True)
    values = dict(line.split('=', 1) for line in inputs.stdout.decode().splitlines() if '=' in line)
    p = pathlib.Path(values['APP_ENV_FILE'])
    assert p.is_absolute() and p.is_file() and not p.is_symlink() and stat.S_IMODE(p.stat().st_mode) == 0o600
    config = resolve()
    check(config, declared['services']['app']['environment'])
    app = config['services']['app']['environment']
    assert all(app.get(k) for k in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET')), 'Missing private input'
    assert app['DB_URL'].startswith('jdbc:mysql://')
    db = urlsplit(app['DB_URL'][5:])
    options = parse_qs(db.query)
    assert db.hostname and db.hostname.endswith('.rds.amazonaws.com')
    assert options.get('sslMode') == ['VERIFY_IDENTITY'], 'RDS hostname verification required'
    assert options.get('trustCertificateKeyStoreUrl') == ['file:/run/rds-truststore.p12']
    assert options.get('trustCertificateKeyStoreType') == ['PKCS12']
    assert options.get('trustServerCertificate', ['false']) == ['false']
    if len(sys.argv) == 3:
        with open(sys.argv[2], 'x') as out:
            pathlib.Path(out.name).chmod(0o600)
            json.dump(config, out)
    print('Configuration validated (no startup).')


if __name__ == '__main__':
    try:
        main()
    except (AssertionError, KeyError, ValueError, OSError, subprocess.CalledProcessError):
        sys.exit('Invalid production inputs; inspect privately. Values withheld.')
