# Reproducible backend demo

`scripts/demo.py` uses ordinary signup, login, onboarding, LifeLog, quest,
reward, mailbox and marketplace APIs. It reuses the CFC runtime helpers and
Compose topology in a separate `lag-demo-*` project derived from the checkout
path. Nothing runs during ordinary application startup. No fixture SQL writes,
admin endpoint, auth bypass, new content or migration is needed.

## Start and prepare

Use a clean committed worktree with Java 21, Docker Compose and Python 3.
Inspect existing processes, ports and Docker VM memory first. Preserve other
projects. Set `FE_ORIGIN` to the **exact free loopback origin** the frontend
will use (including its port), and choose an unused API port:

```sh
python3 -B scripts/demo.py start --port 19080 --origin "$FE_ORIGIN"
python3 -B scripts/demo.py prepare --namespace demo-20260929 --publish
```

The tool refuses an occupied API port, insufficient VM memory, foreign
resources, unexpected network/profile/DB bindings, changed JAR or incompatible
application source. The dedicated Compose services have memory limits. MySQL and
Redis publish no host ports; only loopback HTTP is exposed. CORS is changed
only in this dedicated runtime. Existing servers, containers and volumes are
not stopped or reset. Restart with the same port/origin. Tool-only fixes can
reuse the original JAR when application inputs match; the handoff retains the
actual packaging commit separately from the current tool commit.

Preparation leaves three distinct ordinary accounts:

- **explorer:** two LifeLogs, three accepted quests (progress 2/3, 2/3, 0/1),
  one fictional Person visible with zero Roles. The last LifeLog is left to the
  user. Approved profiles yield 30 EXP, 100 GOLD, one bound first-step fragment
  and one unbound record crystal. The unchanged level-1 curve needs 100 EXP;
  this scenario does not synthesize an EXP/level fixture.
- **seller:** completes adventure preparation through three real LifeLogs,
  claims its unbound record crystal and lists the whole entry (quantity 1).
- **buyer:** completes the same account-once quest and claims its own crystal;
  its 100 GOLD funds the purchase. No top-up or external payment is used.

The listing's price is the **total whole-entry price** (25 GOLD). Current
trade policy rounds the 1% fee down to zero for this price: buyer ends at
75 GOLD, seller at 125 GOLD, buyer at two crystals, seller at zero.

Namespaces determine account emails and LifeLog idempotency keys. Credentials
are generated once into a 0600 JSON file outside Git; their path is printed.
Preparation uses stored identities and existing results after interruption.
Repeating preparation never resets progress, replaces a sold listing, adds
records or repays a reward. Use a new namespace for a fresh demonstration.
One local operation holds an exclusive lock; this is a local scenario tool,
not a distributed provisioning service. Keep its private state directory.

## Consumptive verification

```sh
python3 -B scripts/demo.py verify --namespace demo-20260929-smoke
```

Verification prepares that separate namespace with the same code, then runs:

1. Last LifeLog → completed quests → completed settlement lines (bounded waits)
   → EXP history, wallet, mailbox and item claim into inventory.
2. Buyer reservation → purchase → identical idempotent replay; wallet holds,
   inventories, sale quantity snapshots and both users' trade history.
3. Person update → two Roles sharing that Person → Person archive preserving
   both relations → one relation archive preserving the other. It also checks
   another account cannot read the Person.
4. One Guild and Party creation; HTTP detail and read-only DB checks establish
   creator LEADER membership.
5. Prepare after consumption preserves the exact current business state.

It refuses the published demonstration namespace and an already passed
verification namespace. Completed quest/trade/person phases are retained and
skipped on resume. Checkpoints are between phases: an interruption inside a
reservation/purchase phase may require a fresh namespace. Existing
accounts/data are retained. Use a **new** namespace for a new full demonstration. Do not repeat full application regression
locally: the normal PR CI runs `clean test build` and the Python safety checks.

## Frontend handoff and stop

The fixed handoff is `$HOME/.local/share/lifeasgame-integration/backend.json`.
It contains status/reason, pinned commit, worktree, optional PR, API URL, exact
FE origin, environment and stop command, account IDs/emails, credentials file
path, real scenario IDs/initial/current state and verification result/command.
The next verification command selects a fresh namespace or resumes an
interrupted one; the previous executed command and result remain in
`verification`. No passwords or bearer/reservation tokens are written there. Use
`allowedFeOrigin` for the follow-up frontend server and `apiBaseUrl` for its
API client. Read the private credentials file locally for normal login.

Publishing is atomic and refuses another task's handoff. Failed runtime,
preparation or attempted verification publishes `blocked` with a reason, never
a partial `ready` result. Add a PR reference without
recreating data by repeating `prepare --publish --pr <number>`.

PR #381 is deliberately excluded while review-blocked. This version verifies
archived Person detail and preserved relations; `personStatusAvailable` is
false. No review dismissal, cherry-pick, comment or merge bypass is performed.

```sh
python3 -B scripts/demo.py stop
```

Stop affects only this owned project and retains its volumes and private state.
There is no global cleanup/reset command. The successful environment is left
running for frontend work.
