#!/usr/bin/env python3
"""Inspect expired portfolio runs; prune only their short-lived proof/link records."""
import argparse
import json
import subprocess
from pathlib import Path

PROJECT = "lag-demo-129fd1f60637"
STATE = Path.home() / ".local/share/lifeasgame-demo" / PROJECT
LABEL = "online.lifeasgame.cfc-owner"


def run(*args, input=None):
    return subprocess.run(args, input=input, text=True, check=True, capture_output=True).stdout


def database():
    state = json.loads((STATE / "runtime.json").read_text())
    assert state["env"]["CFC_API_PORT"] == "19081", "Not the dedicated 19081 runtime"
    ids = run("docker", "ps", "-q", "--filter", f"label=com.docker.compose.project={PROJECT}").splitlines()
    mysql = []
    for container_id in ids:
        info = json.loads(run("docker", "inspect", container_id))[0]
        labels = info["Config"]["Labels"]
        assert labels.get(LABEL) == state["env"]["CFC_OWNER"], "Foreign container"
        if labels.get("com.docker.compose.service") == "mysql":
            mysql.append(container_id)
    assert len(mysql) == 1, "Dedicated MySQL container unavailable"
    return mysql[0]


def sql(container, statement):
    return run(
        "docker", "exec", "-i", container, "sh", "-c",
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 '
        '-uroot --batch --skip-column-names lifeasgame_cfc',
        input=statement + "\n",
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="prune only expired proof/link rows")
    parser.add_argument("--dry-run", action="store_true", help="show targets without deleting anything (default)")
    args = parser.parse_args()
    if args.apply and args.dry_run:
        parser.error("Choose --apply or --dry-run")
    container = database()
    targets = sql(container,
        "SELECT r.id,r.status, "
        "(SELECT COUNT(*) FROM portfolio_demo_actors a WHERE a.run_id=r.id), "
        "(SELECT COUNT(*) FROM listings l WHERE l.seller_player_id IN "
        "(SELECT a.player_id FROM portfolio_demo_actors a WHERE a.run_id=r.id)), "
        "(SELECT COUNT(*) FROM trades t WHERE t.seller_player_id IN "
        "(SELECT a.player_id FROM portfolio_demo_actors a WHERE a.run_id=r.id) OR t.buyer_player_id IN "
        "(SELECT a.player_id FROM portfolio_demo_actors a WHERE a.run_id=r.id)), "
        "(SELECT COUNT(*) FROM chat_messages m WHERE m.sender_id IN "
        "(SELECT a.player_id FROM portfolio_demo_actors a WHERE a.run_id=r.id)), "
        "(SELECT COUNT(*) FROM portfolio_demo_peer_links p WHERE p.run_id=r.id) "
        "FROM portfolio_demo_runs r WHERE r.status IN ('CLOSED','EXPIRED') "
        "OR r.expires_at<UTC_TIMESTAMP(6) ORDER BY r.created_at")
    print("Expired/closed targets (runId, status, actors, listings, trades, chat messages, peer links):")
    print(targets.rstrip() or "(none)")
    print("Domain accounts, balances, inventory, listings, trades, chat, receipts, outbox and run/actor "
          "revocation rows are retained. The global 500-run cap prevents unbounded creation.")
    if not args.apply:
        print("Dry run only. Pass --apply to prune expired short-lived proof/link rows.")
        return
    statement = (
        "START TRANSACTION; "
        "DELETE p FROM portfolio_demo_peer_links p JOIN portfolio_demo_runs r ON r.id=p.run_id "
        "WHERE (r.status IN ('CLOSED','EXPIRED') OR r.expires_at<UTC_TIMESTAMP(6)) "
        "AND p.expires_at<UTC_TIMESTAMP(6); "
        "DELETE p FROM portfolio_demo_proofs p JOIN portfolio_demo_runs r ON r.proof_id=p.id "
        "WHERE (r.status IN ('CLOSED','EXPIRED') OR r.expires_at<UTC_TIMESTAMP(6)) "
        "AND p.expires_at<UTC_TIMESTAMP(6); COMMIT;"
    )
    sql(container, statement)
    print("Pruned only expired peer links and proofs for the listed run scope.")


if __name__ == "__main__":
    main()
