"""Focused checks for preparation safety; no app/DB or third-party test framework."""
import importlib.util
from unittest.mock import MagicMock
import json
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('demo', Path(__file__).with_name('demo.py'))
demo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(demo)


class DemoSafetyTest(unittest.TestCase):
    def test_rejects_namespace_path_and_sql_injection(self):
        for value in ('../other', "demo' OR 1=1", 'UPPER', '', 'a' * 33):
            with self.subTest(value=value), self.assertRaises(RuntimeError):
                demo.check_namespace(value)
        self.assertEqual(demo.check_namespace('demo-20260929'), 'demo-20260929')

    def test_atomic_credentials_are_private_and_stable(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(demo.r, 'STATE', Path(directory)):
            original, path = demo.credentials('demo')
            again, _ = demo.credentials('demo')
            self.assertEqual(original, again)
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            self.assertEqual(len({a['email'] for a in original.values()}), 3)
            path.chmod(0o644)
            with self.assertRaises(RuntimeError):
                demo.credentials('demo')

    def test_does_not_replace_another_tasks_handoff(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'backend.json'
            demo.atomic_json(path, {'producer': 'another-task', 'status': 'ready'})
            original = path.read_bytes()
            with patch.object(demo, 'HANDOFF', path), self.assertRaises(RuntimeError):
                demo.owned_handoff()
            self.assertEqual(path.read_bytes(), original)

    def test_accept_reuses_existing_progress_without_post(self):
        current = {'id': 1, 'status': 'IN_PROGRESS', 'progressValue': 2}
        with patch.object(demo, 'quest', return_value={'acceptance': current}), patch.object(demo, 'api') as api:
            self.assertEqual(demo.accept({}, 'token', 'code'), current)
            api.assert_not_called()

    def test_prepared_namespace_only_observes_existing_progress(self):
        saved = {'prepared': True, 'accounts': {'existing': 'identity'}, 'scenarios': {}}
        with tempfile.TemporaryDirectory() as directory, patch.object(demo.r, 'STATE', Path(directory)):
            demo.atomic_json(demo.namespace_path('demo') / 'scenario.json', saved)
            with patch.object(demo, 'login', return_value=(saved['accounts'], {}, Path(directory))), \
                 patch.object(demo, 'expected_rewards', return_value=({}, 1)), \
                 patch.object(demo, 'observe', return_value={'alreadySold': True}), \
                 patch.object(demo, 'trace') as trace, patch.object(demo, 'accept') as accept:
                result, _, observed = demo.prepare({}, 'demo')
            self.assertEqual(result, saved)
            self.assertEqual(observed, {'alreadySold': True})
            trace.assert_not_called()
            accept.assert_not_called()

    def test_verify_refuses_to_consume_published_namespace(self):
        with patch.object(demo, 'owned_handoff', return_value={'seedNamespace': 'demo'}), \
             patch.object(demo, 'prepare') as prepare, self.assertRaises(RuntimeError):
            demo.verify({}, 'demo')
        prepare.assert_not_called()

    def test_http_accepts_record_creation_and_replay_but_rejects_server_error(self):
        state = {'env': {'CFC_API_PORT': '19080'}}
        for status in (200, 201, 500):
            response = MagicMock()
            response.__enter__.return_value = response
            response.status = status
            response.read.return_value = b'{"isSuccess":true,"result":{"replay":true}}'
            response.headers = {'Content-Type': 'application/json'}
            with self.subTest(status=status), patch.object(demo.r.urllib.request, 'build_opener') as opener:
                opener.return_value.open.return_value = response
                if status == 500:
                    with self.assertRaises(RuntimeError):
                        demo.r.http(state, '/api/v1/lifelogs/quick-record', expected=(200, 201))
                else:
                    self.assertTrue(demo.r.http(state, '/api/v1/lifelogs/quick-record', expected=(200, 201))[0]['isSuccess'])

    def test_settlement_waits_for_completed_parent_and_succeeded_lines(self):
        completed = {'status': 'COMPLETED', 'lines': [{'status': 'SUCCEEDED'}]}
        pending = {'status': 'PENDING', 'lines': [{'status': 'PENDING'}]}
        with patch.object(demo, 'quest', return_value={'acceptance': {'id': 9, 'status': 'COMPLETED'}}), \
             patch.object(demo, 'api', return_value={'playerId': 1}), \
             patch.object(demo.r, 'sql', return_value=[['1']]), \
             patch.object(demo.r, 'http', side_effect=[({'result': pending}, {}), ({'result': completed}, {})]), \
             patch.object(demo.time, 'sleep'):
            self.assertEqual(demo.settlement({}, 'token', 'code'), completed)

    def test_verify_resumes_only_unfinished_phases(self):
        completed = {'quest': {'settlementIds': [1]}, 'trade': {'tradeId': 2},
                     'person': {'personStatusAvailable': False}}
        with tempfile.TemporaryDirectory() as directory, patch.object(demo.r, 'STATE', Path(directory)):
            path = demo.namespace_path('smoke') / 'verification.json'
            demo.atomic_json(path, {'status': 'failed', 'namespace': 'smoke', 'phases': completed})
            with patch.object(demo, 'owned_handoff', return_value={}), \
                 patch.object(demo, 'prepare', return_value=({'scenarios': {}}, {}, {'unchanged': True})), \
                 patch.object(demo, 'observe', return_value={'unchanged': True}), \
                 patch.object(demo, 'verify_quest') as quest, patch.object(demo, 'verify_trade') as trade, \
                 patch.object(demo, 'verify_person') as person, \
                 patch.object(demo, 'verify_social', return_value={'socialIds': {'guilds': 3, 'parties': 4}}) as social:
                demo.verify({}, 'smoke')
            quest.assert_not_called()
            trade.assert_not_called()
            person.assert_not_called()
            social.assert_called_once()
            self.assertEqual(json.loads(path.read_text())['status'], 'passed')

    def test_poll_is_bounded(self):
        with patch.object(demo.time, 'monotonic', side_effect=[0, 60]), \
             patch.object(demo.time, 'sleep') as sleep, self.assertRaises(RuntimeError):
            demo.poll(lambda: 'PENDING', lambda v: v == 'COMPLETED', 'not settled')
        sleep.assert_not_called()


if __name__ == '__main__':
    unittest.main()
