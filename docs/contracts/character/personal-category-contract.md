# Personal categories — BE contract v1 (2026-10-01)

Status: implemented in the backend branch; **not deployed**. The shared `backend.json.ready` flag is unchanged. All routes below require the current authenticated player. There is no owner ID in requests. Person PR #384 is independent.

## Model

`kind` is `CERTIFICATION` or `HOBBY`. `source` is `SYSTEM` or `PERSONAL`. System entries come from the existing enums, including all 11 certification and 18 hobby codes even when the shared catalog has no rows. A system entry has `code` and `id: null`; a personal entry has numeric `id` and `code: null`. `name` for system entries is the enum code; FE may localize it. A personal category can be empty. The system category on a shared definition remains independent of the personal category on the player's owned item. A personal folder may contain items with different system categories.

Personal names have leading/trailing Unicode whitespace stripped, must contain 1–80 Unicode code points, and compare case insensitively via `Locale.ROOT` lowercase. Internal spaces are preserved. Names are unique for `(owner, kind, normalized name)`; the database enforces this against concurrent requests. Other players and kinds may use the same name.

## Routes

All responses use the existing `ApiResponse` envelope (`isSuccess`, `code`, `message`, `result`). Errors use the existing ProblemDetail envelope with the error `code`.

| Method | Path | Request | `result` |
| --- | --- | --- | --- |
| GET | `/api/v1/players/categories/{kind}` | — | array of `{id,code,name,source,kind}`; systems first, then personal by ID |
| POST | `/api/v1/players/categories/{kind}` | `{"name":"Cloud notes"}` | created personal category; 201 |
| PATCH | `/api/v1/players/categories/{kind}/{id}` | `{"name":"Study"}` | renamed personal category |
| DELETE | `/api/v1/players/categories/{kind}/{id}` | — | 204, no response body |
| PATCH | `/api/v1/players/categories/{kind}/items/{itemId}/personal-category` | `{"personalCategoryId":123}` or `{"personalCategoryId":null}` | `{itemId,personalCategoryId}` |
| GET | `/api/v1/players/categories/{kind}/{id}/items` | — | existing player certification/hobby `Infos` (`{"infos":[...]}`), filtered by personal category |
| GET | `/api/v1/players/categories/{kind}/system/{code}/items` | — | existing player certification/hobby `Infos`, filtered by definition system category |

`itemId` is the shared `certificationId` or `hobbyId` used in the existing owned item routes, **not** the ownership row ID. An assignment with `null` clears the link. The assignment request must contain `personalCategoryId`; omitted is invalid. Existing `POST /api/v1/players/certifications/{certificationId}` and `/hobbies/{hobbyId}` request bodies are unchanged: register first, then assign by PATCH. Existing owned list `Info` entries now add nullable `personalCategoryId` while keeping `category` as the original system enum code. Existing request omission semantics are unchanged.

Examples:

```json
{"isSuccess":true,"code":"COMMON-200","message":"성공입니다.","result":[{"id":null,"code":"CLOUD","name":"CLOUD","source":"SYSTEM","kind":"CERTIFICATION"},{"id":123,"code":null,"name":"Cloud notes","source":"PERSONAL","kind":"CERTIFICATION"}]}
```

```json
{"personalCategoryId":null}
```

Deleting a personal category disconnects its owned items in the same database transaction through `ON DELETE SET NULL`. Owned certification/hobby records, shared definitions, original system categories, experience and reward history remain. Assignment and deletion serialize on the category row; the FK prevents a dangling ID. System entries have no edit/delete route.

`PCA-400-INVALID-NAME` means blank/overlong name, `PCA-400-INVALID-ASSIGNMENT` means missing assignment field, `PCA-409-DUPLICATE-NAME` means normalized collision, `PCA-404-NOT-FOUND` means nonexistent, deleted, foreign-owned, or wrong-kind category, and `PCA-404-OWNED-ITEM-NOT-FOUND` means the player does not own the requested item. Invalid system codes use existing certification/hobby category errors. A shared catalog with zero items still yields system categories, but its existing empty-catalog restriction prevents registering a new owned item.

## FE interaction

Open the category list when the user opens Certifications or Hobbies. Opening the parent creation action uses POST. Selecting a system or personal row loads its owned item list; opening that list's creation action uses the existing shared catalog and owned registration route, then the assignment PATCH for personal rows. Personal rows allow rename/delete; system rows are read only. The delete confirmation should state that owned items remain and only their personal category link is removed.

## Integration

Deploy only after migration ordering is settled: this branch uses V39 because open Person PR #384 uses V38. If V39 is applied before V38, the later V38 must be renumbered or Flyway migration ordering reconciled before deployment. No existing 19080 server, database, Redis, or demo namespace was changed for this contract. For an isolated new integration run, use a separate MySQL database and Redis namespace, apply Flyway migrations with this branch, start a separate Spring profile/port, then point FE to that port. This document describes code behavior; it is not a claim of live-server readiness.
