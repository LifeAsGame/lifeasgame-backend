"""Conservative PR classifier: only known non-input guides can skip heavy CI steps."""

import os
import subprocess
import sys

GUIDES = {b"README.md", b"FORSETTING.md"}


def paths_from_name_status(raw):
    fields = raw.split(b"\0")
    if not fields or fields[-1] != b"":
        raise ValueError("incomplete git diff output")
    fields.pop()
    paths = []
    index = 0
    while index < len(fields):
        status = fields[index]
        index += 1
        if not status or status[:1] not in (b"A", b"M", b"D", b"T", b"R", b"C"):
            raise ValueError("unknown diff status")
        count = 2 if status[:1] in (b"R", b"C") else 1
        if index + count > len(fields):
            raise ValueError("missing path")
        part = fields[index:index + count]
        if any(not path for path in part):
            raise ValueError("empty path")
        paths.extend(part)
        index += count
    if not paths:
        raise ValueError("empty diff")
    return paths


def guide_only(base, head):
    try:
        merge_base = subprocess.run(["git", "merge-base", base, head], check=True,
                                    capture_output=True).stdout.strip()
        if not merge_base:
            return False
        changed = subprocess.run(["git", "diff", "--name-status", "-z", "-M",
                                  merge_base, head], check=True, capture_output=True).stdout
        return all(path in GUIDES for path in paths_from_name_status(changed))
    except (OSError, subprocess.CalledProcessError, ValueError):
        return False


def main():
    base = os.environ.get("PR_BASE_SHA", "")
    head = os.environ.get("PR_HEAD_SHA", "")
    heavy = not (base and head and guide_only(base, head))
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"heavy={'true' if heavy else 'false'}\n")
    print("Full test/build required" if heavy else "Verified guide-only PR")


if __name__ == "__main__":
    sys.exit(main())
