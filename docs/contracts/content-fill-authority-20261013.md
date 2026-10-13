# Content source alignment (2026-10-13)

This records the source boundary for filling the ordinary-login catalogs. The backend owns ingestion and serves the frontend through its APIs. Player-owned records, memberships and activity are separate from common definitions.

## Sources and precedence

The approved delivery archive is `LifeAsGame_ContentSystem_Approved_Payload_Delivery_v1_Result.zip` (SHA-256 `c150bbcbe1633ec27bd25b55d7eeda8cd5b56c05961aaa09ac7f67ad03788a3c`). `sources/content_2b_final/_MANIFEST.md` identifies Content 2B as FINAL CORRECTED (2026-07-23). The later `AUTH-CONT-001` runtime activation manifest is version `v1.0.0` (2026-09-05), SHA-256 `6bae6f10e182d577622d04c44e578601c9009c20fc0063a244af11c6af43443a`. Its `active=true`, `gated=false`, `deferred=false`, `lifecycleStatus=ACTIVE` tuple takes precedence over a 2B row's `defaultActive` field. `01_ACTIVE_SOURCE_INDEX.csv` (SHA-256 `fde37e4796459b9a89042e680d4f7ebfa7cef292c7ce2126a7757a54f48bb41e`) maps 110 active rows to 11 delivered files; all 11 source hashes matched.

| Family | Delivered catalog rows | Authority-active stable codes | Local alignment |
| --- | ---: | --- | --- |
| Quest | 131 | `Q_RECORD_FIRST_TRACE`, `Q_RECORD_THREE_TRACES`, `Q_RECORD_WEEKLY_LOOKBACK`, `Q_GROWTH_ONE_FOCUS`, `Q_RECOVERY_REST_TEN` | All 5 code/version/name/reward/target values matched. 126 source rows remain gated or deferred. |
| QuestRoute / Steps | 8 / 30 | `ROUTE_RECORD_START` and its `RS_RECORD_01_LEAVE_TRACE`, `RS_RECORD_02_CONNECT_TRACES`, `RS_RECORD_03_LOOK_BACK` | One Route and three Steps matched; the three required Quest links exist. Seven Routes and 27 Steps remain excluded. Quest completion only satisfies criteria; player advance is explicit. |
| Reward Profile | 3 unique (4 CSV lines) | `RP_NONE`, `RP_EXP_TINY_10`, `RP_EXP_AND_ITEM_FIRST_STEP_20` | All matched. `RP_NONE` has zero lines; the others use EXP 10 and EXP 20 plus one `IT_FIRST_STEP_FRAGMENT`. Reimport must not grant a reward. |
| Item | 12 | `IT_FIRST_STEP_FRAGMENT` | Code/name/stack matched. Eleven catalog rows are gated or deferred; no ability, price, Shop or equip behavior is inferred. |
| Equipment Slot | 17 | 10 active metadata codes in `AUTH-EQP-001` / `AUTH-CONT-001` | All 17 slot definitions match their names, versions, enabled flags and lifecycle states; seven are gated. Active slot metadata does not authorize equipment command or Item content. |

The 2B `defaultActive` fields mark 15 Quests and three Items, whereas the later activation manifest authorizes five and one. The manifest controls runtime activation. The separate `LifeAsGame_Backend_Journey_Source_20261004.json` excerpts a seven-step backend journey; it is not a grant to make that journey common content. Its Route, seven Steps and seven Quest names already exist in the local DB. They must be distinguished from the five authority-active core Quests and representative Route.

## Local verification and outstanding source

On the isolated ordinary-login preview at schema V51, authenticated API reads returned 13 Quest blueprints, two Routes, two Items, 19 Hobbies, five Achievements, two Titles, zero command-compatible equipment slots and nine read-only player slots. Existing rows outside the authority-active set are preserved as local content; the larger API count does not extend the activation grant. The read-only alignment check found zero new, changed or erroneous active definitions. No data mutation or new migration was required for these delivered active rows. The previously verified first-record → Quest → EXP/Item Mailbox Claim flow remains the representative reward path.

The delivered ZIP does not include `Content2B_Achievement_Catalog.csv`, `Content2B_Title_Catalog.csv`, `Content2B_Equipment_Item_Catalog.csv`, or `Content2B_Game_Equipment_Catalog.csv`, although the 2B manifest lists them. An approved complete Hobby definition source and approved Guild, Party, Role, Person and Activity example payload were also not found. Until those originals and their activation/ownership/entitlement policies are supplied, keep existing rows distinguishable and do not fabricate descriptions, unlocks, abilities, prices, memberships or player records. Guild/Party/Role/Person/Activity examples belong to explicit ordinary test accounts, not the common catalog or automatic signup state.
