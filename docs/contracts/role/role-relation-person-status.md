# 관계 응답의 Person 상태

`GET /api/v1/roles/{roleId}/relations`와 `GET /api/v1/roles/{roleId}/relations/{relationId}`의 관계 객체에 `personStatus`가 추가된다. 같은 DTO를 사용하는 기존 POST 생성 및 PUT 수정 응답에도 포함된다.

OpenAPI 관계 객체의 스키마 이름은 `RoleRelationDetail`이다. 다른 도메인의 `Detail`과 충돌하지 않게 구분하며 POST의 성공 응답은 실제 HTTP와 같은 201로 문서화한다.

- `status`: 기존 RoleRelation 상태. 의미와 이름을 유지한다.
- `personStatus`: 연결된 소유 Person의 실제 `ACTIVE` 또는 `ARCHIVED` 상태.
- `personDisplayName`, `linkedUserId`, `relationType`, `roleNotes` 등 기존 필드도 유지한다.

정상 응답의 관계 객체 일부:

```json
{"personId": 3, "personDisplayName": "Alice", "relationType": "FRIEND", "roleNotes": "note", "status": "ACTIVE", "personStatus": "ACTIVE"}
```

같은 Person을 보관한 뒤의 관계 객체 일부:

```json
{"personId": 3, "personDisplayName": "Alice", "relationType": "FRIEND", "roleNotes": "note", "status": "ACTIVE", "personStatus": "ARCHIVED"}
```

Person 보관은 연결된 관계를 삭제하거나 보관하지 않는다. 다른 Role의 연결도 유지되며, 프론트는 `personStatus`로 보관된 인물을 표시한다. 목록은 기존대로 ACTIVE 관계만 반환하고 상세는 보관된 관계도 기존 조회 계약에 따라 반환한다. Person 보관 후에도 기존 관계 수정 응답은 실제 Person 상태를 전달한다. 새 관계 생성에 보관된 Person을 사용할 수 없다는 기존 규칙은 유지한다.

인증된 플레이어의 Role 소유권과 Person 소유권을 모두 확인한다. 비소유/없는 Person 참조는 기존 `PER-404-NOT-FOUND` 오류를 유지하며 이름이나 상태를 반환하지 않는다. 누락·미확인 상태를 ACTIVE로 추정하지 않는다. 조회는 쓰기나 이벤트를 발생시키지 않고 목록의 Person 참조는 기존 일괄 조회로 가져온다.
