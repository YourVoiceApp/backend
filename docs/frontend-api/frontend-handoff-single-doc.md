# 프론트 인수인계 — 단일 명세 (이 파일만 전달)

방 · 입장 · 공유 음성 · 라이브러리 · TTS 연동을 **한 문서**로 정리했습니다. 다른 `docs/frontend-api/*.md` 없이 본 파일만 넘기면 됩니다.

---

## 서버/계약 변경 요약

| 구분 | 내용 |
|------|------|
| 음성 이름 | 라이브러리 표시 이름은 **`VoiceOwnership.displayTitle`**. **`PATCH /voices/{ownershipId}`** 로 바꾸며 **`VoiceAsset.title` 은 수정하지 않음**. 허용: **`CREATED`**, **`ROOM_SHARED`** (`ADMIN_GRANTED` 등 제외). |
| 방 공유 이름 | 생성 시 선택 **`shareDisplayTitlesByExternalVoiceId`** 맵. 수정 **`PUT`** 의 선택 **`shareDisplayTitle`**, 또는 **`PATCH .../display-title`**. 응답 **`voiceTitle`** 은 방 표시 이름 우선. |
| 클레임 | **`POST /room/{roomId}/voice-shares/{shareId}/claim`** — **`DOWNLOAD_ALLOWED`** 만. 내 라이브러리에 **`ROOM_SHARED`** 소유권 생성(멱등). 이후 **`POST /voices/{ownershipId}/text-to-speech`**. |
| 멤버십 상태 | **`INVITED` / `LEFT` 제거**. **`ACTIVE`**, **`BLOCKED`** 만. |
| 방 목록 | **`GET /room/discover`** — 전체 방 페이지네이션, **`passwordProtected`**, **`activeMemberCount`**, 초대 코드 **미포함**. |
| 방 입장 | **`POST /room/join`** — **`inviteCode` 와 `roomId` 동시 사용 불가**. 비밀번호 방은 **`roomId` + `password` 만**으로 입장 가능 (`INVITE_CODE_WITH_PASSWORD` 만). |
| 에러 코드 | **`VOICE_SHARE_DOWNLOAD_NOT_ALLOWED`** (`403`) — 클레임 시 다운로드 미허용 공유. |

---

## 공통

- **`Authorization: Bearer <access_token>`** 필수.
- JSON **후행 쉼표 금지** → 깨진 JSON은 **`400`**, `code`: `INVALID_REQUEST`(메시지가 짧게만 올 수 있음).

---

## 엔드포인트 빠른 목록

### 방

| Method | Path |
|--------|------|
| `POST` | `/room` |
| `GET` | `/room` |
| `GET` | `/room/discover` |
| `POST` | `/room/join` |
| `GET` | `/room/{roomId}` |
| `GET` | `/room/{roomId}/members` |
| `PUT` | `/room/{roomId}` |
| `DELETE` | `/room/{roomId}` |

### 방 음성 공유

| Method | Path |
|--------|------|
| `POST` | `/room/{roomId}/voice-shares` |
| `GET` | `/room/{roomId}/voice-shares` |
| `GET` | `/room/{roomId}/voice-shares/{shareId}` |
| `PUT` | `/room/{roomId}/voice-shares/{shareId}` |
| `PATCH` | `/room/{roomId}/voice-shares/{shareId}/display-title` |
| `POST` | `/room/{roomId}/voice-shares/{shareId}/claim` |
| `DELETE` | `/room/{roomId}/voice-shares/{shareId}` |

### 음성 폴더 · 라이브러리 · TTS

| Method | Path |
|--------|------|
| `POST` | `/voice-folders` |
| `GET` | `/voice-folders/contents` |
| `PUT` | `/voice-folders/{folderId}` |
| `DELETE` | `/voice-folders/{folderId}` |
| `GET` | `/voices` |
| `GET` | `/voices/unassigned` |
| `PATCH` | `/voices/folder` |
| `PATCH` | `/voices/{ownershipId}` |
| `POST` | `/voices/cloned-voice` |
| `DELETE` | `/voices/{ownershipId}` |
| `POST` | `/voices/{ownershipId}/text-to-speech` |
| `GET` | `/voices/generated-audios/{generatedAudioId}/stream` |
| `GET` | `/voices/generated-audios/{generatedAudioId}/download` |

---

## Enum

### `joinPolicy`

- `INVITE_CODE_ONLY`
- `INVITE_CODE_WITH_PASSWORD`

### 공유 `accessScope`

- `LISTEN_ONLY` — 클레임 불가.
- `DOWNLOAD_ALLOWED` — **`POST .../claim`** 으로 내 라이브러리 추가(파일 다운로드 API 아님).

### 멤버 `role`

- `OWNER`, `MEMBER`

### 멤버십 `MembershipStatus`

- `ACTIVE`, `BLOCKED` 만.

### 내 음성 `acquiredBy`

- `CREATED`, `ROOM_SHARED`, `ADMIN_GRANTED` (이름 변경 API는 전자 두 타입만).

---

## 배경 개념

- **`VoiceAsset`**: 외부 보이스 실체 (`voiceKey` / `externalVoiceId`).
- **`VoiceOwnership`**: 사용자별 라이브러리 한 줄. TTS는 **`ownershipId`**.
- 목록의 **`title`**: 소유권 **`displayTitle`** 우선, 없으면 자산 기본 제목.

---

## 방 API — 상세

### 생성 `POST /room`

요청 **`title`**, 응답 **`name`**.

```json
{
  "title": "우리 가족 방",
  "joinPolicy": "INVITE_CODE_ONLY",
  "maxParticipants": 3,
  "password": null
}
```

비밀번호 방: `"joinPolicy": "INVITE_CODE_WITH_PASSWORD"`, `"password": "1234"`.

### 내 참여 방 목록 `GET /room`

**`RoomResponse[]`**: `id`, `ownerId`, `name`, **`inviteCode` 포함**, `joinPolicy`, `maxParticipants`, `createdAt`, `updatedAt`.

---

### 전체 방 목록(발견) `GET /room/discover`

모든 방을 **`createdAt` 내림차순** 페이지로 조회. **`inviteCode` 없음.**

**쿼리:** `page`(기본 0), `size`(기본 20, 서버 최대 **50**).

```http
GET /room/discover?page=0&size=20
```

**응답 `200` — `RoomBrowsePageResponse`:**

```json
{
  "content": [
    {
      "id": 6,
      "name": "우리 가족 방",
      "joinPolicy": "INVITE_CODE_WITH_PASSWORD",
      "passwordProtected": true,
      "activeMemberCount": 3,
      "maxParticipants": 10,
      "createdAt": "2026-05-12T08:00:00"
    },
    {
      "id": 7,
      "name": "공개 모임",
      "joinPolicy": "INVITE_CODE_ONLY",
      "passwordProtected": false,
      "activeMemberCount": 1,
      "maxParticipants": 5,
      "createdAt": "2026-05-11T12:00:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

- **`passwordProtected`**: `joinPolicy === INVITE_CODE_WITH_PASSWORD` 와 동일(UI 자물쇠 등).
- **`activeMemberCount`**: ACTIVE 멤버 수.

---

### 입장 `POST /room/join`

바디 **`RoomJoinRequest`**: `inviteCode`, `roomId`, `password` — **`inviteCode` 와 `roomId` 중 하나만** 사용.

#### 방식 A — 초대 코드

```json
{
  "inviteCode": "720341",
  "password": null
}
```

비밀번호 방:

```json
{
  "inviteCode": "720341",
  "password": "1234"
}
```

`inviteCode`: 문자열 **6자리 숫자**.

#### 방식 B — 비밀번호만 (`INVITE_CODE_WITH_PASSWORD` 만)

목록에서 받은 **`id`** 사용:

```json
{
  "roomId": 6,
  "password": "1234"
}
```

`inviteCode` 는 null 또는 생략. **`INVITE_CODE_ONLY`** 방은 **`roomId` 입장 불가** → `400`.

#### 규칙·오류

| 상황 | `code` |
|------|--------|
| 둘 다 없음 / 둘 다 있음 | `INVALID_REQUEST` |
| 초대 코드 형식 오류 | `INVALID_REQUEST` |
| 방 없음 | `ROOM_NOT_FOUND` |
| 비번 없음/틀림 | `INVALID_REQUEST` |
| 초대 전용 방에 `roomId` 입장 | `INVALID_REQUEST` |
| 정원 초과 | `INVALID_REQUEST` |
| 차단 유저 | `INVALID_REQUEST` |

성공 **`200`** — **`RoomResponse`** (기존과 동일). 이미 멤버여도 **`200`**.

---

### 상세 · 멤버 · 수정 · 삭제

- **`GET /room/{roomId}`** — 참여 멤버만.
- **`GET /room/{roomId}/members`** — 항목: `id`(유저), `displayName`, `role`.
- **`PUT /room/{roomId}`** — 생성과 유형 동일 필드.
- **`DELETE /room/{roomId}`** — **204**.

---

### 방 발견 UI 플로우 권장

1. **`GET /room/discover`** 로 카드 리스트 (`passwordProtected`, `activeMemberCount` / `maxParticipants`).
2. 비밀번호 방 → **`POST /join`** `{ roomId, password }`.
3. 초대만 방 → 사용자에게 코드 입력 → **`POST /join`** `{ inviteCode }` (+ 필요 시 `password` 없음).

---

## 방 음성 공유 API

Base: **`/room/{roomId}/voice-shares`** (방 활성 멤버).

### 생성 `POST /room/{roomId}/voice-shares`

```json
{
  "externalVoiceIds": ["p5nUiHWv33MTSMZZ6bffFc"],
  "accessScope": "DOWNLOAD_ALLOWED",
  "shareDisplayTitlesByExternalVoiceId": {
    "p5nUiHWv33MTSMZZ6bffFc": "방에서 보일 이름"
  }
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `externalVoiceIds` | ✓ | 내 소유 음성 외부 ID, 중복 불가 |
| `accessScope` | ✓ | `LISTEN_ONLY` \| `DOWNLOAD_ALLOWED` |
| `shareDisplayTitlesByExternalVoiceId` | ✗ | 키는 목록에 있는 ID만. Swagger `additionalProp*` 는 플레이스홀더 |

**응답 `RoomVoiceShareResponse[]`** 예:

```json
{
  "id": 12,
  "roomId": 6,
  "voiceKey": "p5nUiHWv33MTSMZZ6bffFc",
  "externalVoiceId": "p5nUiHWv33MTSMZZ6bffFc",
  "voiceTitle": "방에서 보일 이름",
  "ownerName": "공유자닉네임",
  "accessScope": "DOWNLOAD_ALLOWED",
  "sharedAt": "2026-05-12T10:00:00"
}
```

### 목록·단건 `GET`

동일 스키마.

### 수정 `PUT /room/{roomId}/voice-shares/{shareId}`

```json
{
  "accessScope": "DOWNLOAD_ALLOWED",
  "shareDisplayTitle": "수정된 방 이름"
}
```

`shareDisplayTitle`: 생략/`null` → 이름 유지; 공백만 → 커스텀 이름 삭제.

### `PATCH .../display-title`

`accessScope` 불변. 바디 `{ "shareDisplayTitle": "..." }` — 필수 필드, 공백만이면 삭제.

### 삭제 `DELETE ...` — **204**

### 클레임 `POST .../claim`

Body 없음. **`DOWNLOAD_ALLOWED`** 만. **`403`** `VOICE_SHARE_DOWNLOAD_NOT_ALLOWED` 그 외. 멱등.

**응답 `OwnedVoiceAssetResponse`:**

```json
{
  "ownershipId": 42,
  "voiceKey": "p5nUiHWv33MTSMZZ6bffFc",
  "title": "방에서 보일 이름",
  "folderId": null,
  "acquiredBy": "ROOM_SHARED",
  "acquiredAt": "2026-05-12T11:00:00"
}
```

---

## 음성 폴더 (요약)

- **`GET /voice-folders/contents`** — 선택 `parentId`
- **`POST /voice-folders`** — `name`, `parentFolderId`
- **`PUT` / `DELETE`** 폴더

---

## 내 음성 라이브러리 API

### `GET /voices` · `GET /voices/unassigned`

선택 쿼리 `folderId`(목록만).

### `PATCH /voices/folder`

```json
{
  "externalVoiceIds": ["voice_abc"],
  "folderId": 1
}
```

### `PATCH /voices/{ownershipId}` — 표시 이름

`{ "name": "..." }` — **`CREATED` | `ROOM_SHARED`**, 최대 100자.

### `POST /voices/cloned-voice`

`multipart/form-data`: `files`, `name`, `description?` — wav/mp3, 3MB.

### `POST /voices/{ownershipId}/text-to-speech`

`{ "text": "..." }` → `streamUrl`, `downloadUrl`, `generatedAudioId`.

### 생성 오디오

`GET .../stream`, `GET .../download` — `audio/mpeg`.

### `DELETE /voices/{ownershipId}` — **204**

---

## 에러 코드

| `code` | HTTP |
|--------|------|
| `INVALID_REQUEST` | 400 |
| `ROOM_NOT_FOUND` | 404 |
| `ROOM_VOICE_SHARE_NOT_FOUND` | 404 |
| `VOICE_SHARE_DOWNLOAD_NOT_ALLOWED` | 403 |
| `VOICE_ASSET_NOT_FOUND` | 404 |
| `VOICE_FOLDER_NOT_FOUND` | 404 |

---

## 권장 UX — 공유 음성 → TTS

1. **`GET /room/{roomId}/voice-shares`**
2. **`DOWNLOAD_ALLOWED`** 만 「내 음성 추가」→ **`POST .../claim`**
3. **`POST /voices/{ownershipId}/text-to-speech`**
4. 방 표시명과 라이브러리명 분리 시 **`PATCH /voices/{ownershipId}`**

---

*문서 끝 — 동일 내용을 여러 파일로 나눈 다른 초안은 참고용이며, 인수인계는 본 파일만 사용하면 됩니다.*
