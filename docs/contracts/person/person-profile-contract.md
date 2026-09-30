# Person profile contract (V38)

`/api/v1/persons` remains a current-player API. The server derives the owner from authentication; clients must not send `ownerPlayerId` or `linkedUserId`. A Person can exist without an app user or a RoleRelation. The owner alone can read or change this private record. Existing `displayName`, `birthday`, `contact`, and `notes` stay at the top level with their existing update behavior; `displayName` is required.

## Requests and response

Create: `POST /api/v1/persons` (201). Existing bodies still work. Missing or null `profile` creates an empty profile.

```json
{
  "displayName": "Alex",
  "birthday": "2000-01-02",
  "contact": "alex@example.invalid",
  "notes": "Met at a class",
  "profile": {
    "nickname": "Al", "gender": "self-described",
    "ageAtReference": 26, "ageReferenceDate": "2026-09-30",
    "occupation": "Designer", "organization": "Studio", "area": "Seoul", "mbti": "INFP",
    "contactChannels": [{"kind": "MESSENGER", "label": "chat", "value": "alex-id"}],
    "hobbies": ["walking"], "interests": ["art"],
    "favoriteFoods": ["noodles"], "avoidedFoods": [],
    "favoriteAnimals": ["cats"], "avoidedAnimals": [],
    "favoriteMusic": ["jazz"], "favoriteMedia": ["novels"],
    "favoriteActivities": ["cafes"], "conversationTopics": ["books"],
    "avoidTopics": [], "giftIdeas": ["sketchbook"],
    "firstMetOn": "2026-05-01", "metContext": "Art class",
    "lastContactOn": "2026-09-20", "conversationNotes": "Ask about exhibition",
    "importantDates": [{"label": "Exhibition", "date": "2026-10-12", "repeatYearly": false}],
    "customNotes": [{"label": "Favorite color", "value": "Blue"}]
  }
}
```

`GET /api/v1/persons/{id}` and `GET /api/v1/persons` return the existing detail fields plus `profile`. The list remains a list of Detail objects. An old row or a profile with omitted values returns an object with nullable scalar values and empty arrays. Example detail result (inside the existing `{ "isSuccess": true, "code": "COMMON-200", "message": "...", "result": ... }` envelope):

```json
{"id":1,"linkedUserId":null,"displayName":"Alex","birthday":"2000-01-02","contact":"alex@example.invalid","notes":"Met at a class","status":"ACTIVE","createdAt":"2026-09-30T00:00:00Z","updatedAt":"2026-09-30T00:00:00Z","version":0,"profile":{"nickname":"Al","gender":"self-described","ageAtReference":26,"ageReferenceDate":"2026-09-30","hobbies":["walking"],"contactChannels":[{"kind":"MESSENGER","label":"chat","value":"alex-id"}],"importantDates":[{"label":"Exhibition","date":"2026-10-12","repeatYearly":false}],"customNotes":[{"label":"Favorite color","value":"Blue"}]}}
```

The example profile above is abbreviated. Real responses include every profile array as `[]` when empty; unused scalar fields may be absent from JSON when null.

Update: `PUT /api/v1/persons/{id}` (200). Top-level fields keep their existing full-update behavior. For `profile`:

| Update body | Effect |
| --- | --- |
| omit `profile` | Preserve all saved profile fields (legacy client) |
| `"profile": null` | Clear all profile fields |
| `"profile": { ... }` | Replace the entire profile; omitted inner fields become null or `[]` |

For a one-field edit, merge the edited field into the *latest full profile* from GET and send that full object. A stale save response must not overwrite a newer local selection. Read an absent `profile` from an older API as empty, but do not claim extended data was saved until the deployed API returns it. Do not render contact or SNS text as HTML.

## Validation

| Field | Limit |
| --- | --- |
| `nickname` | 80 characters |
| `gender` | Free text, 40 characters; never required or inferred |
| `occupation`, `organization`, `area` | 120 characters each |
| `mbti` | 16 characters |
| `ageAtReference`, `ageReferenceDate` | Both present or both absent; integer 0–150 and ISO date |
| Tag arrays: `hobbies`, `interests`, `favoriteFoods`, `avoidedFoods`, `favoriteAnimals`, `avoidedAnimals`, `favoriteMusic`, `favoriteMedia`, `favoriteActivities`, `conversationTopics`, `avoidTopics`, `giftIdeas` | Up to 20 entries each, 100 characters per entry; trim, remove blank and exact duplicates |
| `contactChannels` | Up to 10; `kind`: `PHONE`, `EMAIL`, `MESSENGER`, `SOCIAL`, `OTHER`; optional `label` 40, required `value` 200 |
| `metContext`, `conversationNotes` | 2,000 characters each |
| `firstMetOn`, `lastContactOn` | Optional ISO dates |
| `importantDates` | Up to 20; required `label` 80, ISO `date`, boolean `repeatYearly` |
| `customNotes` | Up to 20; required `label` 80 and `value` 1,000 |
| Whole normalized profile | At most 65,536 UTF-8 JSON bytes; excess gets 400 `PER-400-INVALID-PERSON-PROFILE` |

Blank optional strings become null. Birthdays remain independent of recorded age: display the current age calculated from `birthday` when known, and label `ageAtReference` as age *on `ageReferenceDate`*. If the user enters only an age, the FE supplies today's visible reference date. Never infer a birth year from age. Dates, contact history, reminders, external profile lookup, and RoleRelation notes are not automated by this contract.
