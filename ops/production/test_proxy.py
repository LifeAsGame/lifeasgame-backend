"""Local Docker proxy smoke; synthetic backend and self-signed test CA only."""
import json
import pathlib
import subprocess
import socket
import ssl
import tempfile
import time
import unittest
import uuid

BASE = pathlib.Path(__file__).parent
NGINX = 'nginx@sha256:0985e772fb9f729e6fa0980da05fca5d9c468e870eed43071545afa9d2e27d94'
PYTHON = 'certbot/certbot@sha256:f70ad0adbb7e117f0fe42a63c553f28ea451edabc0148757b6efcd9735acaa20'
MOCK = '''from http.server import BaseHTTPRequestHandler, HTTPServer
import json
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  upgrade = self.headers.get('Upgrade') == 'websocket'
  self.send_response(101 if upgrade else 200)
  if upgrade:
   self.send_header('Upgrade', 'websocket'); self.send_header('Connection', 'Upgrade')
  self.end_headers()
  if not upgrade: self.wfile.write(json.dumps(dict(self.headers)).encode())
 def log_message(self, *args): pass
HTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
'''


def run(*args):
    return subprocess.check_output(args, stderr=subprocess.PIPE, text=True).strip()


class ProxyTest(unittest.TestCase):
    def test_tls_routes_headers_upgrade_and_app_replacement(self):
        name = 'lag-proxy-test-' + uuid.uuid4().hex[:10]
        names = [name + '-nginx', name + '-app']
        with tempfile.TemporaryDirectory() as d:
            root = pathlib.Path(d)
            cert = root / 'cert' / 'live' / 'api.example.test'
            cert.mkdir(parents=True)
            run('openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1', '-subj', '/CN=api.example.test', '-addext', 'subjectAltName=DNS:api.example.test', '-keyout', str(cert / 'privkey.pem'), '-out', str(cert / 'fullchain.pem'))
            (root / 'mock.py').write_text(MOCK)
            run('docker', 'network', 'create', name)
            try:
                def start_app():
                    run('docker', 'run', '-d', '--name', names[1], '--network', name, '--network-alias', 'app', '-v', f'{root}/mock.py:/mock.py:ro', '--entrypoint', 'python', PYTHON, '/mock.py')
                start_app()
                run('docker', 'run', '-d', '--name', names[0], '--network', name, '-p', '127.0.0.1::443', '-e', 'BE_DOMAIN=api.example.test', '-e', 'NGINX_ENVSUBST_FILTER=^BE_DOMAIN$', '-v', f'{BASE}/nginx/https.conf.template:/etc/nginx/templates/default.conf.template:ro', '-v', f'{BASE}/nginx/proxy.conf:/etc/nginx/conf.d/proxy.inc:ro', '-v', f'{root}/cert:/etc/letsencrypt:ro', NGINX)
                run('docker', 'exec', names[0], 'nginx', '-t')
                port = run('docker', 'port', names[0], '443/tcp').rsplit(':', 1)[1]
                def request(path, *extra):
                    return run('curl', '--noproxy', '*', '--silent', '--show-error', '--max-time', '5', '--cacert', str(cert / 'fullchain.pem'), '--resolve', f'api.example.test:{port}:127.0.0.1', f'https://api.example.test:{port}{path}', *extra)
                for attempt in range(20):
                    try:
                        data = json.loads(request('/api/v1/users/me?token=secret-test-query', '-H', 'X-Forwarded-For: forged', '-H', 'Forwarded: forged', '-H', 'X-Forwarded-Proto: http', '-H', 'Origin: https://lifeasgame-app.vercel.app'))
                        break
                    except (subprocess.CalledProcessError, json.JSONDecodeError):
                        if attempt == 19: raise
                        time.sleep(.3)
                self.assertNotEqual(data['X-Forwarded-For'], 'forged')
                self.assertNotIn('Forwarded', data)
                self.assertEqual(data['X-Forwarded-Proto'], 'https')
                self.assertEqual(data['Origin'], 'https://lifeasgame-app.vercel.app')
                for path in ('/admin/v1/users', '/actuator/loggers', '/v3/api-docs', '/debug'):
                    self.assertEqual(request(path, '-o', '/dev/null', '-w', '%{http_code}'), '404')
                for path in ('/ws/info', '/ws/000/session/xhr', '/actuator/health'):
                    self.assertEqual(request(path, '-o', '/dev/null', '-w', '%{http_code}'), '200')
                logs = run('docker', 'logs', names[0])
                self.assertNotIn('secret-test-query', logs)
                self.assertIn('GET 200', logs)
                config = run('docker', 'exec', names[0], 'cat', '/etc/nginx/conf.d/default.conf')
                self.assertIn('$request_uri', config)
                self.assertNotIn('${BE_DOMAIN}', config)
                context = ssl.create_default_context(cafile=str(cert / 'fullchain.pem'))
                with socket.create_connection(('127.0.0.1', int(port)), timeout=5) as sock:
                    with context.wrap_socket(sock, server_hostname='api.example.test') as tls:
                        tls.sendall(b'GET /ws/000/session/websocket HTTP/1.1\r\nHost: api.example.test\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n')
                        self.assertIn(b' 101 ', tls.recv(4096).split(b'\r\n')[0])
                # Replacing app must not require restarting the proxy.
                run('docker', 'rm', '-f', names[1]); start_app()
                for attempt in range(30):
                    if request('/api/v1/users/me', '-o', '/dev/null', '-w', '%{http_code}') == '200': break
                    time.sleep(.5)
                else: self.fail('Nginx did not resolve replacement app')
            finally:
                for container in names:
                    subprocess.run(['docker', 'rm', '-f', container], capture_output=True)
                subprocess.run(['docker', 'network', 'rm', name], capture_output=True)

    def test_real_compose_health_failure_stops_without_restarting(self):
        name = 'lag-health-test-' + uuid.uuid4().hex[:10]
        with tempfile.TemporaryDirectory() as d:
            config = pathlib.Path(d) / 'compose.json'
            config.write_text(json.dumps({'name': name, 'services': {'app': {
                'image': PYTHON, 'restart': 'unless-stopped',
                'entrypoint': ['python', '-c', 'import time; time.sleep(300)'],
                'healthcheck': {'test': ['CMD', 'python', '-c', 'raise SystemExit(1)'], 'interval': '1s', 'timeout': '1s', 'retries': 1},
            }}, 'networks': {'default': {'internal': True}}}))
            command = ['docker', 'compose', '-f', str(config)]
            try:
                result = subprocess.run(command + ['up', '-d', '--wait', '--wait-timeout', '5', 'app'], capture_output=True)
                self.assertNotEqual(result.returncode, 0)
                container = run(*command, 'ps', '-aq', 'app')
                run('docker', 'update', '--restart=no', container)
                run(*command, 'stop', 'app')
                state = json.loads(run('docker', 'inspect', container))[0]
                self.assertEqual(state['State']['Status'], 'exited')
                self.assertEqual(state['HostConfig']['RestartPolicy']['Name'], 'no')
            finally:
                subprocess.run(command + ['down'], capture_output=True)


if __name__ == '__main__':
    unittest.main()
