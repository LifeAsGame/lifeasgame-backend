# Backend developer journey contract — DRAFT

This document records the **2026-10-04 activation decision**. The historical source revision `CONTENT_2B_FINAL_CORRECTED_2026-07-23` was GATED; this is not a retroactive approval. Source file SHA-256: `aadbccf655586dc328592b5eecca15f79dbb8448a74b5041ec21a7d7a669fa26`. Canonical row fingerprints below use UTF-8 JSON with sorted keys and compact separators; the source file is retained outside product Git.

## Product scope

`ROUTE_BACKEND_DEVELOPER_START` adds seven ordered required Quest steps. Route selection is optional and does not accept or complete Quests. The route completion reward is `RP_NONE`. Quest reward profiles in step order: `RP_NONE`, `RP_EXP_TINY_10`, `RP_EXP_TINY_10`, `RP_EXP_TINY_10`, `RP_EXP_TINY_10`, `RP_EXP_AND_ITEM_FIRST_STEP_20`, `RP_EXP_TINY_10`. Existing reward lines, Mailbox and Claim apply. Achievement, title, and item candidate codes are not automatically granted. This is a self-recorded learning and implementation journey, not professional certification.

The source's `CollectionEntryRecorded` maps to an explicit link to the existing Collection-backed LifeLog record. Current `CollectionCategory` lacks `PROJECT`; this activation adds `PROJECT` without moving any existing record. `LifeLogRecorded` already exists as a durable event, but this journey only progresses on an explicit link, including an older owned contentful record. `ExternalEvidenceLinked` has no current fact source; the explicit URL/description command stores the evidence and progresses this one Quest. No global event replay or duplicate event is needed.

Role selection uses the existing user-owned active Role's stable `roleType`, equal to `ROLE_BACKEND_DEVELOPER` or `ROLE_JOB_SEEKER`. Display name never determines eligibility. The selected Role is bound to the journey and cannot be switched afterward. A linked LifeLog with another `primaryRoleId` is rejected; an unassigned owned record is allowed.

## FE HTTP contract

Authentication supplies the Player on every operation. Existing `ApiResponse` wrappers and Quest/Route response shapes apply. IDs below are numeric and server-owned. Obtain `routeId`/`stepId` from route catalog and `roleId` from `GET /api/v1/roles`. Create a compatible Role with `POST /api/v1/roles` (`{ "roleType": "ROLE_BACKEND_DEVELOPER", "name": "My backend role", "description": null }`) if needed. For a PROJECT record, `POST /api/v1/players/collections` with `{ "category": "PROJECT", "title": "Service project", "quantity": 1, "lifeLogSubtype": "PROJECT", "primaryRoleId": 123 }`; its additive `lifeLogId` response field is the evidence ID. Existing records can be selected from `GET /api/v1/lifelogs`.

| Action | HTTP | Request | Result |
| --- | --- | --- | --- |
| Catalog | `GET /api/v1/quests/catalog`, `GET /api/v1/quest-routes` | none | Seven added Quest blueprints, one added Route |
| Select Role and Route | `POST /api/v1/quest-routes/{routeId}/select` | `{ "roleId": 123 }` | Route with `playerProgress.roleId`; same selection replays safely |
| Accept Quest | `POST /api/v1/players/quests/{questCode}` | `{ "partyId": null, "guildId": null }` | Acceptance; selected Route/Role required for seven new codes |
| Link goal memo | `PUT /api/v1/players/quests/{questCode}/evidence/memo` | `{ "memo": "Build a small service" }` | Acceptance becomes `GOAL_REACHED` for step 1 |
| Link LifeLog | `PUT /api/v1/players/quests/{questCode}/evidence/life-log` | `{ "lifeLogId": 123 }` | Acceptance becomes `GOAL_REACHED` for Java or PROJECT Collection stages |
| Link deployment | `PUT /api/v1/players/quests/{questCode}/evidence/deployment` | `{ "url": "https://example.org/demo", "description": "Service deployed" }` | Acceptance becomes `GOAL_REACHED` for step 6; no remote request |
| Unlink before completion | `DELETE /api/v1/players/quests/{questCode}/evidence` | none | Acceptance returns to `IN_PROGRESS` |
| Evidence status | `GET /api/v1/players/quests/{questCode}/evidence` | none | Evidence metadata or `null`, never full source content |
| User completion | `POST /api/v1/players/quests/{questCode}/complete` | none | `COMPLETED`; duplicate command returns same result without a second reward |
| Refresh | `GET /api/v1/players/quests/{questCode}`, `GET /api/v1/quest-routes/my/{routeId}` | none | Current acceptance and current step readiness |
| Advance | `POST /api/v1/quest-routes/my/{routeId}/advance` | `{ "expectedStepId": 123 }` | Next step or completed Route; stale ID conflicts |

Linking is limited to the selected Quest acceptance. A PROJECT Collection LifeLog may be linked to multiple Quests only through separate commands. Memo and deployment description are 1–1000 non-whitespace characters. Deployment URL is at most 2048 characters, absolute `http`/`https`, with no user-info. A repeated identical link is idempotent; a different link requires explicit unlink first. Unlink after completion conflicts. Completion retains only source kind/ID and short evidence metadata as a snapshot; later source edits/deletion neither revoke nor repay rewards.

Expected errors follow existing `QUE-*` conventions: 400 for invalid evidence/URL, 404 for missing or foreign Player records, 409 for wrong Role, missing selection, conflicting evidence, invalid state, and stale step. A failed ownership check never returns source details.

## Source row provenance

| Kind | Stable code | Historical row SHA-256 |
| --- | --- | --- |
| route | `ROUTE_BACKEND_DEVELOPER_START` | `d70348727b3feb61d7bb4d4ea1b69fabeee7cbb7998b1e304451f186a1c1031c` |
| step | `RS_DEV_01_DIRECTION` | `6ddb25d7cdca8e714a0a6ee8735c590b3be071ad9319ec2e16e2d68bea8b24d9` |
| step | `RS_DEV_02_JAVA` | `08173853a1ba944afe0e3a2ad2c28b0f318a67a2e6065bb3e4d6ee74ab05f397` |
| step | `RS_DEV_03_SPRING` | `51c96b5ace54b100f877d9a1d40412de65f68453b94d0af5ea1f034572648444` |
| step | `RS_DEV_04_DATABASE` | `19f1fd7cfbd3395fdc3051936712fb061154a4d8f248317f5fd7f91fa3ecbd79` |
| step | `RS_DEV_05_TEST` | `3944effa21a1cafe3a1a3768710746bd40e7cec861dc7855725a7163f8e58c7f` |
| step | `RS_DEV_06_DEPLOY` | `44b4db06f1ad7edd17a1cfc5f88d8a385bf1a841db459792ebf7afebf8a132ff` |
| step | `RS_DEV_07_PORTFOLIO` | `357596371b85ee6c8fa601fe32839f3bf840b8ea9c0f79677ac943f9ef64ae30` |
| quest | `Q_DEV_DEFINE_BACKEND_GOAL` | `4afbbe6623c9b4e36eb78f5354e3fed45b2ed5ae1b8dd7e60cd2fe8202f0a064` |
| quest | `Q_DEV_RECORD_JAVA_STUDY` | `00267fbaed04a999e5a03ff81e4284e57aa33cd044d8370dd6e932cda8e38472` |
| quest | `Q_DEV_BUILD_SPRING_CRUD` | `7ecc057a2cbb37204ff08bde5db9e9f7286a71ae36835410d7e6eda97558a0b1` |
| quest | `Q_DEV_MODEL_DATABASE` | `311bcdfa388370b2a9a9fcf9292a3832d984951bb4e69cf3bf8366e12de144d5` |
| quest | `Q_DEV_WRITE_DOMAIN_TEST` | `54eb49955f48dedb82375acdb25ad5fdbc3e893b1c646e7ea87094b3c658a5e9` |
| quest | `Q_DEV_DEPLOY_SERVICE` | `bb4f91d81541c61005cee9e0642e6bfb3e480c7b89b6068fed5c9ab95cbf4775` |
| quest | `Q_DEV_POLISH_README` | `90b679507ba17bd78bbc4a929c6b496ae068974503fcc7634074599a56969f60` |
