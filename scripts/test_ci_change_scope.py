import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import ci_change_scope as scope


class ChangeScopeTest(unittest.TestCase):
    def test_only_allowlisted_guides_skip(self):
        for name_status, expected in [
            (b"M\0README.md\0", True),
            (b"D\0FORSETTING.md\0", True),
            (b"A\0README.md\0", True),
            (b"R100\0README.md\0FORSETTING.md\0", True),
            (b"C100\0README.md\0FORSETTING.md\0", True),
            (b"M\0README.md\0M\0src/main/App.java\0", False),
            (b"M\0src/main/resources/db/migration/V47.sql\0", False),
            (b"M\0docs/contracts/social/contract.md\0", False),
            (b"M\0.github/workflows/ci.yml\0", False),
            (b"R100\0src/old.java\0README.md\0", False),
            (b"R100\0README.md\0docs/contracts/new.md\0", False),
            (b"D\0src/test/AppTest.java\0", False),
        ]:
            with self.subTest(name_status=name_status):
                self.assertEqual(all(p in scope.GUIDES for p in scope.paths_from_name_status(name_status)), expected)

    def test_unknown_or_incomplete_diff_runs_full(self):
        for raw in [b"", b"X\0README.md\0", b"T\0README.md\0",
                    b"Mextra\0README.md\0", b"R101\0README.md\0FORSETTING.md\0",
                    b"R100\0README.md\0", b"M\0README.md"]:
            with self.subTest(raw=raw), self.assertRaises(ValueError):
                scope.paths_from_name_status(raw)
        with patch.object(scope.subprocess, "run", side_effect=OSError("fetch failed")):
            self.assertFalse(scope.guide_only("base", "head"))

    def test_sha_and_merge_base_must_be_unambiguous(self):
        from subprocess import CompletedProcess
        sha = "a" * 40
        with patch.object(scope.subprocess, "run") as run:
            self.assertFalse(scope.guide_only("a" * 39, sha))
            self.assertFalse(scope.guide_only(sha + "é", sha))
            run.assert_not_called()
            run.return_value = CompletedProcess([], 0, (sha + "\n" + "b" * 40 + "\n").encode())
            self.assertFalse(scope.guide_only(sha, sha))
            run.return_value = CompletedProcess([], 0, b"")
            self.assertFalse(scope.guide_only(sha, sha))

    def test_workflow_only_skips_on_explicit_false(self):
        workflow = (Path(__file__).resolve().parents[1] / ".github/workflows/ci.yml").read_text()
        self.assertEqual(workflow.count("if: steps.classify.outputs.heavy != 'false'"), 4)
        self.assertIn("if: steps.classify.outputs.heavy == 'false'", workflow)

    def test_merge_base_covers_code_in_earlier_commit(self):
        import subprocess
        with tempfile.TemporaryDirectory() as folder:
            repo = Path(folder)
            def git(*args):
                return subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True).stdout.decode().strip()
            git("init", "-q")
            git("config", "user.email", "ci@example.invalid")
            git("config", "user.name", "CI Test")
            (repo / "README.md").write_text("guide\n")
            git("add", ".")
            git("commit", "-qm", "base")
            base = git("rev-parse", "HEAD")
            (repo / "src").mkdir()
            (repo / "src" / "App.java").write_text("class App {}\n")
            git("add", ".")
            git("commit", "-qm", "code")
            (repo / "README.md").write_text("updated guide\n")
            git("commit", "-qam", "guide")
            head = git("rev-parse", "HEAD")
            original_run = subprocess.run
            with patch.object(scope.subprocess, "run", side_effect=lambda *a, **k: original_run(*a, **k, cwd=repo)):
                self.assertFalse(scope.guide_only(base, head))


if __name__ == "__main__":
    unittest.main()
