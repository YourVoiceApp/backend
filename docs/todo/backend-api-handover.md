# Backend API Handover

프론트 연동 기준으로, 백엔드에 추가 요청하거나 계약 보완이 필요한 내용을 정리한 문서입니다.

## 현재 기준

- 인증 / 내 정보 API: 연동 완료
- 음성 폴더 / 음성 목록 / 폴더 이동 / 클론 보이스 업로드: 연동 완료
- 광고 제거 결제 API: 연동 완료
- 방 목록 / 생성 / 상세 / 공유 음성 목록 / 공유 음성 반영 일부: 연동 시작

## P0. 바로 필요한 항목

### 1. 방 입장 API (`roomId` 기준)

프론트 `RoomJoinPage` 등은 **`roomId`**로 입장합니다(디스커버·딥링크·공유에서 `roomId` 전달). **`inviteCode` 계약은 폐기.**

권장 예시:

- `POST /room/join`

Request:

```json
{
  "roomId": 1,
  "password": "1234"
}
```

공개 방은 `password` 생략 가능:

```json
{
  "roomId": 1
}
```

Response:

```json
{
  "id": 1,
  "ownerId": 1,
  "name": "우리 가족 방",
  "joinPolicy": "PASSWORD_PROTECTED",
  "maxParticipants": 3,
  "createdAt": "2026-04-17T13:40:00",
  "updatedAt": "2026-04-17T13:40:00"
}
```

참고:

- `GET /room` 은 "내가 참여 중인 방" 기준(백엔드 동작은 항목 8 참고). 초대 코드 없이 **`roomId`만으로** 입장 플로우를 연결하면 됨.

### 2. 방 상세 응답에 멤버 목록 제공 또는 멤버 조회 API 추가

현재 `RoomDetailPage`는 멤버 목록을 보여주고 있습니다.  
하지만 문서의 `GET /room/{roomId}` 응답에는 멤버 정보가 없습니다.

두 가지 중 하나가 필요합니다.

방법 A:

- `GET /room/{roomId}` 응답에 `members` 포함

예시:

```json
{
  "id": 1,
  "ownerId": 1,
  "name": "우리 가족 방",
  "joinPolicy": "PUBLIC",
  "maxParticipants": 3,
  "members": [
    {
      "id": 1,
      "displayName": "나",
      "role": "OWNER"
    },
    {
      "id": 2,
      "displayName": "엄마",
      "role": "MEMBER"
    }
  ]
}
```

방법 B:

- `GET /room/{roomId}/members` 별도 API 추가

### 3. 공유 음성 응답에 소유자 표시용 필드 추가

현재 `voice-shares` 응답에는 `voiceTitle`, `accessScope`는 있지만, 프론트 UI에서 표시하는 `ownerName`이 없습니다.

현재 응답:

```json
{
  "id": 1,
  "roomId": 1,
  "voiceKey": "voice_abc",
  "externalVoiceId": "voice_abc",
  "voiceTitle": "엄마.안내_01.m4a",
  "accessScope": "LISTEN_ONLY",
  "sharedAt": "2026-04-17T13:40:00"
}
```

권장 추가 필드:

```json
{
  "ownerName": "엄마"
}
```

필요 이유:

- 공유 음성 목록 카드에서 "누가 공유한 음성인지" 표시해야 함

### 4. 음성 목록 응답에 `ownershipId` 추가

현재 문서에도 적혀 있듯 TTS는 아래 API를 사용합니다.

- `POST /voices/{ownershipId}/text-to-speech`

그런데 `GET /voices`, `GET /voices/unassigned`, `GET /voice-folders/contents` 응답에는 `ownershipId`가 없습니다.

현재 응답 예시:

```json
{
  "voiceKey": "voice_abc",
  "title": "나의 목소리",
  "folderId": 1,
  "acquiredBy": "CREATED",
  "acquiredAt": "2026-04-17T13:40:00"
}
```

권장:

```json
{
  "ownershipId": 10
}
```

필요 이유:

- 프론트의 `VoiceListenPage`를 실제 서버 TTS로 교체하려면 필수

## P1. 있으면 바로 연결 가능한 항목

### 5. 음성 삭제 API

현재 프론트 UI에는 음성 삭제 액션이 있습니다.  
하지만 문서 기준으로는 음성 삭제 API가 없습니다.

권장 예시:

- `DELETE /voices/{ownershipId}`

또는

- `DELETE /voices/{voiceKey}`

중 하나로 명확히 통일 필요

추가로 확인할 점:

- 삭제 대상이 "내가 만든 음성"만인지
- 공유받은 음성 / 구매 음성도 라이브러리에서 제거 가능한지

### 6. 공유 음성 접근 범위 수정 UI용 계약 확정

문서에는 아래 API가 있습니다.

- `PUT /room/{roomId}/voice-shares/{shareId}`

프론트는 아직 접근 범위 수정 UI를 붙이지 않았지만, 향후 쉽게 붙일 수 있도록 아래 enum 유지가 필요합니다.

- `LISTEN_ONLY`
- `DOWNLOAD_ALLOWED`

## P2. 마켓 기능 확장 시 필요한 항목

### 7. 마켓플레이스 API 명세 추가

현재 `docs/frontend-api/market-screen.md` 기준으로는 광고 제거 결제 API만 있습니다.  
마켓 본체는 아직 데모 상태라 실제 기능을 위해서는 최소 아래가 필요합니다.

- 마켓 목록 조회
- 마켓 상세 조회
- 판매 등록
- 내 판매 목록
- 구매
- 구매 후 내 음성함 반영

예시 후보:

- `GET /market/listings`
- `GET /market/listings/{listingId}`
- `POST /market/listings`
- `GET /market/my-listings`
- `POST /market/listings/{listingId}/purchase`

## 계약상 확인이 필요한 항목

### 8. `GET /room` 의 의미

현재 문서에는 "참여 중인 방 목록 조회"라고 되어 있지만, 설명 문구는 "내가 만든 방 기준"처럼 보입니다.

백엔드 확인 필요:

- 정말 "내가 만든 방"만 내려주는지
- 아니면 "내가 참여한 방 전체"를 내려주는지

프론트 입장에서는 가능하면 "내가 참여 중인 방 전체"가 더 자연스럽습니다.

### 9. `externalVoiceIds` 와 프론트 voice id 매핑

현재 프론트는 `voiceKey` / `externalVoiceId`를 동일한 식별자로 취급해 방 공유와 폴더 이동에 사용합니다.

확인 필요:

- `PATCH /voices/folder`
- `POST /room/{roomId}/voice-shares`

이 두 API에서 요구하는 값이 항상 `voiceKey == externalVoiceId`인지

문서상 현재는 같다고 되어 있지만, 이후 분리될 가능성이 있으면 응답 계약에 명확히 적어둘 필요가 있습니다.

## 백엔드 전달 권장 순서

1. 방 입장 API (`roomId` / 선택적 `password`) — 계약 반영 및 프론트 마이그레이션
2. 방 멤버 목록 또는 상세 응답 보강
3. 공유 음성 ownerName
4. 음성 목록에 ownershipId 추가
5. 음성 삭제 API
6. 마켓플레이스 API 설계
