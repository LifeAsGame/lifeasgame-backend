#!/usr/bin/env python3
"""Apply the approved local verifier cohort through ordinary Current Player APIs."""
import csv
import json
import os
import pathlib
import secrets
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

BASE = os.environ.get('LAG_CONTENT_BASE_URL', 'http://127.0.0.1:19081')
SOURCE = pathlib.Path(os.environ['LAG_CONTENT_SOURCE_DIR'])
SPACE = pathlib.Path(os.environ['LAG_CONTENT_STATE_DIR'])
STATE = SPACE / 'state.json'
CREDS = SPACE / 'credentials.json'
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def save(path, value):
    temp = path.with_suffix('.tmp')
    fd = os.open(temp, os.O_CREAT | os.O_TRUNC | os.O_WRONLY, 0o600)
    with os.fdopen(fd, 'w') as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temp, path)


def call(path, method='GET', body=None, token=None, expected=(200, 201, 204)):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(BASE + path, method=method, headers=headers,
            data=None if body is None else json.dumps(body).encode())
    try:
        response = HTTP.open(request, timeout=25)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        value = json.loads(raw) if raw else {}
        if response.status not in expected:
            raise RuntimeError(f'{method} {path}: HTTP {response.status}, code={value.get("code")}')
        return value.get('result', value)


def rows(name):
    with (SOURCE / name).open(encoding='utf-8-sig', newline='') as stream:
        return list(csv.DictReader(stream))


def page(value):
    return value.get('contents', value) if isinstance(value, dict) else value


def marker(ref):
    return f'fixture:{state["scenario"]}:{state["cohort"]}:{ref}'


def id_for(ref):
    return state['records'][ref]['id']


def ensure(ref, listing, identity, create, detail=None):
    record = state['records'].get(ref)
    if record and record.get('id'):
        if detail:
            detail(record['id'])
        return record['id']
    # A pending journal is written before any non-idempotent POST. Recovery reads
    # the source by its namespace marker; an ambiguous result stops for inspection.
    matches = [v for v in page(listing()) if identity(v)]
    if len(matches) > 1:
        raise RuntimeError(f'ambiguous fixture recovery: {ref}')
    if matches:
        result = matches[0]
    elif record:
        raise RuntimeError(f'uncertain POST result for {ref}; inspect source before retry')
    else:
        state['records'][ref] = {'pending': True}
        save(STATE, state)
        result = create()
    ident = result.get('id', result.get('linkId'))
    if ident is None:
        raise RuntimeError(f'created {ref} without source ID: {result}')
    state['records'][ref] = {'id': ident}
    save(STATE, state)
    return ident


def action_once(ref, present, apply):
    prior = state['actions'].get(ref)
    if prior == 'done':
        return
    if present():
        state['actions'][ref] = 'done'
        save(STATE, state)
        return
    if prior == 'pending':
        raise RuntimeError(f'uncertain action {ref}; inspect source before retry')
    state['actions'][ref] = 'pending'
    save(STATE, state)
    apply()
    state['actions'][ref] = 'done'
    save(STATE, state)


def account(alias):
    credential = credentials[alias]
    try:
        auth = call('/api/v1/auth/login', 'POST', credential)
    except RuntimeError:
        # Registration may have completed before an interrupted response.
        try:
            call('/api/v1/auth/register', 'POST', {**credential,
                'nickname': 'cv' + credential['email'].split('@')[0][-12:]})
        except RuntimeError:
            pass
        auth = call('/api/v1/auth/login', 'POST', credential)
    if not auth['playerId']:
        call('/api/v1/players/register', 'POST', {'name': alias, 'gender': 'MALE'},
             auth['accessToken'], (201,))
        auth = call('/api/v1/auth/login', 'POST', credential)
    return auth


def token(alias='content-verifier-owner'):
    return accounts[alias]['accessToken']


def relation(role, person, ref, kind):
    base = f'/api/v1/roles/{role}/relations'
    return ensure(ref, lambda: call(base, token=token()),
        lambda v: v.get('roleNotes') == marker(ref),
        lambda: call(base, 'POST', {'personId': person, 'relationType': kind,
                                   'roleNotes': marker(ref)}, token()))


def member(group_kind, group_id, alias):
    base = f'/api/v1/{group_kind}/{group_id}'
    pid = accounts[alias]['playerId']
    def present():
        return any(v.get('playerId') == pid for v in page(call(base + '/members', token=token())))
    def apply():
        call(base + '/invite', 'POST', {'inviteePlayerId': pid}, token())
        call(base + '/accept-invitation', 'POST', token=token(alias))
        assert present()
    action_once(f'MEMBER:{group_kind}:{group_id}:{alias}', present, apply)


def main():
    global state, credentials, accounts
    SPACE.mkdir(mode=0o700, parents=True, exist_ok=True)
    if STATE.exists():
        state = json.loads(STATE.read_text())
    else:
        if CREDS.exists():
            raise RuntimeError('credential file exists without fixture state; recover it first')
        state = {'scenario': 'RELEASE_CONTENT_PLAN_2026.10_V2',
                 'cohort': str(uuid.uuid4()),
                 'anchorKstDate': datetime.now(ZoneInfo('Asia/Seoul')).date().isoformat(),
                 'records': {}, 'actions': {}}
        save(STATE, state)
    state.setdefault('actions', {})
    if not CREDS.exists():
        credentials = {alias: {'email': f'{alias}-{state["cohort"][:8]}@example.invalid',
                    'password': secrets.token_urlsafe(24)} for alias in
                    ('content-verifier-owner', 'content-verifier-peer-dev', 'content-verifier-peer-band')}
        save(CREDS, credentials)
    else:
        credentials = json.loads(CREDS.read_text())
    assert STATE.stat().st_mode & 0o777 == CREDS.stat().st_mode & 0o777 == 0o600
    accounts = {alias: account(alias) for alias in credentials}
    owner = token()
    entities = rows('06_Verification_Scenario_Entities_v2.csv')
    for row in entities:
        ref, kind = row['refCode'], row['entityType']
        if kind == 'ROLE':
            ensure(ref, lambda: call('/api/v1/roles', token=owner),
                   lambda v: v.get('description') == marker(ref),
                   lambda: call('/api/v1/roles', 'POST', {'roleType': row['typeOrPurpose'],
                      'name': row['displayName'], 'description': marker(ref)}, owner))
        elif kind == 'PERSON':
            ensure(ref, lambda: call('/api/v1/persons', token=owner),
                   lambda v: v.get('notes') == marker(ref),
                   lambda: call('/api/v1/persons', 'POST', {'displayName': row['displayName'],
                       'notes': marker(ref)}, owner))
        elif kind in ('GUILD', 'PARTY'):
            endpoint = '/api/v1/' + ('guilds' if kind == 'GUILD' else 'parties')
            code = 'CV' + uuid.uuid5(uuid.UUID(state['cohort']), ref).hex[:12].upper()
            ensure(ref, lambda: call(endpoint + '/mine', token=owner),
                   lambda v: v.get('code') == code or v.get('descriptionMd') == marker(ref),
                   lambda: call(endpoint, 'POST', {'name': row['displayName'], 'code': code,
                       'descriptionMd': marker(ref), 'visibility': 'PRIVATE',
                       'joinPolicy': 'INVITE_ONLY', 'maxMembers': 10}, owner))
        elif kind == 'ROLE_PARTY':
            role_id = id_for(row['contextRefCode'])
            endpoint = f'/api/v1/roles/{role_id}/role-parties'
            ensure(ref, lambda: call(endpoint, token=owner),
                   lambda v: v.get('description') == marker(ref),
                   lambda: call(endpoint, 'POST', {'name': row['displayName'],
                       'description': marker(ref), 'maxMembers': 10}, owner))

    links = rows('07_Verification_Scenario_Memberships_Relations_v2.csv')
    for row in links:
        kind, ref = row['linkType'], row['refCode']
        if kind == 'RELATION':
            relation(id_for(row['containerRefCode']), id_for(row['memberRefCode']),
                     ref, row['membershipOrRelationType'])
        elif kind == 'MEMBERSHIP' and row['membershipOrRelationType'] == 'MEMBER' \
                and (row['containerRefCode'].startswith('SCN_GUILD')
                     or row['containerRefCode'].startswith('SCN_PARTY')):
            container = row['containerRefCode']
            if container.startswith('SCN_GUILD'):
                member('guilds', id_for(container), row['memberRefCode'])
            elif container.startswith('SCN_PARTY'):
                member('parties', id_for(container), row['memberRefCode'])
        elif kind == 'FOLLOW_PREREQUISITE':
            peer = 'content-verifier-peer-band'
            for actor, target in [('content-verifier-owner', peer), (peer, 'content-verifier-owner')]:
                target_id = accounts[target]['playerId']
                action_once(f'FOLLOW:{actor}:{target}',
                    lambda: any(v.get('targetPlayerId') == target_id and v.get('state') == 'ACTIVE'
                        for v in page(call('/api/v1/follows/followings', token=token(actor)))),
                    lambda: call('/api/v1/follows', 'POST', {'targetPlayerId': target_id}, token(actor)))
        elif kind == 'MEMBERSHIP' and row['containerRefCode'].startswith('SCN_ROLEPARTY') \
                and row['membershipOrRelationType'] == 'MEMBER':
            group = id_for(row['containerRefCode'])
            peer = row['memberRefCode']
            def present():
                return any(v.get('playerId') == accounts[peer]['playerId'] for v in
                    page(call(f'/api/v1/role-parties/{group}/members', token=owner)))
            def apply():
                invite = call(f'/api/v1/role-parties/{group}/invitations', 'POST',
                              {'inviteePlayerId': accounts[peer]['playerId']}, owner)
                call(f'/api/v1/role-parties/{group}/invitations/{invite["invitationId"]}/accept',
                     'POST', token=token(peer))
            action_once(f'MEMBER:role-party:{group}:{peer}', present, apply)
        elif kind == 'GROUP_LINK':
            role = id_for(row['containerRefCode'])
            group = id_for(row['memberRefCode'])
            group_kind = row['memberRefType']
            path = f'/api/v1/roles/{role}/group-links'
            action_once(ref,
                lambda: any(v.get('groupType') == group_kind and v.get('groupId') == group
                    for v in page(call(path, token=owner))),
                lambda: call(path, 'POST', {'groupType': group_kind, 'groupId': group}, owner))

    anchor = datetime.fromisoformat(state['anchorKstDate']).replace(tzinfo=ZoneInfo('Asia/Seoul'))
    for row in rows('08_Verification_Scenario_Activities_v2.csv'):
        ref, kind = row['refCode'], row['activitySourceKind']
        start = anchor + timedelta(days=int(row['offsetDays']))
        hour, minute = map(int, row['startLocalTime'].split(':'))
        start = start.replace(hour=hour, minute=minute)
        end = start + timedelta(minutes=int(row['durationMinutes']))
        times = {'startsAt': start.isoformat(), 'endsAt': end.isoformat()}
        if kind == 'ROLE_EVENT':
            path = f'/api/v1/roles/{id_for(row["projectionRoleRefCode"])}/events'
            payload = {'title': row['displayName'], 'description': marker(ref), **times}
            ident = ensure(ref, lambda: call(path, token=owner),
                           lambda v: v.get('description') == marker(ref),
                           lambda: call(path, 'POST', payload, owner))
            state['records'][ref].setdefault('materialized', times)
            save(STATE, state)
            detail = call(f'{path}/{ident}', token=owner)
            for participant in row['participantOrRsvpRefs'].split(';'):
                ptype, value = participant.split(':', 1)
                if ptype == 'PERSON':
                    ptype, pid = 'PERSON', id_for(value)
                else:
                    ptype, pid = 'SERVICE_USER', accounts[value.removeprefix('userId(').removesuffix(')')]['userId']
                action_once(f'PARTICIPANT:{ref}:{ptype}:{pid}',
                    lambda: any(v['participantType'] == ptype and v['participantId'] == pid
                        for v in call(f'{path}/{ident}', token=owner)['participants']),
                    lambda: call(f'{path}/{ident}/participants', 'POST',
                         {'participantType': ptype, 'participantId': pid}, owner))
        else:
            group = id_for(row['groupRefCode'])
            path = (f'/api/v1/guilds/{group}/events' if kind == 'GUILD_EVENT' else
                    f'/api/v1/{"parties" if kind == "PARTY_ACTIVITY" else "role-parties"}/{group}/activities')
            payload = {'title': row['displayName'], 'sharedDescription': marker(ref), **times}
            if kind != 'GUILD_EVENT':
                payload['clientRequestId'] = str(uuid.uuid5(uuid.UUID(state['cohort']),
                                              state['scenario'] + ':' + ref))
            ident = ensure(ref, lambda: call(path, token=owner),
                           lambda v: v.get('sharedDescription') == marker(ref),
                           lambda: call(path, 'POST', payload, owner))
            state['records'][ref].setdefault('materialized', times)
            save(STATE, state)
            for part in row['participantOrRsvpRefs'].split(';'):
                alias = part.removeprefix('RSVP_PLAYER_ALIAS:')
                action_once(f'RSVP:{ref}:{alias}',
                    lambda: any(v.get('playerId') == accounts[alias]['playerId'] for v in
                        page(call(f'{path}/{ident}/participants', token=owner))),
                    lambda: call(f'{path}/{ident}/rsvp', 'PUT', token=token(alias)))

    print(json.dumps({'cohort': state['cohort'], 'anchorKstDate': state['anchorKstDate'],
                      'recordCount': len(state['records']), 'recordIds':
                      {k: v['id'] for k, v in state['records'].items()}}, ensure_ascii=False))


if __name__ == '__main__':
    main()
