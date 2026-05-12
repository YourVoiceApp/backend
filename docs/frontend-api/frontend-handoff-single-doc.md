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
| 방 목록 | **`GET /room/discover`** — 전체 방 페이지네이션, **`passwordProtected`**, **`activeMemberCount`**. |
| **방 유형** | **`joinPolicy`** 는 **`PUBLIC`(공개방)** / **`PASSWORD_PROTECTED`(비밀번호방)** 만. **초대 코드(inviteCode)는 API에서 제거** — 입장은 항상 **`roomId`** 기준. |
| 방 입장 | **`POST /room/join`** — 바디 **`{ "roomId": number, "password"?: string }`**. 공개 방은 `password` 없음 / 비밀번호 방은 `password` 필수. |
| 방 응답 | **`RoomResponse`** 에 **`inviteCode` 필드 없음** (신규 방은 DB에도 초대 코드 미사용). |
| 에러 코드 | **`VOICE_SHARE_DOWNLOAD_NOT_ALLOWED`** (`403`) — 클레임 시 다운로드 미허용 공유. |

---

## 방 입장 모델 변경 — 통합 정리

**초대 코드(inviteCode) 기반 입장을 없애고**, 방 종류를 **공개방(`PUBLIC`)** 과 **비밀번호방(`PASSWORD_PROTECTED`)** 두 가지만 두었습니다. 프론트는 디스커버·목록·딥링크 등에서 얻은 **`roomId`로만 입장**하면 됩니다.

### 이전 vs 이후

| 항목 | 이전 | 이후 |
|------|------|------|
| 방 종류 (`joinPolicy`) | `INVITE_CODE_ONLY`, `INVITE_CODE_WITH_PASSWORD` | **`PUBLIC`**, **`PASSWORD_PROTECTED`** |
| 입장 식별자 | 초대 코드(+ 선택 비밀번호) | **`roomId`** (+ 비밀번호방만 **`password`**) |
| 방 조회 응답 | `inviteCode` 포함 가능 | **`inviteCode` 필드 없음** (`RoomResponse`) |
| 생성 규칙 | 정책별 초대 코드 발급 등 | 공개방은 **`password` 금지**, 비밀번호방은 **`password` 필수** |

### 제거·폐기된 계약

- **`RoomJoinRequest`**: `inviteCode` 제거 → **`roomId`(필수), `password`(선택)** 만 허용.
- **`RoomResponse`**: `inviteCode` 제거.
- **`RoomRepository`**: 초대 코드로 방 조회(`findByInviteCode` 등) 제거 — 입장 경로는 **`roomId`** 단일.
- 디스커버 응답 **`RoomBrowseResponse`**: UI 편의용 **`passwordProtected`** (`joinPolicy === PASSWORD_PROTECTED`) 유지.

### 서버 동작 요약

- **`POST /room/join`**: 방을 **`request.roomId`** 로만 조회 후, 아직 멤버가 아니면 **`joinPolicy`에 따라 비밀번호 검증**. 공개방에 비밀번호가 오면 `400`.
- **이미 멤버인 경우**: 재입장 시 **`password` 검증 없이** `200` + `RoomResponse`(멤버십 상태 정상화 등 기존 로직).
- **`PUT /room/{roomId}`**: **`PUBLIC`으로 바꿀 때** 저장된 비밀번호 해시 제거.

### 프론트엔드 마이그레이션 체크리스트

1. 입장 화면 요청 본문을 **`{ roomId, password? }`** 로 교체하고, 초대 코드 입력 UI·검증 제거.
2. 방 카드/상세 모델에서 **`inviteCode` 표시·복사·공유** 제거; 공유는 **`roomId`(및 필요 시 비밀번호 안내)** 기준으로 재설계.
3. 생성·수정 폼: **`joinPolicy`** 값을 **`PUBLIC` / `PASSWORD_PROTECTED`** 만 선택 가능하게 변경.
4. API 타입·목업에서 **`inviteCode`** 필드 제거.
5. 에러 처리: 공개방에 비밀번호 전송, 비밀번호방 미입력/오류는 **`INVALID_REQUEST`** 등 기존 코드 활용.

### 운영 DB

기존 행에 `INVITE_CODE_*` 가 남아 있으면 JPA enum 매핑이 깨질 수 있습니다. 아래 **`기존 DB 마이그레이션`** 절의 SQL로 `join_policy` 문자열을 치환하세요. 레거시 `invite_code` 컬럼은 신규 방에서 **null** 이면 되며, 스키마가 NOT NULL이면 해당 절의 `ALTER` 참고.

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

- **`PUBLIC`** — 누구나 **`roomId`** 로 입장. 비밀번호 없음.
- **`PASSWORD_PROTECTED`** — **`roomId` + 올바른 `password`** 필요.

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

**공개 방:**

```json
{
  "title": "오픈 채널",
  "joinPolicy": "PUBLIC",
  "maxParticipants": 50,
  "password": null
}
```

공개 방에는 **`password` 를 보내면 안 됩니다**(보내면 `400`).

**비밀번호 방:**

```json
{
  "title": "가족 방",
  "joinPolicy": "PASSWORD_PROTECTED",
  "maxParticipants": 10,
  "password": "1234"
}
```

비밀번호 방은 **`password` 필수.**

### 내 참여 방 목록 `GET /room`

**`RoomResponse[]`**: `id`, `ownerId`, `name`, `joinPolicy`, `maxParticipants`, `createdAt`, `updatedAt`. (**`inviteCode` 없음.**)

---

### 전체 방 목록(발견) `GET /room/discover`

모든 방을 **`createdAt` 내림차순** 페이지로 조회.

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
      "name": "가족 방",
      "joinPolicy": "PASSWORD_PROTECTED",
      "passwordProtected": true,
      "activeMemberCount": 3,
      "maxParticipants": 10,
      "createdAt": "2026-05-12T08:00:00"
    },
    {
      "id": 7,
      "name": "오픈 모임",
      "joinPolicy": "PUBLIC",
      "passwordProtected": false,
      "activeMemberCount": 1,
      "maxParticipants": 50,
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

- **`passwordProtected`**: `joinPolicy === PASSWORD_PROTECTED` 와 동일(UI 자물쇠 등).

---

### 입장 `POST /room/join`

바디 **`RoomJoinRequest`**:

```json
{
  "roomId": 6,
  "password": "1234"
}
```

| 유형 | 바디 |
|------|------|
| **공개 방** | `{ "roomId": 7 }` — **`password` 생략 또는 null**. 비밀번호를 보내면 `400`. |
| **비밀번호 방** | `{ "roomId": 6, "password": "1234" }` — **`password` 필수**. |

`roomId` 는 **`@NotNull`** (필수).

이미 해당 방 **멤버**이면 비밀번호 검증 없이 **`200`** 으로 기존과 동일 **`RoomResponse`** 반환.

#### 오류 예시

| 상황 | `code` |
|------|--------|
| 공개 방인데 `password` 있음 | `INVALID_REQUEST` |
| 비밀번호 방인데 `password` 없음/틀림 | `INVALID_REQUEST` |
| 방 없음 | `ROOM_NOT_FOUND` |
| 정원 초과 | `INVALID_REQUEST` |
| 차단 유저 | `INVALID_REQUEST` |

성공 **`200`** — **`RoomResponse`**.

---

### 상세 · 멤버 · 수정 · 삭제

- **`GET /room/{roomId}`** — 참여 멤버만.
- **`GET /room/{roomId}/members`** — 항목: `id`(유저), `displayName`, `role`.
- **`PUT /room/{roomId}`** — 생성과 같은 필드 타입(`joinPolicy` 는 `PUBLIC` / `PASSWORD_PROTECTED`). 공개로 바꾸면 저장 비밀번호 해시 제거.
- **`DELETE /room/{roomId}`** — **204**.

---

### 방 발견 UI 플로우 권장

1. **`GET /room/discover`** 로 카드 리스트.
2. 항목 선택 후 **`POST /room/join`**: 공개면 `roomId` 만, 비밀번호 방이면 비밀번호 입력 후 `roomId` + `password`.

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

## 기존 DB 마이그레이션 (운영·스테이징)

`join_policy` 컬럼 문자열을 교체해야 하는 경우 예시(SQL은 DB 제품에 맞게 조정):

```sql
UPDATE voice_room SET join_policy = 'PUBLIC' WHERE join_policy = 'INVITE_CODE_ONLY';
UPDATE voice_room SET join_policy = 'PASSWORD_PROTECTED' WHERE join_policy = 'INVITE_CODE_WITH_PASSWORD';
```

초대 코드 컬럼은 레거시일 수 있음. 신규 방은 **null** 허용이 필요하면:

```sql
ALTER TABLE voice_room MODIFY COLUMN invite_code INT NULL;
```

(JPA `ddl-auto: update` 사용 시 환경에 따라 자동 반영되기도 함.)

---

*문서 끝 — 동일 내용을 여러 파일로 나눈 다른 초안은 참고용이며, 인수인계는 본 파일만 사용하면 됩니다.*
