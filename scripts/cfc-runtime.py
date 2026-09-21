#!/usr/bin/env python3
"""Disposable local CFC runtime. Only HTTP commands and read-only SQL verification."""
import argparse
import hashlib
from http.client import HTTPException
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
PROJECT = 'lag-cfc-' + hashlib.sha256(str(ROOT).encode()).hexdigest()[:12]
STATE = Path(tempfile.gettempdir()) / PROJECT
COMPOSE = ROOT / 'docker-compose/docker-compose.cfc.yml'
LABEL = 'online.lifeasgame.cfc-owner'


def check(condition, message):
    if not condition:
        raise RuntimeError(message)


def run(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, text=True, **kwargs)


def resources():
    found = []
    for kind, listing in [('container', ['ps', '-aq']), ('volume', ['volume', 'ls', '-q']),
                          ('network', ['network', 'ls', '-q'])]:
        ids = run(['docker', *listing, '--filter', f'label=com.docker.compose.project={PROJECT}'],
                  capture_output=True).stdout.split()
        for resource_id in ids:
            data = json.loads(run(['docker', kind, 'inspect', resource_id], capture_output=True).stdout)[0]
            labels = data['Config']['Labels'] if kind == 'container' else data['Labels']
            found.append((kind, resource_id, labels or {}))
    return found


def load():
    check(STATE.is_dir() and not STATE.is_symlink(), 'No owned runtime; run start first.')
    state = json.loads((STATE / 'runtime.json').read_text())
    check(state['root'] == str(ROOT), 'Runtime belongs to a different checkout.')
    for kind, resource_id, labels in resources():
        check(labels.get(LABEL) == state['env']['CFC_OWNER'],
              f'Refusing foreign {kind}: {resource_id}')
    return state


def compose(state, *args, **kwargs):
    env = {k: v for k, v in os.environ.items() if not k.startswith(('COMPOSE_', 'CFC_'))}
    env.update(state['env'])
    return run(['docker', 'compose', '--env-file', os.devnull, '-p', PROJECT,
                '-f', str(COMPOSE), *args], env=env, **kwargs)


def sql(state, query):
    # SELECT only, even when running against our disposable container.
    check(query.lstrip().upper().startswith('SELECT ') and ';' not in query, 'Only one SELECT is allowed.')
    result = compose(state, 'exec', '-T', 'mysql', 'sh', '-c',
                     'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 '
                     '-uroot --batch --skip-column-names lifeasgame_cfc',
                     input=query + ';\n', capture_output=True)
    return [line.split('\t') for line in result.stdout.splitlines()]


def http(state, path, method='GET', body=None, token=None, expected=200, headers=None):
    request_headers = {'Content-Type': 'application/json', **(headers or {})}
    if token:
        request_headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request('http://127.0.0.1:' + state['env']['CFC_API_PORT'] + path,
                                 data=None if body is None else json.dumps(body).encode(),
                                 headers=request_headers, method=method)
    # Never inherit an HTTP proxy for a disposable localhost runtime.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        response = opener.open(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        check(response.status == expected, f'{method} {path}: HTTP {response.status}, expected {expected}')
        data = json.loads(raw) if raw and 'json' in response.headers.get('Content-Type', '') else {}
        if isinstance(data, dict) and 'isSuccess' in data and expected < 400:
            check(data['isSuccess'] is True, f'{method} {path}: failed envelope')
        return data, response.headers


def api(state, path, **kwargs):
    data, _ = http(state, path, **kwargs)
    return data.get('result', data)


def ready(state):
    app_id = compose(state, 'ps', '-q', 'app', capture_output=True).stdout.strip()
    check(bool(app_id), 'Owned app is not running; run start first.')
    container = json.loads(run(['docker', 'container', 'inspect', app_id], capture_output=True).stdout)[0]
    check(container['State']['Running'] and container['NetworkSettings']['Ports'].get('8080/tcp') ==
          [{'HostIp': '127.0.0.1', 'HostPort': state['env']['CFC_API_PORT']}], 'Owned HTTP binding mismatch')
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            data, _ = http(state, '/actuator/health')
            if data.get('status') == 'UP' and sql(state, 'SELECT COUNT(*) FROM quests') == [['5']]:
                break
        except (RuntimeError, urllib.error.URLError, TimeoutError, ConnectionError, HTTPException, subprocess.CalledProcessError):
            pass
        time.sleep(2)
    else:
        raise RuntimeError('Readiness timeout; inspect with logs, then cleanup.')
    check(sql(state, "SELECT version, success FROM flyway_schema_history WHERE version = '35'") == [['35', '1']],
          'Flyway V35 was not applied successfully.')
    logs = compose(state, 'logs', '--no-color', 'app', capture_output=True).stdout
    check('Initialized JPA EntityManagerFactory' in logs, 'Hibernate initialization not observed.')
    # Compose explicitly fixes ddl-auto=validate; successful initialization proves validation ran.
    print(f'PASS readiness: MySQL/Redis health, Flyway V35, Hibernate validate; API http://127.0.0.1:{state["env"]["CFC_API_PORT"]}')


def content(state):
    # Expected runtime contract: SeedLevel1Quest bootstrap and V20/V34 migrations.
    expected = [
        ['Q_GROWTH_ONE_FOCUS', '1', '한 가지에 25분 집중하기', '25', 'MINUTES', 'DAILY', 'USER_CONFIRM', 'MANUAL_CHECK', 'RP_NONE'],
        ['Q_RECORD_FIRST_TRACE', '1', '첫 흔적 남기기', '1', 'COUNT', 'ONCE', 'AUTO', 'RECORD_CREATED', 'RP_EXP_TINY_10'],
        ['Q_RECORD_THREE_TRACES', '1', '흔적 세 개 이어보기', '3', 'COUNT', 'ONCE', 'AUTO', 'RECORD_CREATED', 'RP_EXP_AND_ITEM_FIRST_STEP_20'],
        ['Q_RECORD_WEEKLY_LOOKBACK', '1', '이번 주 흔적 돌아보기', '1', 'COUNT', 'WEEKLY', 'AUTO', 'RECORD_CREATED', 'RP_NONE'],
        ['Q_RECOVERY_REST_TEN', '1', '10분 쉬어가기', '10', 'MINUTES', 'DAILY', 'USER_CONFIRM', 'MANUAL_CHECK', 'RP_NONE'],
    ]
    check(sql(state, 'SELECT code, definition_version, title_id, target_value, target_type, repeat_rule, '
              'completion_policy, progress_source, reward_profile_code FROM quests ORDER BY code') == expected,
          'Quest stableCode/version/payload mismatch')
    check(sql(state, 'SELECT code, definition_version, title FROM quest_routes ORDER BY code') ==
          [['ROUTE_RECORD_START', '1', '기록을 시작하는 길']], 'Route contract mismatch')
    check(sql(state, 'SELECT r.code, r.definition_version, s.step_code, s.step_order, q.code, l.requirement_type, '
              's.criterion_type, s.required_evidence_count, s.user_advance_required+0, '
              's.retroactive_evidence_allowed+0, s.skip_allowed+0 FROM quest_route_steps s '
              'JOIN quest_routes r ON r.id=s.route_id LEFT JOIN quest_route_step_quests l ON l.step_id=s.id '
              'LEFT JOIN quests q ON q.id=l.quest_id ORDER BY s.step_order') == [
                  ['ROUTE_RECORD_START', '1', code, str(i), quest, 'REQUIRED', 'QUEST_COMPLETION_SET', '1', '1', '1', '0']
                  for i, code, quest in [
                      (1, 'RS_RECORD_01_LEAVE_TRACE', 'Q_RECORD_FIRST_TRACE'),
                      (2, 'RS_RECORD_02_CONNECT_TRACES', 'Q_RECORD_THREE_TRACES'),
                      (3, 'RS_RECORD_03_LOOK_BACK', 'Q_RECORD_WEEKLY_LOOKBACK')]], 'Step links/version mismatch')
    check(sql(state, "SELECT p.code, p.status, COALESCE(l.sort_order,0), COALESCE(d.code,'NONE'), "
              "COALESCE(d.reward_type,'NONE'), COALESCE(l.amount_override,d.amount,0), "
              "COALESCE(i.code,'NONE'), COALESCE(d.active+0,1) FROM reward_profiles p "
              'LEFT JOIN reward_profile_lines l ON l.reward_profile_id=p.id '
              'LEFT JOIN reward_definitions d ON d.id=l.reward_definition_id LEFT JOIN items i ON i.id=d.item_id '
              "WHERE p.code IN ('RP_NONE','RP_EXP_TINY_10','RP_EXP_AND_ITEM_FIRST_STEP_20') "
              'ORDER BY p.code,l.sort_order') == [
                  ['RP_EXP_AND_ITEM_FIRST_STEP_20', 'ACTIVE', '1', 'EXP_PLAYER', 'EXP', '20', 'NONE', '1'],
                  ['RP_EXP_AND_ITEM_FIRST_STEP_20', 'ACTIVE', '2', 'ITEM_DEFINITION', 'ITEM', '1', 'IT_FIRST_STEP_FRAGMENT', '1'],
                  ['RP_EXP_TINY_10', 'ACTIVE', '1', 'EXP_PLAYER', 'EXP', '10', 'NONE', '1'],
                  ['RP_NONE', 'ACTIVE', '0', 'NONE', 'NONE', '0', 'NONE', '1']], 'Reward links/payload mismatch')
    check(sql(state, "SELECT code,name,category,type,rarity,stackable+0,max_stack, "
              "COALESCE(equipment_compatibility_kind,'NONE') FROM items WHERE code='IT_FIRST_STEP_FRAGMENT'") ==
          [['IT_FIRST_STEP_FRAGMENT', '첫걸음의 조각', 'QUEST', 'ETC', 'COMMON', '1', '99', 'NONE']], 'Item contract mismatch')
    print('PASS content: exact 5 Quest @1, Route @1 / 3 Step links, Reward/Item stable codes and payloads')
    print('LIMIT: Step is checked under its Route version; Reward/Item have no persisted content-version field.')


def actor(state):
    suffix = secrets.token_hex(6)
    credentials = {'email': f'cfc-{suffix}@example.invalid', 'password': secrets.token_urlsafe(24)}
    registration = api(state, '/api/v1/auth/register', method='POST',
                       body={**credentials, 'nickname': 'cfc' + suffix})
    check(registration['requiresVerification'] is False, 'Normal signup requires verification; no bypass allowed.')
    login = api(state, '/api/v1/auth/login', method='POST', body=credentials)
    player = api(state, '/api/v1/players/register', method='POST', token=login['accessToken'],
                 body={'name': 'CFC smoke', 'gender': 'MALE'}, expected=201)
    # Authenticate again through the same normal login route the FE uses.
    login = api(state, '/api/v1/auth/login', method='POST', body=credentials)
    check(login['playerId'] == player['id'], 'Login player identity mismatch')
    return player['id'], login['accessToken']


def smoke(state):
    ready(state)
    content(state)
    for path in ['/api/v1/players', '/api/v1/economy/wallet', '/api/v1/notifications',
                 '/api/v1/notifications/unread-count']:
        http(state, path, expected=401)
    origin = state['env']['CFC_ORIGINS'].split(',')[0]
    _, headers = http(state, '/api/v1/economy/wallet', method='OPTIONS', headers={
        'Origin': origin, 'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'authorization,content-type,idempotency-key'})
    check(headers.get('Access-Control-Allow-Origin') == origin, 'CORS origin mismatch')
    player_id, token = actor(state)
    me = api(state, '/api/v1/players', token=token)
    check(me['playerId'] == player_id, 'Current-player identity mismatch')
    before = sql(state, 'SELECT (SELECT COUNT(*) FROM wallets), (SELECT COUNT(*) FROM wallet_balances), '
                        '(SELECT COUNT(*) FROM outbox_events)')
    check(sql(state, f'SELECT COUNT(*) FROM wallets WHERE owner_id={int(player_id)}') == [['0']], 'New actor already has a wallet')
    for _ in range(3):
        wallet = api(state, '/api/v1/economy/wallet', token=token)
        check(wallet == {'amount': 0, 'currency': 'GOLD', 'balances': [
            {'currency': c, 'available': 0, 'held': 0} for c in ['GOLD', 'GEM']]}, 'Wallet response mismatch')
    check(before == sql(state, 'SELECT (SELECT COUNT(*) FROM wallets), (SELECT COUNT(*) FROM wallet_balances), '
                              '(SELECT COUNT(*) FROM outbox_events)'), 'Wallet GET created state/outbox')
    check(api(state, '/api/v1/notifications', token=token)['notifications'] == [], 'New inbox not empty')
    check(api(state, '/api/v1/notifications/unread-count', token=token)['unreadCount'] == 0, 'New unread not zero')
    print('PASS HTTP: unauthenticated 401, CORS, normal signup/login/onboarding, current-player, zero GOLD/GEM, empty inbox/unread')
    print('PASS wallet: repeated GET leaves wallet/balance/outbox counts unchanged; actor wallet absent')

    quests = ['Q_RECORD_FIRST_TRACE', 'Q_RECORD_THREE_TRACES']
    for quest in quests:
        api(state, '/api/v1/players/quests/' + quest, method='POST', body={}, token=token, expected=201)
    for i in range(3):
        api(state, '/api/v1/lifelogs/quick-record', method='POST', token=token, expected=201,
            headers={'Idempotency-Key': secrets.token_hex(16)}, body={
                'type': 'COLLECTION', 'lifeLogSubtype': 'QUICK_NOTE',
                'collection': {'category': 'OTHER', 'title': f'CFC synthetic trace {i+1}', 'quantity': 1}})
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        inbox = api(state, '/api/v1/notifications', token=token)['notifications']
        completed = [api(state, '/api/v1/players/quests/' + q, token=token)['acceptance']['status'] for q in quests]
        settlements = sql(state, f'SELECT status FROM reward_settlements WHERE player_id={int(player_id)} ORDER BY id')
        if len(inbox) == 4 and completed == ['COMPLETED', 'COMPLETED'] and settlements == [['COMPLETED'], ['COMPLETED']]:
            break
        time.sleep(1)
    else:
        raise RuntimeError('Quest -> reward -> notification did not converge within 60s; no manual relay/SQL repair performed.')
    expected_copy = set()
    for title in ['첫 흔적 남기기', '흔적 세 개 이어보기']:
        expected_copy.add(('QUEST_COMPLETED', 'Quest를 완료했어요', title + ' 완료 사실이 기록되었습니다.'))
        expected_copy.add(('QUEST_REWARD_READY', 'Quest 보상이 준비됐어요',
                          title + '의 확인 가능한 보상이 준비되었습니다. Mailbox 또는 결과 화면에서 상태를 확인해 주세요.'))
    check({(n['type'], n['title'], n['body']) for n in inbox} == expected_copy, 'Approved notification text mismatch')
    for n in inbox:
        prefix = 'notification.ntf_' + n['type'].lower()
        check((n['titleCopyId'], n['titleCopyVersion'], n['bodyCopyId'], n['bodyCopyVersion'], n['copyLocale']) ==
              (prefix + '.title', 1, prefix + '.body', 1, 'ko-KR'), 'Notification copy metadata mismatch')
    check(api(state, '/api/v1/players', token=token)['exp'] == me['exp'] + 30, 'EXP reward mismatch')
    check(sql(state, f'SELECT item_code,quantity FROM inventory_reward_deliveries WHERE player_id={int(player_id)}') ==
          [['IT_FIRST_STEP_FRAGMENT', '1']], 'ITEM reward receipt mismatch')
    mailbox = api(state, '/api/v1/mailbox', token=token)['entries']
    check(len(mailbox) == 1 and mailbox[0]['itemName'] == '첫걸음의 조각'
          and mailbox[0]['quantity'] == 1 and mailbox[0]['bound'] is True, 'Bound Mailbox reward mismatch')
    check(api(state, '/api/v1/notifications/unread-count', token=token)['unreadCount'] == 4, 'Unread count mismatch')
    check(api(state, '/api/v1/economy/wallet', token=token) == wallet
          and sql(state, f'SELECT COUNT(*) FROM wallets WHERE owner_id={int(player_id)}') == [['0']],
          'Approved EXP/Item reward unexpectedly funded or created a Wallet')
    _, other_token = actor(state)
    check(api(state, '/api/v1/notifications', token=other_token)['notifications'] == [], 'Inbox owner isolation failed')
    print('PASS HTTP + DB: 3 Quick Records -> 2 completed Quests -> EXP 30 + bound Mailbox Item/receipt -> 4 approved notifications with @1 ko-KR metadata; owner isolation')
    print('BLOCKED Marketplace: approved Item reward is bound; verified payment/funding is unavailable. No admin grant or SQL fixture used.')
    print('Scope: representative runtime smoke only; Marketplace end-to-end verification remains blocked.')


def start(args):
    if not STATE.exists():
        check(not resources(), 'Project resources already exist without an ownership record; refusing to adopt them.')
        STATE.mkdir(mode=0o700)
        state = {'root': str(ROOT), 'env': {
            'CFC_OWNER': secrets.token_hex(16), 'CFC_DB_PASSWORD': secrets.token_urlsafe(32),
            'CFC_JWT_SECRET': secrets.token_urlsafe(48), 'CFC_RUNTIME_DIR': str(STATE),
            'CFC_API_PORT': str(args.port), 'CFC_ORIGINS': args.origins}}
        (STATE / 'runtime.json').write_text(json.dumps(state))
        (STATE / 'runtime.json').chmod(0o600)
    state = load()
    if not (STATE / 'app.jar').exists():
        run(['./gradlew', 'bootJar'])
        jars = [p for p in (ROOT / 'build/libs').glob('*.jar') if not p.name.endswith('-plain.jar')]
        check(len(jars) == 1, 'Expected exactly one bootJar.')
        state['sha'] = run(['git', 'rev-parse', 'HEAD'], capture_output=True).stdout.strip()
        state['dirty'] = bool(run(['git', 'status', '--porcelain'], capture_output=True).stdout)
        state['jar_sha256'] = hashlib.sha256(jars[0].read_bytes()).hexdigest()
        (STATE / 'runtime.json').write_text(json.dumps(state))
        shutil.copyfile(jars[0], STATE / 'app.jar')
    compose(state, 'up', '-d')
    print(f'Runtime project={PROJECT}, source HEAD={state["sha"]}; dirty={state["dirty"]}, jar SHA256={state["jar_sha256"]}')
    ready(state)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['start', 'ready', 'smoke', 'stop', 'cleanup', 'logs'])
    parser.add_argument('--port', type=int, default=18080, help='start only; loopback HTTP port (default 18080)')
    parser.add_argument('--origins', default='http://localhost:3000', help='start only; exact comma-separated FE origins')
    args = parser.parse_args()
    check(1024 <= args.port <= 65535, 'Port must be between 1024 and 65535.')
    if args.command == 'start':
        start(args)
    elif args.command == 'cleanup' and not STATE.exists():
        check(not resources(), 'Unowned resources exist; refusing cleanup.')
        print('PASS cleanup: no runtime resources')
    else:
        state = load()
        if args.command == 'ready':
            ready(state)
        elif args.command == 'smoke':
            smoke(state)
        elif args.command == 'logs':
            compose(state, 'logs', '--tail', '100', 'app')
        elif args.command == 'stop':
            compose(state, 'stop')
        elif args.command == 'cleanup':
            compose(state, 'down', '--volumes')
            check(not resources(), 'Cleanup left project resources behind.')
            shutil.rmtree(STATE)
            print('PASS cleanup: owned containers/network/volumes and local runtime secrets/jar removed')


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError, urllib.error.URLError, TimeoutError, ConnectionError, HTTPException) as error:
        # Do not dump HTTP bodies, credentials or bearer tokens on failure.
        print(f'FAIL: {error}', file=sys.stderr)
        sys.exit(1)
