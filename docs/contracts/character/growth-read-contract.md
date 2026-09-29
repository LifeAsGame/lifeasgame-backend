# Current Player growth read contract

`GET /api/v1/players/growth` reads the authenticated current Player. It accepts no
player identity parameter and retains the existing `ApiResponse` envelope and
`recentExpChanges` contract (at most 20 entries, newest timestamp and ID first).
Unauthenticated requests remain 401. The query remains read-only.

## Current state

Existing `level`, `exp`, core stats, `extraStats` and `representativeTitleId`
retain their meaning. In particular, `exp` is **cumulative applied experience**,
not experience within the current level or the remaining amount to the next one.

The following fields are added to `result.current`:

| Field | JSON / OpenAPI type | Meaning |
|---|---|---|
| `expIntoLevel` | integer / int64 | Experience gained within the current level. |
| `capForLevel` | integer / int64 | Total experience needed to complete the current level. |
| `expToNext` | integer / int64 | Experience still needed to reach the next level. |
| `progressRatio` | number / double | Progress from 0 to 1. |
| `maxLevelReached` | boolean | Stored current level is at least the configured policy maximum. |

Use **`expIntoLevel / capForLevel`** for the main progress display.
`expToNext` is a separate remaining amount, never the denominator.

The application read model delegates to `LevelingPolicy.progressOf(exp, level)`
using the same configured policy bean as experience grants. The web mapper only
copies values. This does not recalculate the stored level, grant experience,
change the level curve, or persist a progress snapshot.

At an exact level boundary, the new level starts with `expIntoLevel = 0`,
`expToNext = capForLevel` and `progressRatio = 0.0`, unless it is the maximum.
At maximum level, the existing policy returns `expIntoLevel = 0`,
`capForLevel = 0`, `expToNext = 0` and `progressRatio = 1.0`. These zeros indicate
that there is no next level; they do not reset cumulative `exp`.
`maxLevelReached = true` explicitly identifies this state. Display maximum level
and do not divide by the zero cap. The boolean is based on level and policy
maximum, not inferred from a ratio of 1 or a zero cap.

## Synthetic response example

This is a synthetic example, not a response from an existing account or deployment.
With the current default curve, level 1 requires 100 experience and level 2 requires
135. At cumulative experience 130 the response is:

```json
{
  "isSuccess": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "result": {
    "current": {
      "level": 2,
      "exp": 130,
      "str": 1,
      "agi": 1,
      "dex": 1,
      "intel": 1,
      "vit": 1,
      "luc": 1,
      "extraStats": {},
      "representativeTitleId": null,
      "expIntoLevel": 30,
      "capForLevel": 135,
      "expToNext": 105,
      "progressRatio": 0.2222222222222222,
      "maxLevelReached": false
    },
    "recentExpChanges": []
  }
}
```

Configured policy values are authoritative; clients must not hardcode this curve.
The example's empty history does not assert that applied experience is reconstructed
from history. Current state is read from Player independently of the bounded history.
