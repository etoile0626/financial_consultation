## 절대 하면 안 되는 일

### 시크릿/민감정보 관리
- API 키, OAuth Client Secret, LLM API 키 등을 코드/커밋에 하드코딩 금지 — `.env` 또는 환경변수로 관리, `.gitignore`에 반드시 포함
- 소셜 로그인(OAuth) Access/Refresh Token을 평문으로 로그에 출력하거나 DB에 암호화 없이 저장 금지

### 개인정보(PII) 처리
- 실사용자의 이름, 계좌번호, 소득 등 개인/금융 정보를 LLM API(OpenAI, Anthropic 등)에 그대로 전송 금지 — 익명화/마스킹 후 전달
- 개인정보가 포함된 요청/응답 데이터를 로그에 그대로 남기지 않기 (에러 로그 포함)
- 테스트 코드에도 실제 개인정보 패턴(실명, 실제 형식의 주민번호/계좌번호)을 더미 데이터로 사용하지 않기

### 운영 환경 (배포 이후 적용)
- 운영(production) DB에 직접 접근해서 데이터 수정/삭제 금지 — 반드시 마이그레이션 스크립트와 코드 리뷰를 거칠 것
- `main` 브랜치에 직접 커밋/푸시 금지 (Git 컨벤션과 동일)
- 운영 환경 설정(application-prod.yml 등)의 값을 로컬 테스트용으로 임의 변경 금지

### 금융 상담 응답 관련
- LLM이 생성한 상담 결과를 사실 확인 없이 "확정된 금융 자문"처럼 제공하지 않기 — 약관 근거 명시 + 법적 자문이 아니라는 disclaimer 필수
- 예적금/대출 금리·조건 비교 시 실제 약관에 없는 수치를 추론/생성해서 보여주지 않기 (환각 방지)

## 패키지 구조

- 도메인 기반으로 디렉토리를 나누며, 패키지명은 소문자 사용
- `/domain` : 도메인별 기능 코드
- `/global` : util, 예외처리, BaseEntity 등 전역 설정 파일

## 코드 컨벤션

### 네이밍 컨벤션

| 항목 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스, 인터페이스 | PascalCase | UserController, AuthService |
| 메서드 | camelCase | getUserInfo(), sendMail() |
| DTO | Response / Request suffix | UserInfoRequest, KakaoAuthResponse |
| 변수 | camelCase | userList, savedUser, isActive |
| 상수 | SNAKE_CASE | MAX_RETRY_COUNT |

### 기타 규칙

- Lombok 사용
    - DTO에는 가급적 `@Getter`, `@Setter` 사용
    - Entity에는 `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 명시
- Validation
    - 요청 DTO에 `@Valid`, `@NotNull`, `@Size` 등 활용
- 함수 하나에서는 하나의 역할만 하도록 설계

### Swagger 컨벤션

- **Controller**

    ```java
    /**
     * 즐겨찾기 생성 or 수정
     * @param userDetail 사용자정보
     * @param req 즐겨찾기 생성을 위한 정보
     */
    @Operation(summary = "즐겨찾기 생성 또는 수정", description = "즐겨찾기 생성 또는 수정: 로그인 필요")
    @PostMapping
    public CommonResponse<CreateBookmarkRes> createCategory(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal UserDetailsImpl userDetail,
        @Parameter(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모", required = true)
        @RequestBody CreateBookmarkReq req) {
        return CommonResponse.success(bookmarkService.createBookmark(userDetail.getUser().getUserId(), req));
    }
    ```

- **ResponseDTO**

    ```java
    @Getter
    @Schema(description = "DTO 설명 여기")
    public class UpdatePlaceNicknamesResponse {
        @Schema(description = "장소 id", example = "1")
        private Long PlaceId;
    }
    ```

- **RequestDTO**

    ```java
    @Getter
    @Setter
    @Schema(description = "DTO 설명 여기")
    public class ModifyPlaceRequest {
        @Schema(description = "장소 detail", example = "CU")
        private String detail;
    }
    ```

## DB 컨벤션

### 테이블명

- 소문자 + snake_case, ex) `user_accounts`
- 복수형 권장 - 엔티티 집합 의미, ex) `users`
- 접두사/접미사 지양 - 명확한 도메인으로 구분, ex) `app_users` ❌, `users` ✅

### 컬럼명

- 소문자 + snake_case, ex) `created_at`
- 테이블명 생략 - 중복 생략
- `boolean` 값은 `is`, `has`, `flag_` 로 시작, ex) `is_active`, `has_benefit`

### 기본키 및 외래키

- 기본키 `id` 사용
- 외래키 - 참조 테이블 + `_id`, ex) `orders.user_id`

### 날짜/시간 컬럼명

- 생성 시각 `created_at`
- 최종 수정 시각 `updated_at`
- 삭제 시각 `deleted_at`

### 인덱스, 제약 조건, 트리거 이름

- 인덱스 - `idx_<테이블명>_<컬럼명>` → `idx_users_email`
    - 복합 인덱스 - `idx_<테이블명>_<컬럼1>_<컬럼2>...` → `idx_users_email_password`
- 외래키 - `fk_<테이블명>_<참조테이블>` → `fk_orders_users`
- 유니크 제약 - `uk_<테이블명>_<컬럼명>` → `uk_users_email`
- 트리거 - `trg_<테이블명>_<이벤트>` → `trg_users_before_insert`

### 기타

- ENUM 대신 코드 테이블 사용
- NULL 최소화 - 가능한 NOT NULL + 기본값 지정

## Git 컨벤션

### 브랜치 전략

| 브랜치 | 용도 |
| --- | --- |
| `main` | 실제 운영 환경에 배포되는 코드를 관리하는 브랜치 |
| `dev` | 모든 개발자들이 작업 결과를 통합하는 브랜치, 배포 전 통합 테스트 목적 |
| `feat/기능명` | 개별 기능 단위 작업용 브랜치 (dev에서 분기) |
| `fix/버그명` | 운영 중인 버그 긴급 수정 브랜치 (main 또는 dev에서 분기) |

**PR 및 Merge 규칙**

- 모든 기능은 `feat/기능명` 브랜치에서 개발 후, dev 브랜치로 PR을 생성하여 병합. 모든 PR은 하나 이상의 Approve 필요.
- 절대로 `main` 브랜치에 직접 커밋하거나 푸시하지 않음. 배포 시에는 반드시 `dev` → `main` PR을 통해서 merge.

### 커밋 컨벤션

| Prefix | 설명 | 예시 |
| --- | --- | --- |
| `feat` | 새로운 기능 추가 | `feat: 카카오 로그인 기능 구현` |
| `fix` | 버그 수정 | `fix: 토큰 만료 시 예외 처리 수정` |
| `docs` | 문서 변경 | `docs: README 파일 업데이트` |
| `style` | 코드 스타일 변경 (포맷팅, 세미콜론 등) | `style: 코드 줄 정리 및 공백 수정` |
| `refactor` | 코드 리팩토링 (기능 변화 없음) | `refactor: 중복 코드 함수로 분리` |
| `test` | 테스트 코드 추가/수정 | `test: UserService 단위 테스트 추가` |
| `chore` | 빌드, 설정 등 기타 작업 | `chore: GitHub Actions CI 설정 추가` |
| `comment` | 주석 추가 또는 수정 | `comment: 메서드 설명 주석 추가` |
| `rename` | 파일/폴더 이름 변경 | `rename: user-controller → user-rest-controller` |
| `remove` | 파일 삭제 | `remove: 사용하지 않는 AuthUtils 삭제` |
| `update` | 코드 수정 | `update: Role 추가` |

### 브랜치명 컨벤션

| Prefix | 설명 | 예시 |
| --- | --- | --- |
| `feat/` | 기능 개발 | `feat/kakao-login` |
| `fix/` | 버그 수정 | `fix/token-refresh` |
| `refactor/` | 코드 리팩토링 (기능 변화 없음) | `refactor/user-service` |
| `test/` | 테스트 코드 추가 | `test/auth-service` |
| `docs/` | 문서 작업 | `docs/readme-update` |
| `chore/` | 설정, 빌드, 환경 구성 등 기타 작업 | `chore/github-actions` |

규칙: 브랜치명은 소문자와 하이픈(-)으로 구성, 작업 내용을 간결하고 명확하게 작성, 한글 사용 X, 개인 이름 X(`dongjun-feat` 등 금지)

### PR / 이슈 공통 규칙

1. **제목 작성 규칙**
    - `[Prefix]`는 반드시 대괄호로 감싸서 사용 (❌ `feat:` / ⭕ `[Feat]`)
    - 제목은 한글로, 요약형으로 간결하게 작성, ex) `[Fix] 로그인 시 500 에러 발생 문제 해결`
2. **Prefix 사용 규칙**
    - 기능 개발 관련 작업은 이슈/PR 둘 다 `[Feat]` 사용
    - 설정/CI 작업은 `[Chore]`, 문서 작업은 `[Docs]`로 고정
    - 기능 추가 없이 코드 정리만 한 경우는 `[Refactor]`
3. **이슈 번호 연동**
    - PR 제목 끝에는 관련 이슈 번호를 반드시 적음: `(#12)`
    - PR 본문에 `Closes #12` 또는 `Fixes #12` 명시 → 자동 이슈 닫힘
4. **작업 단위 원칙**
    - 한 PR = 한 기능 단위 (가능하면 500줄 이하 권장)
    - 너무 작거나 단순한 이슈는 묶어서 PR 가능, 단 제목은 명확히
5. **브랜치명과 PR 제목 연동**

    | 브랜치명 | PR 제목 |
    | --- | --- |
    | `feat/signup-api` | `[Feat] 회원가입 API 구현 (#5)` |
    | `fix/review-bug` | `[Fix] 리뷰 작성 오류 수정 (#19)` |

### PR 컨벤션

```
[Prefix] 작업 내용 요약
```

| Prefix | 설명 | 예시 |
| --- | --- | --- |
| `[Feat]` | 기능 개발 | `[Feat] 카카오 로그인 구현 (#12)` |
| `[Fix]` | 버그 수정 | `[Fix] 토큰 만료 시 예외처리 수정 (#31)` |
| `[Refactor]` | 리팩토링 | `[Refactor] UserService 리팩토링 (#44)` |
| `[Test]` | 테스트 코드 추가 | `[Test] 요금제 비교 테스트 코드 작성 (#21)` |
| `[Docs]` | 문서 변경 | `[Docs] README 요약 작성 (#1)` |
| `[Chore]` | 설정 파일 등 기타 | `[Chore] Dockerfile 수정 (#7)` |

### 이슈 컨벤션

```
[Prefix] 작업 요약 (ex. [Feat] 카카오 로그인 API 구현)
```

| Prefix | 의미 | 예시 |
| --- | --- | --- |
| `[Feat]` | 새로운 기능 | `[Feat] 요금제 비교 기능 추가` |
| `[Fix]` | 버그 수정 | `[Fix] 로그인 시 토큰 오류 해결` |
| `[Refactor]` | 리팩토링 | `[Refactor] UserService 로직 정리` |
| `[Docs]` | 문서 작성/수정 | `[Docs] README 배포 방식 업데이트` |
| `[Test]` | 테스트 코드 | `[Test] 금칙어 필터링 테스트 작성` |
| `[Chore]` | 기타 설정 작업 | `[Chore] GitHub Actions 설정 추가` |