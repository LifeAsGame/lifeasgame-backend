#!/usr/bin/env python3
"""BE-DEMO-01: isolated runtime, resumable preparation, separate HTTP verification."""
import argparse
from contextlib import contextmanager
import fcntl
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import shutil
import socket
import subprocess
import sys
import tempfile
import time
from urllib.parse import urlsplit

# Reuse the existing ownership checks, Compose runner, normal HTTP and SELECT-only SQL.
_spec = importlib.util.spec_from_file_location('cfc', Path(__file__).with_name('cfc-runtime.py'))
r = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(r)
r.PROJECT = 'lag-demo-' + hashlib.sha256(str(r.ROOT).encode()).hexdigest()[:12]
r.STATE = Path.home() / '.local/share/lifeasgame-demo' / r.PROJECT
r.COMPOSE = r.ROOT / 'docker-compose/docker-compose.demo.yml'
HANDOFF = Path.home() / '.local/share/lifeasgame-integration/backend.json'
ACTORS = ('explorer', 'seller', 'buyer')
QUESTS = ('Q_RECORD_THREE_TRACES', 'Q_ADVENTURE_PREPARATION', 'Q_RECORD_FIRST_TRACE')
PRICE = 25  # Total price of the whole entry; never a unit price.


def check_namespace(value):
    r.check(re.fullmatch(r'[a-z0-9][a-z0-9-]{0,31}', value), 'Use a lowercase namespace of 1–32 letters/digits/hyphens.')
    return value


def atomic_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd, name = tempfile.mkstemp(dir=path.parent, prefix='.' + path.name)
    try:
        with os.fdopen(fd, 'w') as file:
            json.dump(data, file, ensure_ascii=False, indent=2)
            file.write('\n')
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


@contextmanager
def exclusive():
    r.STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    with (r.STATE / 'operation.lock').open('a') as file:
        try:
            fcntl.flock(file, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise RuntimeError('Another demo operation is running.') from None
        yield


def namespace_path(namespace):
    return r.STATE / 'namespaces' / check_namespace(namespace)


def command(action, namespace=None):
    args = ['python3', str(r.ROOT / 'scripts/demo.py'), action]
    if namespace:
        args += ['--namespace', namespace]
    return shlex.join(args)


def next_verification(seed):
    prefix = 'verify-' + hashlib.sha256(seed.encode()).hexdigest()[:16]
    index = 1
    while True:
        namespace = prefix + '-' + str(index)
        path = namespace_path(namespace) / 'verification.json'
        if not path.exists() or json.loads(path.read_text()).get('status') != 'passed':
            return namespace
        index += 1


def owned_handoff():
    if not HANDOFF.exists():
        return {}
    data = json.loads(HANDOFF.read_text())
    r.check(data.get('producer') == 'BE-DEMO-01' and data.get('environment', {}).get('project') == r.PROJECT,
            'backend.json belongs to another task/runtime; preserve it and resolve the handoff explicitly.')
    return data


def isolated():
    state = r.load()
    for service in ('app', 'mysql', 'redis'):
        ids = r.compose(state, 'ps', '-aq', service, capture_output=True).stdout.split()
        r.check(len(ids) == 1, 'Dedicated service missing: ' + service)
        container = json.loads(r.run(['docker', 'inspect', ids[0]], capture_output=True).stdout)[0]
        r.check(container['State']['Running'] and not container['State']['OOMKilled'], 'Service unavailable: ' + service)
        labels = container['Config']['Labels']
        r.check(labels['com.docker.compose.project.config_files'] == str(r.COMPOSE), 'Unexpected Compose definition.')
        r.check(set(container['NetworkSettings']['Networks']) == {r.PROJECT + '_default'}, 'Unexpected network attachment.')
        if service == 'app':
            env = dict(v.split('=', 1) for v in container['Config']['Env'] if '=' in v)
            r.check(env.get('SPRING_PROFILES_ACTIVE') == 'local,demo'
                    and env.get('DB_URL') == 'jdbc:mysql://mysql:3306/lifeasgame_cfc?useSSL=false&allowPublicKeyRetrieval=true'
                    and env.get('REDIS_HOST') == 'redis'
                    and env.get('DB_PASSWORD') == state['env']['CFC_DB_PASSWORD'], 'Dedicated profile/DB/Redis mismatch.')
            r.check(container['NetworkSettings']['Ports'].get('8080/tcp') == [
                {'HostIp': '127.0.0.1', 'HostPort': state['env']['CFC_API_PORT']}], 'HTTP binding mismatch.')
        else:
            r.check(not any(container['NetworkSettings']['Ports'].values()), 'DB/Redis must not publish ports.')
            mount = next(m for m in container['Mounts'] if m['Type'] == 'volume')
            expected = r.PROJECT + '_' + ('mysql_data' if service == 'mysql' else 'redis_data')
            r.check(mount['Name'] == expected, 'Unexpected data volume.')
    r.check(hashlib.sha256((r.STATE / 'app.jar').read_bytes()).hexdigest() == state['jar_sha256'], 'Runtime JAR changed.')
    return state


def preflight(port):
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', port))
    # Read the shared VM's available memory; never stop another project to make room.
    ids = r.run(['docker', 'ps', '-q'], capture_output=True).stdout.split()
    r.check(ids, 'Cannot inspect Docker VM memory without a running container.')
    info = r.run(['docker', 'exec', ids[0], 'cat', '/proc/meminfo'], capture_output=True).stdout
    available = int(re.search(r'MemAvailable:\s+(\d+)', info)[1])
    r.check(available >= 1400 * 1024, 'Docker VM needs at least 1400 MiB available; existing resources were preserved.')


def start(args):
    origin = urlsplit(args.origin)
    r.check(origin.scheme == 'http' and origin.hostname in ('localhost', '127.0.0.1')
            and origin.port and not origin.path and not origin.query and not origin.fragment
            and not origin.username and not origin.password, 'Provide one exact loopback FE origin with a port.')
    r.check(1024 <= args.port <= 65535, 'Invalid API port.')
    if not (r.STATE / 'runtime.json').exists():
        r.check(not r.resources(), 'Unowned demo resources exist; refusing adoption.')
        preflight(args.port)
        state = {'root': str(r.ROOT), 'env': {
            'CFC_OWNER': secrets.token_hex(16), 'CFC_DB_PASSWORD': secrets.token_urlsafe(32),
            'CFC_JWT_SECRET': secrets.token_urlsafe(48), 'CFC_RUNTIME_DIR': str(r.STATE),
            'CFC_API_PORT': str(args.port), 'CFC_ORIGINS': args.origin}}
        atomic_json(r.STATE / 'runtime.json', state)
    state = r.load()
    r.check(state['env']['CFC_API_PORT'] == str(args.port) and state['env']['CFC_ORIGINS'] == args.origin,
            'Existing runtime port/origin differ; reuse its recorded settings.')
    sha = r.run(['git', 'rev-parse', 'HEAD'], capture_output=True).stdout.strip()
    r.check(not r.run(['git', 'status', '--porcelain', '--untracked-files=no'], capture_output=True).stdout,
            'Commit tracked changes before packaging a reproducible runtime.')
    if not (r.STATE / 'app.jar').exists():
        r.run(['./gradlew', '--no-daemon', '--max-workers=1',
               '-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m', 'bootJar'])
        jars = [p for p in (r.ROOT / 'build/libs').glob('*.jar') if not p.name.endswith('-plain.jar')]
        r.check(len(jars) == 1, 'Expected one bootJar.')
        shutil.copyfile(jars[0], r.STATE / 'app.jar')
        state.update(sha=sha, dirty=False, jar_sha256=hashlib.sha256(jars[0].read_bytes()).hexdigest())
        atomic_json(r.STATE / 'runtime.json', state)
    # Tool-only fixes can reuse the exact application JAR; retain its actual packaging commit.
    result = subprocess.run(['git', 'diff', '--quiet', state['sha'], sha, '--',
                             'src/main', 'build.gradle', 'settings.gradle', 'gradle', 'gradlew'], cwd=r.ROOT)
    r.check(result.returncode == 0, 'Application source changed; preserve the pinned runtime and use a new worktree.')
    if not r.compose(state, 'ps', '-q', 'app', capture_output=True).stdout.strip():
        preflight(args.port)
    r.compose(state, 'up', '-d', '--wait', '--wait-timeout', '180')
    r.ready(state)
    isolated()
    _, headers = r.http(state, '/api/v1/economy/wallet', method='OPTIONS', headers={
        'Origin': state['env']['CFC_ORIGINS'], 'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'authorization,content-type,idempotency-key'})
    r.check(headers.get('Access-Control-Allow-Origin') == state['env']['CFC_ORIGINS'], 'Dedicated CORS origin mismatch.')
    r.http(state, '/api/v1/economy/wallet', expected=401)
    print('PASS dedicated runtime: ' + r.PROJECT)


def credentials(namespace):
    path = namespace_path(namespace) / 'credentials.json'
    if not path.exists():
        actors = {actor: {'email': f'demo-{namespace}-{actor}@example.invalid',
                          'password': secrets.token_urlsafe(24)} for actor in ACTORS}
        atomic_json(path, actors)
    r.check(path.is_file() and not path.is_symlink() and path.stat().st_mode & 0o777 == 0o600,
            'Credentials must be a regular 0600 file outside the repository.')
    return json.loads(path.read_text()), path


def login(state, namespace):
    creds, path = credentials(namespace)
    accounts, tokens = {}, {}
    for actor in ACTORS:
        credential = creds[actor]
        expected_email = f'demo-{namespace}-{actor}@example.invalid'
        r.check(credential['email'] == expected_email, 'Unexpected account identity.')
        if not r.sql(state, f"SELECT id FROM users WHERE email='{expected_email}'"):
            nickname = 'demo' + hashlib.sha256(expected_email.encode()).hexdigest()[:16]
            signup = r.api(state, '/api/v1/auth/register', method='POST', body={**credential, 'nickname': nickname})
            r.check(signup['requiresVerification'] is False, 'Normal signup requires verification; no bypass allowed.')
        result = r.api(state, '/api/v1/auth/login', method='POST', body=credential)
        if not result['playerId']:
            r.api(state, '/api/v1/players/register', method='POST', token=result['accessToken'],
                  expected=201, body={'name': '데모 ' + actor, 'gender': 'MALE'})
            result = r.api(state, '/api/v1/auth/login', method='POST', body=credential)
        accounts[actor] = {k: result[k] for k in ('userId', 'playerId')}
        accounts[actor]['email'] = expected_email
        tokens[actor] = result['accessToken']
        r.check(r.api(state, '/api/v1/players', token=tokens[actor])['playerId'] == result['playerId'],
                'Current-player identity mismatch.')
    r.check(len({a['userId'] for a in accounts.values()}) == 3
            and len({a['playerId'] for a in accounts.values()}) == 3, 'Accounts/players must be distinct.')
    return accounts, tokens, path


def api(state, token, path, **kwargs):
    return r.api(state, '/api/v1/' + path, token=token, **kwargs)


def poll(read, predicate, message, timeout=60):
    deadline = time.monotonic() + timeout
    while True:
        value = read()
        if predicate(value):
            return value
        if time.monotonic() >= deadline:
            raise RuntimeError(message + ' (60s deadline; no manual repair)')
        time.sleep(1)


def quest(state, token, code):
    return api(state, token, 'players/quests/' + code)


def accept(state, token, code):
    current = quest(state, token, code)['acceptance']
    if current is None:
        current = api(state, token, 'players/quests/' + code, method='POST', expected=201, body={})
    r.check(current['status'] != 'CANCELED', 'Existing acceptance was canceled; use a new namespace.')
    return current


def trace(state, token, namespace, actor, index):
    key = hashlib.sha256(f'BE-DEMO-01:{namespace}:{actor}:trace:{index}'.encode()).hexdigest()
    return api(state, token, 'lifelogs/quick-record', method='POST', expected=(200, 201),
               headers={'Idempotency-Key': key}, body={
                   'type': 'COLLECTION', 'lifeLogSubtype': 'QUICK_NOTE',
                   'collection': {'category': 'OTHER', 'title': f'데모 {namespace} {actor} 기록 {index}', 'quantity': 1}})


def settlement(state, token, code):
    acceptance = poll(lambda: quest(state, token, code)['acceptance'],
                      lambda a: a['status'] == 'COMPLETED', 'Quest did not complete: ' + code)
    path = 'reward-settlements/quest-completions/' + str(acceptance['id'])
    def read():
        data, _ = r.http(state, '/api/v1/' + path, token=token, expected=200)
        return data.get('result', data)
    # A settlement does not exist until the completion outbox is consumed.
    poll(lambda: r.sql(state, f"SELECT id FROM reward_settlements WHERE player_id="
                      f"{int(api(state, token, 'players')['playerId'])} AND source_id={int(acceptance['id'])}"),
         bool, 'Reward settlement was not created: ' + code)
    return poll(read, lambda s: s['status'] == 'COMPLETED'
                and all(line['status'] == 'SUCCEEDED' for line in s['lines']), 'Reward did not settle: ' + code)


def expected_rewards(state):
    rows = r.sql(state, "SELECT p.code,d.reward_type,COALESCE(l.amount_override,d.amount),COALESCE(d.item_code,'') "
                 "FROM reward_profiles p JOIN reward_profile_lines l ON l.reward_profile_id=p.id "
                 "JOIN reward_definitions d ON d.id=l.reward_definition_id WHERE p.status='ACTIVE' AND d.active=1 "
                 "AND p.code IN ('RP_EXP_TINY_10','RP_EXP_AND_ITEM_FIRST_STEP_20','RP_ADVENTURE_PREPARATION') "
                 "ORDER BY p.code,l.sort_order")
    profiles = {}
    for profile, kind, amount, item in rows:
        profiles.setdefault(profile, []).append({'type': kind, 'amount': int(amount), 'itemCode': item or None})
    r.check(profiles == {
        'RP_EXP_TINY_10': [{'type': 'EXP', 'amount': 10, 'itemCode': None}],
        'RP_EXP_AND_ITEM_FIRST_STEP_20': [{'type': 'EXP', 'amount': 20, 'itemCode': None},
                                        {'type': 'ITEM', 'amount': 1, 'itemCode': 'IT_FIRST_STEP_FRAGMENT'}],
        'RP_ADVENTURE_PREPARATION': [{'type': 'GOLD', 'amount': 100, 'itemCode': None},
                                   {'type': 'ITEM', 'amount': 1, 'itemCode': 'IT_RECORD_CRYSTAL'}]},
        'Approved seed reward profiles changed; inspect the contract before preparing.')
    item = r.sql(state, "SELECT id,reward_bound+0 FROM items WHERE code='IT_RECORD_CRYSTAL'")
    r.check(len(item) == 1 and item[0][1] == '0', 'Approved record crystal must be tradeable.')
    return profiles, int(item[0][0])


def inventory(state, token, item_id):
    return [e for e in api(state, token, 'inventory')['entries'] if e['itemId'] == item_id]


def observe(state, tokens, scenarios):
    return {'quests': {code: quest(state, tokens['explorer'], code)['acceptance'] for code in QUESTS},
            'growth': api(state, tokens['explorer'], 'players/growth'),
            'wallets': {actor: api(state, token, 'economy/wallet') for actor, token in tokens.items()},
            'inventories': {actor: api(state, token, 'inventory') for actor, token in tokens.items()},
            'mailbox': api(state, tokens['explorer'], 'mailbox'),
            'listings': api(state, tokens['seller'], 'economy/listings/me'),
            'reservations': api(state, tokens['buyer'], 'economy/listings/reservations'),
            'trades': {actor: api(state, tokens[actor], 'economy/trades') for actor in ('seller', 'buyer')},
            'persons': api(state, tokens['explorer'], 'persons'),
            'roles': api(state, tokens['explorer'], 'roles')}


def prepare(state, namespace):
    check_namespace(namespace)
    accounts, tokens, secret_path = login(state, namespace)
    path = namespace_path(namespace) / 'scenario.json'
    saved = json.loads(path.read_text()) if path.exists() else {}
    profiles, item_id = expected_rewards(state)
    if not saved.get('prepared'):
        explorer = tokens['explorer']
        for code in QUESTS[:2]:
            accept(state, explorer, code)
        for index in (1, 2):
            trace(state, explorer, namespace, 'explorer', index)
        for code in QUESTS[:2]:
            poll(lambda: quest(state, explorer, code)['acceptance'], lambda a: a['progressValue'] >= 2,
                 'Preparation progress did not converge: ' + code)
        accept(state, explorer, QUESTS[2])
        for actor in ('seller', 'buyer'):
            token = tokens[actor]
            accept(state, token, QUESTS[1])
            for index in (1, 2, 3):
                trace(state, token, namespace, actor, index)
            settlement(state, token, QUESTS[1])
            mails = api(state, token, 'mailbox')['entries']
            for mail in mails:
                if mail['itemId'] == item_id:
                    r.check(mail['bound'] is False, 'Never trade a bound reward.')
                    api(state, token, 'mailbox/claim', method='POST', expected=204,
                        body={'slotIndex': mail['slotIndex'], 'quantity': mail['quantity']})
        listings = api(state, tokens['seller'], 'economy/listings/me')['listings']
        r.check(len(listings) <= 1, 'Unexpected seller listings; preserve progress and use a new namespace.')
        if not listings:
            entries = inventory(state, tokens['seller'], item_id)
            r.check(len(entries) == 1 and not entries[0]['bound'], 'Expected one tradeable seller entry.')
            entry_id = entries[0]['itemInstanceId']
            api(state, tokens['seller'], 'economy/listings', method='POST',
                body={'inventoryEntryId': entry_id, 'price': PRICE, 'currency': 'GOLD'})
            listings = api(state, tokens['seller'], 'economy/listings/me')['listings']
        listing = listings[0]
        row = r.sql(state, f"SELECT item_inst_id FROM listings WHERE id={int(listing['id'])}")
        person_name = '데모 인물 ' + namespace
        people = [p for p in api(state, explorer, 'persons') if p['displayName'] == person_name]
        if not people:
            people = [api(state, explorer, 'persons', method='POST', expected=201, body={
                'displayName': person_name, 'notes': '허구의 데모 인물',
                'birthday': '2000-01-02', 'contact': 'fictional@example.invalid'})]
        r.check(len(people) == 1, 'Expected one demo Person.')
        saved = {'prepared': True, 'accounts': accounts, 'credentialsFile': str(secret_path), 'scenarios': {
            'quest': {'quests': {code: quest(state, explorer, code) for code in QUESTS},
                      'rewardProfiles': profiles, 'finalTraceIndex': 3,
                      'expectedExp': 30, 'expectedGold': 100,
                      'levelUp': 'Not prepared: normal 30 EXP is below the unchanged level-1 boundary.',
                      'steps': ['Save one COLLECTION/QUICK_NOTE LifeLog', 'Wait for all 3 completed settlements',
                                'Read players/growth, wallet, inventory and mailbox']},
            'trade': {'listingId': listing['id'], 'entryId': int(row[0][0]), 'itemId': item_id,
                      'itemCode': 'IT_RECORD_CRYSTAL', 'price': listing['price'], 'currency': 'GOLD',
                      'saleQuantity': listing['saleQuantity'], 'initialListingStatus': listing['status'],
                      'steps': ['Buyer reserves listing', 'Purchase with reservationToken and a new idempotencyKey',
                                'Replay identical purchase payload', 'Read wallets, inventories, trades, saleQuantity']},
            'person': {'personId': people[0]['id'], 'initialRoleCount': len(api(state, explorer, 'roles')),
                       'steps': ['Update Person', 'Create 2 Roles and link this Person to both',
                                 'Archive Person; read archived detail and both active relations',
                                 'Archive 1 relation; verify the other remains active']}}}
        initial = observe(state, tokens, saved['scenarios'])
        r.check(all(a['status'] == 'IN_PROGRESS' for a in initial['quests'].values())
                and [initial['quests'][c]['progressValue'] for c in QUESTS] == [2, 2, 0]
                and listing['status'] == 'OPEN' and listing['price'] == PRICE and listing['saleQuantity'] == 1
                and not initial['reservations']['reservations'] and not initial['roles'],
                'Namespace has progressed beyond preparation; preserve it and choose a fresh namespace.')
        saved['initial'] = initial
        atomic_json(path, saved)
    r.check(saved['accounts'] == accounts, 'Account identity changed; refusing to reset progress.')
    return saved, tokens, observe(state, tokens, saved['scenarios'])


def publish(state, namespace, saved, current, pr):
    old = owned_handoff()
    manifest = {'producer': 'BE-DEMO-01', 'status': 'ready', 'reason': None,
                'repo': str(r.ROOT), 'worktree': str(r.ROOT), 'commit': state['sha'],
                'toolCommit': r.run(['git', 'rev-parse', 'HEAD'], capture_output=True).stdout.strip(), 'pr': pr or old.get('pr'),
                'apiBaseUrl': 'http://127.0.0.1:' + state['env']['CFC_API_PORT'],
                'allowedFeOrigin': state['env']['CFC_ORIGINS'],
                'environment': {'project': r.PROJECT, 'volumes': [r.PROJECT + '_' + v for v in ('mysql_data', 'redis_data')],
                                'stopCommand': command('stop')},
                'seedNamespace': namespace, 'accounts': saved['accounts'], 'credentialsFile': saved['credentialsFile'],
                'scenarios': saved['scenarios'], 'initial': saved['initial'], 'current': current,
                'personStatusAvailable': False, 'personStatusReason': '#381 is not included in the pinned develop source.',
                'prepareCommand': command('prepare', namespace) + ' --publish' + (f' --pr {pr}' if pr else ''),
                'verifyCommand': command('verify', next_verification(namespace)),
                'verification': old.get('verification', {'status': 'not-run'})}
    atomic_json(HANDOFF, manifest)
    print('READY handoff: ' + str(HANDOFF))
    print('Credentials file: ' + saved['credentialsFile'])


def verify_quest(state, namespace, saved, tokens):
    before = saved['initial']
    explorer = tokens['explorer']
    trace_result = trace(state, explorer, namespace, 'explorer', 3)
    settlements = [settlement(state, explorer, code) for code in QUESTS]
    for settled in settlements:
        expected = saved['scenarios']['quest']['rewardProfiles'][settled['rewardProfileCode']]
        actual = [{'type': l['rewardType'], 'amount': l['amount'], 'itemCode': l['itemCode']} for l in settled['lines']]
        r.check(actual == expected, 'Settlement differs from approved reward profile.')
    growth = api(state, explorer, 'players/growth')
    r.check(growth['current']['exp'] == before['growth']['current']['exp'] + 30
            and sorted(c['appliedExp'] for c in growth['recentExpChanges']) == [10, 20]
            and {c['sourceId'] for c in growth['recentExpChanges']} ==
                {s['sourceId'] for s in settlements if any(l['rewardType'] == 'EXP' for l in s['lines'])},
            'EXP/growth history mismatch.')
    r.check(api(state, explorer, 'economy/wallet')['amount'] == 100, 'Explorer GOLD reward mismatch.')
    mails = api(state, explorer, 'mailbox')['entries']
    r.check(sorted((m['itemName'], m['quantity'], m['bound']) for m in mails) ==
            sorted([('기록 결정', 1, False), ('첫걸음의 조각', 1, True)]), 'Reward items/binding mismatch.')
    for mail in mails:
        api(state, explorer, 'mailbox/claim', method='POST', expected=204,
            body={'slotIndex': mail['slotIndex'], 'quantity': mail['quantity']})
    r.check(sorted((e['itemId'], e['quantity'], e['bound']) for e in api(state, explorer, 'inventory')['entries']) ==
            sorted((m['itemId'], m['quantity'], m['bound']) for m in mails), 'Mailbox claim delivery mismatch.')

    return {'trace': trace_result, 'settlementIds': [s['settlementId'] for s in settlements]}


def verify_trade(state, namespace, saved, tokens):
    trade = saved['scenarios']['trade']
    lid, item_id = trade['listingId'], trade['itemId']
    buyer, seller = tokens['buyer'], tokens['seller']
    reservation = api(state, buyer, f'economy/listings/{lid}/reserve', method='POST', body={'ttlSeconds': 120})
    balances = api(state, buyer, 'economy/wallet')['balances']
    r.check(balances[0]['available'] == 75 and balances[0]['held'] == PRICE, 'Reservation funds mismatch.')
    reservations = api(state, buyer, 'economy/listings/reservations')['reservations']
    r.check(len(reservations) == 1 and reservations[0]['saleQuantity'] == trade['saleQuantity'], 'Reservation snapshot mismatch.')
    payload = {'reservationToken': reservation['reservationToken'], 'idempotencyKey': 'demo-' + namespace}
    purchased = api(state, buyer, f'economy/listings/{lid}/purchase', method='POST', body=payload)
    after_purchase = observe(state, tokens, saved['scenarios'])
    replay = api(state, buyer, f'economy/listings/{lid}/purchase', method='POST', body=payload)
    r.check(replay == purchased and observe(state, tokens, saved['scenarios']) == after_purchase, 'Purchase replay repeated business effects.')
    r.check(after_purchase['listings']['listings'][0]['status'] == 'SOLD'
            and after_purchase['listings']['listings'][0]['saleQuantity'] == trade['saleQuantity']
            and not after_purchase['reservations']['reservations']
            and after_purchase['wallets']['buyer']['balances'][0] == {'currency': 'GOLD', 'available': 75, 'held': 0}
            and after_purchase['wallets']['seller']['amount'] == 125
            and not inventory(state, seller, item_id)
            and sum(e['quantity'] for e in inventory(state, buyer, item_id)) == 2
            and all(v['trades'] == [purchased] for v in after_purchase['trades'].values()), 'Trade effects mismatch.')
    r.check(r.sql(state, f'SELECT sale_quantity FROM trades WHERE id={int(purchased["id"])}') == [['1']], 'Trade quantity snapshot mismatch.')

    return {'tradeId': purchased['id']}


def verify_person(state, namespace, saved, tokens):
    explorer, buyer = tokens['explorer'], tokens['buyer']
    pid = saved['scenarios']['person']['personId']
    person_path = 'persons/' + str(pid)
    expected_name = '수정한 데모 인물 ' + namespace
    person = api(state, explorer, person_path)
    if person['status'] == 'ACTIVE':
        person = api(state, explorer, person_path, method='PUT', body={'displayName': expected_name,
            'notes': '허구의 관계 시연', 'birthday': '2000-01-02', 'contact': 'updated@example.invalid'})
    r.check(person['displayName'] == expected_name and person['notes'] == '허구의 관계 시연'
            and person['birthday'] == '2000-01-02' and person['contact'] == 'updated@example.invalid',
            'Person update fields mismatch.')
    relations = []
    for index in (1, 2):
        name = f'데모 역할 {index}'
        roles = [role for role in api(state, explorer, 'roles') if role['name'] == name]
        if not roles:
            roles = [api(state, explorer, 'roles', method='POST', expected=201,
                         body={'roleType': 'DEMO', 'name': name, 'description': '허구의 역할'})]
        r.check(len(roles) == 1, 'Ambiguous demo Role; refusing to recreate it.')
        role = roles[0]
        base = f'roles/{role["id"]}/relations'
        rows = r.sql(state, f'SELECT id FROM role_relations WHERE role_id={int(role["id"])} AND person_id={int(pid)}')
        if not rows:
            created = api(state, explorer, base, method='POST', expected=201,
                          body={'personId': pid, 'relationType': 'FRIEND', 'roleNotes': f'관계 메모 {index}'})
            relation_id = created['id']
        else:
            r.check(len(rows) == 1, 'Ambiguous demo relation.')
            relation_id = int(rows[0][0])
        # Compare persisted API business state; creation timestamps lose precision in MySQL.
        relation = api(state, explorer, base + '/' + str(relation_id))
        r.check(relation['personId'] == pid and relation['personDisplayName'] == expected_name
                and relation['relationType'] == 'FRIEND' and relation['roleNotes'] == f'관계 메모 {index}'
                and relation['linkedUserId'] is None, 'Relation reference/notes mismatch.')
        relations.append((base, relation))
    if person['status'] == 'ACTIVE':
        api(state, explorer, person_path, method='DELETE', expected=204)
    r.check(api(state, explorer, person_path)['status'] == 'ARCHIVED', 'Person archive failed.')
    available = []
    for base, relation in relations:
        current = api(state, explorer, base + '/' + str(relation['id']))
        fields = ('id', 'personId', 'personDisplayName', 'linkedUserId', 'relationType', 'roleNotes', 'status')
        r.check(all(current[k] == relation[k] for k in fields), 'Person archive changed relation business state.')
        r.check(api(state, explorer, base) == ([current] if current['status'] == 'ACTIVE' else []),
                'Relation list/detail mismatch.')
        available.append('personStatus' in current)
        if available[-1]:
            r.check(current['personStatus'] == 'ARCHIVED', 'Archived Person status mismatch.')
    base, relation = relations[0]
    if relation['status'] == 'ACTIVE':
        api(state, explorer, base + '/' + str(relation['id']), method='DELETE', expected=204)
    r.check(api(state, explorer, base + '/' + str(relation['id']))['status'] == 'ARCHIVED'
            and not api(state, explorer, base), 'Relation archive failed.')
    base, relation = relations[1]
    r.check(api(state, explorer, base) == [api(state, explorer, base + '/' + str(relation['id']))]
            and api(state, explorer, base)[0]['status'] == 'ACTIVE', 'Other relation did not survive.')
    r.http(state, '/api/v1/persons/' + str(pid), token=buyer, expected=404)
    return {'personStatusAvailable': all(available)}


def verify_social(state, namespace, saved, tokens):
    explorer = tokens['explorer']
    player_id = saved['accounts']['explorer']['playerId']
    social_ids = {}
    for kind in ('guilds', 'parties'):
        code = 'd' + hashlib.sha256((namespace + kind).encode()).hexdigest()[:16]
        id_column = 'guild_id' if kind == 'guilds' else 'party_id'
        rows = r.sql(state, f"SELECT {id_column} FROM {kind} WHERE code_value='{code}' AND player_id={int(player_id)}")
        if rows:
            r.check(len(rows) == 1, 'Ambiguous demo social group.')
            created = api(state, explorer, f'{kind}/{int(rows[0][0])}')
        else:
            created = api(state, explorer, kind, method='POST', body={
                'name': '데모 ' + kind, 'code': code,
                'visibility': 'PUBLIC', 'joinPolicy': 'OPEN', 'maxMembers': 5})
        r.check(api(state, explorer, f'{kind}/{created["id"]}')['leaderPlayerId'] == player_id,
                'Creator leader identity mismatch.')
        table, key = ('guild_members', 'guild_id') if kind == 'guilds' else ('party_members', 'party_id')
        r.check(r.sql(state, f'SELECT player_id,role FROM {table} WHERE {key}={int(created["id"])}') ==
                [[str(player_id), 'LEADER']], 'Creator LEADER membership mismatch.')
        social_ids[kind] = created['id']
    return {'socialIds': social_ids}


def verify(state, namespace):
    handoff = owned_handoff()
    r.check(namespace != handoff.get('seedNamespace'), 'Never consume the published demo namespace.')
    verification_path = namespace_path(namespace) / 'verification.json'
    progress = json.loads(verification_path.read_text()) if verification_path.exists() else {'phases': {}}
    r.check(progress.get('status') != 'passed', 'Already verified; use a fresh namespace for a new demonstration.')
    saved, tokens, _ = prepare(state, namespace)
    progress.update(status='running', namespace=namespace)
    atomic_json(verification_path, progress)
    # ponytail: checkpoint between phases; an interruption inside a trade may need a fresh namespace.
    for name, phase in [('quest', verify_quest), ('trade', verify_trade), ('person', verify_person), ('social', verify_social)]:
        if name not in progress['phases']:
            progress['phases'][name] = phase(state, namespace, saved, tokens)
            atomic_json(verification_path, progress)
            print('PASS real HTTP phase: ' + name, flush=True)
    final = observe(state, tokens, saved['scenarios'])
    _, _, rerun = prepare(state, namespace)
    r.check(rerun == final, 'Prepare rerun reset progress or repeated rewards.')
    result = {'status': 'passed', 'namespace': namespace, 'executedCommand': command('verify', namespace),
              'mode': 'real HTTP on dedicated MySQL/Redis',
              'phases': progress['phases'], 'prepareAfterConsumption': 'passed',
              'personStatusAvailable': progress['phases']['person']['personStatusAvailable']}
    atomic_json(verification_path, result)
    if handoff:
        handoff.update(status='ready', reason=None, verification=result, verifyCommand=command('verify', next_verification(handoff['seedNamespace'])),
                       personStatusAvailable=result['personStatusAvailable'])
        atomic_json(HANDOFF, handoff)
    print('PASS real HTTP scenarios; result: ' + str(verification_path))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('start', 'prepare', 'verify', 'stop'))
    parser.add_argument('--namespace')
    parser.add_argument('--port', type=int, default=19080)
    parser.add_argument('--origin', default='http://localhost:3000')
    parser.add_argument('--publish', action='store_true', help='prepare: atomically publish the FE handoff')
    parser.add_argument('--pr', type=int)
    args = parser.parse_args()
    if args.action in ('prepare', 'verify'):
        r.check(args.namespace is not None, '--namespace is required.')
        check_namespace(args.namespace)
    with exclusive():
        if args.publish:
            owned_handoff()  # Refuse foreign handoff before any account writes.
        try:
            if args.action == 'start':
                start(args)
            else:
                state = isolated()
                if args.action == 'stop':
                    r.compose(state, 'stop')  # Volumes remain; no global cleanup/reset command.
                    old = owned_handoff()
                    if old:
                        old.update(status='blocked', reason='Dedicated runtime stopped; restart the pinned runtime.')
                        atomic_json(HANDOFF, old)
                elif args.action == 'verify':
                    verify(state, args.namespace)
                else:
                    saved, _, current = prepare(state, args.namespace)
                    if args.publish:
                        publish(state, args.namespace, saved, current, args.pr)
                    print('PASS prepare (progress preserved): ' + args.namespace)
        except Exception as error:
            verification_path = namespace_path(args.namespace) / 'verification.json' if args.action == 'verify' else None
            attempted = verification_path and verification_path.exists()
            if attempted:
                progress = json.loads(verification_path.read_text())
                attempted = progress.get('status') != 'passed'
                if attempted:
                    progress.update(status='failed', reason=str(error) if isinstance(error, RuntimeError) else type(error).__name__)
                    atomic_json(verification_path, progress)
            if args.publish or args.action == 'start' or attempted:
                old = owned_handoff()
                reason = str(error) if isinstance(error, (RuntimeError, OSError)) else type(error).__name__
                old.update(producer='BE-DEMO-01', status='blocked', reason=f'{args.action}: {reason}',
                           environment={'project': r.PROJECT}, worktree=str(r.ROOT))
                atomic_json(HANDOFF, old)
            raise


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # Never dump HTTP bodies, tokens, subprocess environment or passwords.
        safe = str(error) if isinstance(error, (RuntimeError, OSError)) else type(error).__name__
        print('FAIL: ' + safe, file=sys.stderr)
        sys.exit(1)
