# 프론트 연동 통합 명세 — 방 · 공유 음성 · 라이브러리 · TTS

> **프론트 인계 시에는 [frontend-handoff-single-doc.md](./frontend-handoff-single-doc.md) 한 파일만 넘기는 것을 권장합니다.** 아래 내용은 동일 주제의 보관본입니다.

이 파일 하나를 **방(함께 탭)과 음성 탭을 함께 연동할 때의 기준 문서**로 쓰면 됩니다.

---

## 공통

- 모든 요청에 **`Authorization: Bearer <access_token>`** 필요.
- JSON 바디는 **표준 JSON만** 허용합니다. 객체/배열 **마지막 요소 뒤 쉼표(trailing comma)** 가 있으면 파싱 실패로 **`400`** (`code`: `INVALID_REQUEST`, 메시지는 종종 `"Invalid request"`만 반환될 수 있음).

---

## 빠른 참조 — 엔드포인트 목록

### 방 (함께)

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

### 음성 라이브러리 · 폴더 · TTS

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

## Enum 값

### 방 `joinPolicy`

- **`PUBLIC`** — 공개 방 (`roomId`만으로 입장)
- **`PASSWORD_PROTECTED`** — 비밀번호 방 (`roomId` + `password`)

초대 코드(`inviteCode`)는 사용하지 않습니다. **`RoomResponse`에도 `inviteCode` 필드 없음.**

### 방 공유 `accessScope`

- `LISTEN_ONLY` — 방 목록/표시용. **클레임(`POST .../claim`) 불가.**
- `DOWNLOAD_ALLOWED` — UI상 다운로드 허용. 서버에서는 **내 라이브러리에 소유권 추가(클레임)** 에 해당. 바이너리 파일 다운로드 API는 아님.

### 방 멤버 `role`

- `OWNER`
- `MEMBER`

### 방 멤버십 `MembershipStatus` (서버)

- **`ACTIVE`**, **`BLOCKED`** 만 사용 (비활성·초대만 등 상태 구분 없음).

### 내 음성 `acquiredBy`

- `CREATED` — 직접 클론 생성
- `ROOM_SHARED` — 방 공유를 **클레임**해 라이브러리에 추가
- `ADMIN_GRANTED` — 예약. **`PATCH /voices/{ownershipId}` 이름 변경은 현재 불가**

---

## 배경 개념

### `VoiceAsset` vs `VoiceOwnership`

- **`VoiceAsset`**: 외부 보이스 ID(`voiceKey` / `externalVoiceId`)에 해당하는 학습된 보이스 **실체**. 공유 시 여러 사용자가 같은 자산을 참조할 수 있음.
- **`VoiceOwnership`**: 사용자별 **내 라이브러리 한 줄**. TTS 등은 **`ownershipId`** 로 호출.

라이브러리에서 보이는 이름은 **`VoiceOwnership.displayTitle`** (선택). 비어 있으면 **`VoiceAsset`** 의 기본 제목을 씀.

### 방 공유 응답의 `voiceTitle`

공유 건마다 방 안에서 쓸 **`shareDisplayTitle`** 이 있으면 그 값이 우선이고, 없으면 자산 기본 제목 → 응답 필드 **`voiceTitle`** 에 반영됨.

---

## 방 일반 API (요약)

### 생성 `POST /room`

요청 필드 **`title`**, 응답 필드 **`name`** (동일 의미, 이름만 다름).

```json
{
  "title": "우리 가족 방",
  "joinPolicy": "PUBLIC",
  "maxParticipants": 3,
  "password": null
}
```

비밀번호 방: `"joinPolicy": "PASSWORD_PROTECTED"`, `"password": "1234"`.

### 전체 방 목록(발견) `GET /room/discover`

페이지네이션으로 **모든 방**을 조회합니다. (**초대 코드 필드 없음** — 공개/비밀번호 유형만 `joinPolicy`로 표현.)

쿼리: `page`(기본 0), `size`(기본 20, 최대 50). 정렬은 생성일 내림차순 기준.

응답: `RoomBrowsePageResponse` — `content[]`에 `id`, `name`, `joinPolicy`, **`passwordProtected`**, `activeMemberCount`, `maxParticipants`, `createdAt` 및 페이지 메타(`totalElements`, `totalPages`, `page`, `size`, `first`, `last`).

상세 예시는 [frontend-handoff-single-doc.md](./frontend-handoff-single-doc.md) 참고.

### 목록 `GET /room`

참여 중인 방 배열 (`RoomResponse`: `id`, `ownerId`, `name`, `joinPolicy`, `maxParticipants`, `createdAt`, `updatedAt`).

### 입장 `POST /room/join`

**`roomId` 필수.** 공개 방은 `password` 없음, 비밀번호 방은 `password` 필수. 자세한 내용은 [frontend-handoff-single-doc.md](./frontend-handoff-single-doc.md).

### 상세 `GET /room/{roomId}` · 멤버 `GET /room/{roomId}/members`

멤버 응답 항목: `id`(유저 id), `displayName`, `role`.

### 수정 `PUT /room/{roomId}` · 삭제 `DELETE /room/{roomId}`

수정 시 요청은 생성과 유사(`title`, `joinPolicy`, `maxParticipants`, `password`). 삭제는 **204**.

---

## 방 음성 공유 API

Base: **`/room/{roomId}/voice-shares`**

방 **활성 멤버**만 호출 가능. 비멤버는 **`ROOM_NOT_FOUND`** 처리에 가깝게 동작할 수 있음.

### 공유 생성 `POST /room/{roomId}/voice-shares`

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
| `externalVoiceIds` | ✓ | 내가 소유한 음성의 외부 ID. **중복 불가.** |
| `accessScope` | ✓ | `LISTEN_ONLY` \| `DOWNLOAD_ALLOWED` |
| `shareDisplayTitlesByExternalVoiceId` | ✗ | 키는 반드시 위 목록에 있는 ID. 값은 방 전용 이름. 생략/`null`이면 자산 기본 제목. Swagger의 `additionalProp*` 는 플레이스홀더일 뿐. |

**200** — 배열, 원소 타입 **`RoomVoiceShareResponse`**:

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

(`voiceKey` 와 `externalVoiceId` 는 현재 동일 값.)

### 목록 `GET /room/{roomId}/voice-shares` · 단건 `GET /room/{roomId}/voice-shares/{shareId}`

응답은 위와 동일 스키마.

### 공유 수정 `PUT /room/{roomId}/voice-shares/{shareId}`

```json
{
  "accessScope": "DOWNLOAD_ALLOWED",
  "shareDisplayTitle": "수정된 방 이름"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `accessScope` | ✓ | |
| `shareDisplayTitle` | ✗ | **`null`/필드 생략** → 표시 이름 유지. **공백만** → 커스텀 이름 삭제. 그 외 → 새 이름 (100자 이하). |

### 표시 이름만 수정 `PATCH /room/{roomId}/voice-shares/{shareId}/display-title`

`accessScope` 불변.

```json
{
  "shareDisplayTitle": "이름만 변경"
}
```

`shareDisplayTitle` 은 **필수**(null 불가). 공백만이면 커스텀 이름 제거.

### 공유 삭제 `DELETE /room/{roomId}/voice-shares/{shareId}`

**204**

### 클레임 — 내 라이브러리에 추가 `POST /room/{roomId}/voice-shares/{shareId}/claim`

- Body 없음.
- **`accessScope === DOWNLOAD_ALLOWED`** 일 때만 성공. 아니면 **`403`** `VOICE_SHARE_DOWNLOAD_NOT_ALLOWED`.
- 같은 `VoiceAsset` 에 대해 이미 내 소유권이 있으면 **새로 만들지 않고** 기존 항목 반환 (**멱등**).

**200** — **`OwnedVoiceAssetResponse`**:

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

이후 TTS: **`POST /voices/{ownershipId}/text-to-speech`** (경로는 `voiceKey` 아님).

---

## 음성 폴더 API (요약)

- **`GET /voice-folders/contents`** — 선택 쿼리 `parentId`. 폴더 트리 + 음성 목록 혼합 응답(기존 `voice-screen` 문서와 동일 구조).
- **`POST /voice-folders`** — `{ "name": "...", "parentFolderId": null | number }`
- **`PUT /voice-folders/{folderId}`** · **`DELETE /voice-folders/{folderId}`**

---

## 내 음성 라이브러리 API

### 목록 `GET /voices`

쿼리: **`folderId`** 선택 (없으면 전체).

응답: **`OwnedVoiceAssetResponse[]`**

```json
[
  {
    "ownershipId": 10,
    "voiceKey": "voice_abc",
    "title": "엄마.안내_01.m4a",
    "folderId": 1,
    "acquiredBy": "CREATED",
    "acquiredAt": "2026-04-17T13:40:00"
  }
]
```

**`title`**: 소유권 표시 이름 우선(`displayTitle`), 없으면 자산 기본 제목.

### 미분류 `GET /voices/unassigned`

스키마 동일, `folderId` 는 `null`.

### 폴더 이동 `PATCH /voices/folder`

```json
{
  "externalVoiceIds": ["voice_abc", "voice_xyz"],
  "folderId": 1
}
```

미분류로: `"folderId": null`. `externalVoiceIds` 중복 불가. 하나라도 내 소유가 아니면 실패.

### 표시 이름 변경 `PATCH /voices/{ownershipId}`

```json
{
  "name": "내 라이브러리에서 부를 이름"
}
```

- **`acquiredBy` 가 `CREATED` 또는 `ROOM_SHARED`** 만 허용.
- 공통 검증: 이름 필수, 최대 100자.

### 클론 생성 `POST /voices/cloned-voice`

- **`multipart/form-data`**
- 필드: **`files`** (파일 1개), **`name`** (string), **`description`** (선택)
- 확장자: **`.wav`**, **`.mp3`**, 최대 **3MB**

응답 예:

```json
{
  "voiceKey": "external_voice_id_123",
  "externalVoiceId": "external_voice_id_123",
  "title": "동호 목소리"
}
```

### TTS `POST /voices/{ownershipId}/text-to-speech`

```json
{
  "text": "안녕하세요. 테스트 음성입니다."
}
```

응답 예:

```json
{
  "speechRequestId": 1,
  "generatedAudioId": 20,
  "streamUrl": "/voices/generated-audios/20/stream",
  "downloadUrl": "/voices/generated-audios/20/download"
}
```

### 생성 오디오

- **`GET /voices/generated-audios/{generatedAudioId}/stream`** — 재생
- **`GET /voices/generated-audios/{generatedAudioId}/download`** — 다운로드  
  Content-Type: **`audio/mpeg`**

### 삭제 `DELETE /voices/{ownershipId}`

해당 사용자의 **소유권 행만** 삭제 (**204**). (`ROOM_SHARED` 로 받은 항목도 동일.)

---

## 에러 코드 참고

| `code` | HTTP | 예시 |
|--------|------|------|
| `INVALID_REQUEST` | 400 | 잘못된 JSON, 검증 실패, rename 불가 타입 등 |
| `ROOM_NOT_FOUND` | 404 | 방 없음 또는 멤버 아님 등 |
| `ROOM_VOICE_SHARE_NOT_FOUND` | 404 | 잘못된 `shareId` |
| `VOICE_SHARE_DOWNLOAD_NOT_ALLOWED` | 403 | `LISTEN_ONLY` 인데 `claim` 호출 |
| `VOICE_ASSET_NOT_FOUND` | 404 | 잘못된 `ownershipId` 등 |
| `VOICE_FOLDER_NOT_FOUND` | 404 | 폴더 관련 |

---

## 권장 UX 플로우 (방 공유 → 내 음성 → TTS)

1. **`GET /room/{roomId}/voice-shares`** 로 목록 표시.
2. **`accessScope === DOWNLOAD_ALLOWED`** 일 때만 「내 음성에 추가」→ **`POST .../claim`**.
3. 받은 **`ownershipId`** 로 **`POST /voices/{ownershipId}/text-to-speech`** → **`streamUrl` / `downloadUrl`** 로 재생·저장.
4. 방에서 보이는 이름과 라이브러리 이름을 다르게 하려면 클레임 후 **`PATCH /voices/{ownershipId}`**.

---

## 레거시·보조 문서

- 방 목록(발견)·입장 전용: [frontend-handoff-single-doc.md](./frontend-handoff-single-doc.md)
- 같은 폴더의 `rooms-screen.md`, `voice-screen.md` 는 화면별 초안일 수 있습니다. **내용이 겹치면 [frontend-handoff-single-doc.md](./frontend-handoff-single-doc.md) 우선**합니다.
