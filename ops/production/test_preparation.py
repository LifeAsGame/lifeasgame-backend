import copy
import importlib.util
import pathlib
import subprocess
import tempfile
import unittest
import sys

BASE = pathlib.Path(__file__).parent

def module(name):
    spec = importlib.util.spec_from_file_location(name, BASE / f'{name}.py')
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


class PreparationTest(unittest.TestCase):
    def test_rehearsal_credentials_are_private_and_cannot_overwrite(self):
        with tempfile.TemporaryDirectory() as d:
            target = pathlib.Path(d) / 'new'
            command = ['python3', str(BASE / 'init-rehearsal.py'), str(target)]
            subprocess.run(command, check=True, capture_output=True)
            self.assertTrue(all(p.stat().st_mode & 0o777 == 0o600 for p in target.iterdir()))
            self.assertNotEqual(subprocess.run(command, capture_output=True).returncode, 0)
            self.assertIn('jdbc:mysql://mysql:3306/', (target / 'app.env').read_text())

    def test_compose_rejects_mutable_images_public_database_and_runtime_overrides(self):
        # Validate the real rendered compose without starting any service.
        import json
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            app = root / 'app.env'
            app.write_text('DB_URL=jdbc:mysql://db.invalid/lifeasgame\nDB_PASSWORD=synthetic\n')
            env = root / 'release.env'
            values = (BASE / 'images.env.example').read_text().replace('REPLACE_WITH_VERIFIED_ECR_REPOSITORY@sha256:REPLACE', 'example/app@sha256:' + 'a' * 64).replace('/etc/lifeasgame/app.env', str(app)).replace('api.example.invalid', 'api.example.test').replace('REPLACE_WITH_INSPECTED_PROJECT', 'lag-config-test')
            env.write_text(values); env.chmod(0o600)
            raw = subprocess.check_output(['docker', 'compose', '--profile', 'certificate', '--env-file', str(env), '-f', str(BASE / 'compose.yml'), 'config', '--format', 'json'])
            config = json.loads(raw)
            check = module('validate-config').check
            declared = set(config['services']['app']['environment']) - {'DB_URL', 'DB_PASSWORD'}
            check(config, declared)
            for field, value in [('image', 'example/app:latest'), ('ports', [{'published': '8080', 'target': 8080}])]:
                changed = copy.deepcopy(config); changed['services']['app'][field] = value
                with self.subTest(field=field), self.assertRaises(AssertionError): check(changed, declared)
            for key, value in [('SPRING_FLYWAY_ENABLED', 'true'), ('SPRING_APPLICATION_JSON', '{}'), ('JAVA_TOOL_OPTIONS', '-Dspring.flyway.enabled=true'), ('LIFEASGAME_WEB_ALLOWED_ORIGINS', 'https://*.vercel.app')]:
                changed = copy.deepcopy(config); changed['services']['app']['environment'][key] = value
                with self.subTest(key=key), self.assertRaises(AssertionError): check(changed, declared)

            for domain in ('https://api.example.com', 'api.example.com;\nreturn 200;', '*.example.com', '../example.com', 'api.example.invalid'):
                changed = copy.deepcopy(config)
                changed['services']['nginx']['environment']['BE_DOMAIN'] = domain
                with self.subTest(domain=domain), self.assertRaises(AssertionError): check(changed, declared)

    def test_failed_dump_never_gets_a_completed_backup_name(self):
        import os
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            cnf = root / 'read.cnf'
            cnf.write_text('[client]\nhost=example.rds.amazonaws.com\nssl-mode=VERIFY_IDENTITY\nssl-ca=/secure/ca.pem\n')
            cnf.chmod(0o600)
            mysql = root / 'mysql'
            mysql.write_text('#!/bin/sh\ncase "$*" in *"COUNT(*)"*) echo 0 ;; *) cat >/dev/null ;; esac\n')
            dump = root / 'mysqldump'
            dump.write_text('#!/bin/sh\nif [ "$1" = --version ]; then echo "mysqldump Ver 8.0.46"; exit; fi\necho partial-synthetic-dump\nexit 1\n')
            mysql.chmod(0o700); dump.chmod(0o700)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'], WRITERS_AND_DDL_FROZEN='yes')
            output = root / 'backup'
            result = subprocess.run(['bash', str(BASE / 'backup.sh'), str(cnf), str(output)], env=env, capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse((output / 'production.sql.gz').exists())
            self.assertFalse((output / 'SHA256SUMS').exists())
            self.assertIn('MISSING', (output / 'history.tsv').read_text())
            dump.write_text('#!/bin/sh\nif [ "$1" = --version ]; then echo "mysqldump Ver 8.0.46"; exit; fi\necho synthetic-dump\n')
            good = root / 'complete'
            passed = subprocess.run(['bash', str(BASE / 'backup.sh'), str(cnf), str(good)], env=env, capture_output=True)
            self.assertEqual(passed.returncode, 0)
            self.assertTrue((good / 'production.sql.gz').exists())
            self.assertTrue((good / 'SHA256SUMS').exists())
            self.assertEqual((output / 'production.sql.gz.partial').stat().st_mode & 0o777, 0o600)

    def test_malformed_private_defaults_do_not_print_their_contents(self):
        import os
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            cnf = root / 'read.cnf'
            cnf.write_text('[client]\nmalformed-sensitive-test-value\n')
            cnf.chmod(0o600)
            result = subprocess.run(['bash', str(BASE / 'backup.sh'), str(cnf), str(root / 'backup')], env=dict(os.environ, WRITERS_AND_DDL_FROZEN='yes'), capture_output=True)
            self.assertEqual(result.returncode, 2)
            self.assertNotIn(b'malformed-sensitive-test-value', result.stderr + result.stdout)

    def test_scripts_fail_closed_without_authorization(self):
        for name in ('backup.sh', 'rollout.sh'):
            result = subprocess.run(['bash', str(BASE / name)], capture_output=True)
            self.assertNotEqual(result.returncode, 0)

    def test_certificate_failure_never_reloads_nginx(self):
        import os
        import shlex
        service = (BASE / 'lifeasgame-cert-renew.service').read_text().replace('\\\n', '')
        command = shlex.split(next(line for line in service.splitlines() if line.startswith('ExecStart=')).split('=', 1)[1])
        shell = command[command.index('-ec') + 1]
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            docker = root / 'docker'
            docker.write_text('#!/bin/sh\necho "$*" >> "$CALLS"\ncase "$*" in *renew*) exit "$RENEW_EXIT" ;; esac\n')
            docker.chmod(0o700)
            calls = root / 'calls'
            for code in ('1', '0'):
                calls.write_text('')
                result = subprocess.run(['bash', '-ec', shell], env=dict(os.environ, PATH=str(root)+os.pathsep+os.environ['PATH'], CALLS=str(calls), RENEW_EXIT=code), capture_output=True)
                self.assertEqual(result.returncode, int(code))
                self.assertEqual('nginx -s reload' in calls.read_text(), code == '0')

    def test_rollout_retains_recovery_and_stops_on_health_or_public_failure(self):
        import json
        import os
        import shutil
        real_docker = shutil.which('docker')
        sha = 'c' * 40
        image = 'example/app@sha256:' + 'a' * 64
        for failure in ('schema', 'image', 'health', 'public', 'none'):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as d:
                root = pathlib.Path(d).resolve()
                bundle = root / 'releases' / (sha + '-test')
                shutil.copytree(BASE, bundle / 'ops/production')
                shared = root / 'shared'; shared.mkdir()
                migration = bundle / 'src/main/resources/db/migration'; migration.mkdir(parents=True)
                (migration / 'V1__test.sql').write_text('-- synthetic\n')
                (bundle / 'SOURCE_SHA').write_text(sha)
                manifest = subprocess.check_output(['shasum', '-a', '256', 'src/main/resources/db/migration/V1__test.sql'], cwd=bundle)
                (shared / 'approved-migrations.sha256').write_bytes(manifest if failure != 'schema' else b'wrong')
                (shared / 'host-ready').write_text('backup-restore-host-approved-v1')
                app = root / 'app.env'
                app.write_text('DB_URL=jdbc:mysql://db.example.rds.amazonaws.com/lifeasgame?sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/run/rds-truststore.p12&trustCertificateKeyStoreType=PKCS12\nDB_USERNAME=test\nDB_PASSWORD=private-$-test\nJWT_SECRET=private-test\n')
                app.chmod(0o600)
                values = (BASE / 'images.env.example').read_text().replace('REPLACE_WITH_VERIFIED_ECR_REPOSITORY@sha256:REPLACE', image).replace('/etc/lifeasgame/app.env', str(app)).replace('api.example.invalid', 'api.example.test').replace('REPLACE_WITH_INSPECTED_PROJECT', 'lag-rollout-test')
                (shared / 'host.env').write_text(values)
                (shared / 'host.env').chmod(0o600)
                config = json.loads(subprocess.check_output([real_docker, 'compose', '--profile', 'certificate', '--env-file', str(shared / 'host.env'), '-f', str(bundle / 'ops/production/compose.yml'), 'config', '--format', 'json']))
                old = root / 'previous'; old.mkdir()
                (old / 'compose.json').write_text(json.dumps(config))
                (root / 'current').symlink_to(old)
                binpath = root / 'bin'; binpath.mkdir()
                fake = binpath / 'docker'
                fake.write_text('''#!/usr/bin/env python3
import json, os, pathlib, subprocess, sys
root = pathlib.Path(os.environ['TEST_ROOT']); a = sys.argv[1:]
with (root/'calls').open('a') as f: f.write(' '.join(a)+'\\n')
config = json.loads((root/'previous/compose.json').read_text())
if a[0] == 'compose':
 if 'config' in a: sys.exit(subprocess.call([os.environ['REAL_DOCKER']] + a))
 if 'ps' in a: print('redis-id' if a[-1] == 'redis' else 'app-id')
 if 'up' in a and a[-1] == 'app' and os.environ['FAILURE'] == 'health': sys.exit(1)
 if 'stop' in a: (root/'stopped').touch()
elif a[0] == 'inspect':
 if '-f' not in a: print(json.dumps([{'Config':{'Image':config['services']['app']['image']}}]))
 elif 'Health.Status' in a[2]: print('healthy')
 elif 'Config.Image' in a[2]: print(config['services']['redis']['image'])
 else: print('sha256:'+'b'*64)
elif a[:2] == ['image', 'save']: print('synthetic saved image')
elif a[:2] == ['image', 'inspect']: print('c'*40)
elif a[0] == 'login': sys.stdin.read()
''')
                (binpath / 'aws').write_text('#!/bin/sh\necho synthetic-token\n')
                (binpath / 'curl').write_text('#!/bin/sh\n[ "$FAILURE" != public ] || exit 22\ncase "$*" in *admin*) printf 404 ;; esac\n')
                # GNU mv/flock behavior is exercised by CI on Linux; these shims only support macOS fixtures.
                if sys.platform == 'darwin':
                    (binpath / 'flock').write_text('#!/bin/sh\nexit 0\n')
                    (binpath / 'mv').write_text('#!/usr/bin/env python3\nimport os,sys\nos.replace(sys.argv[-2],sys.argv[-1])\n')
                for p in binpath.iterdir(): p.chmod(0o700)
                env = dict(os.environ, PATH=str(binpath)+os.pathsep+os.environ['PATH'], TEST_ROOT=str(root), REAL_DOCKER=real_docker, FAILURE=failure)
                if failure == 'image': env['APP_IMAGE'] = 'example/app@sha256:' + 'd' * 64
                result = subprocess.run(['bash', str(bundle / 'ops/production/rollout.sh'), sha, image], env=env, capture_output=True)
                self.assertNotIn(b'private-', result.stdout + result.stderr)
                if failure == 'none':
                    self.assertEqual(result.returncode, 0, result.stderr.decode())
                    self.assertEqual((root / 'current').resolve(), bundle)
                    self.assertFalse((root / 'stopped').exists())
                else:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertEqual((root / 'current').resolve(), old)
                    self.assertEqual((root / 'stopped').exists(), failure in ('health', 'public'))
                if failure not in ('schema', 'image'):
                    self.assertTrue((bundle / 'recovery/previous-image.tar.gz').exists())
                    self.assertTrue(all(p.stat().st_mode & 0o777 == 0o600 for p in (bundle / 'recovery').iterdir()))
                    calls = (root / 'calls').read_text()
                    self.assertNotIn('prune', calls)
                    self.assertNotIn('migrate', calls)
                    if failure != 'none': self.assertIn('update --restart=no app-id', calls)


if __name__ == '__main__':
    unittest.main()
