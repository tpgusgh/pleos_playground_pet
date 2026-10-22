# 차량 정보 질문과 먼저 말하기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 연료로 갈 수 있는 거리와 목적지까지 남은 거리를 읽어 질문에 답하고, 연료 부족과 도착 임박을 토끼가 먼저 알려준다.

**Architecture:** 신호 모델(`Sample`)에 주행 가능 거리와 목적지 값을 더하고, 판정·질문 분류·먼저 말하기 규칙은 Android 의존 없는 순수 Kotlin으로 두어 JVM 단위 테스트한다. `PetViewModel`이 이를 연결하고, 개발자 도구에 값을 움직이는 컨트롤을 더한다. LLM과 마이크는 이 계획의 범위가 아니다.

**Tech Stack:** 기존 프로젝트 그대로 (Kotlin 2.0.0, Compose, coroutines 1.9.0, DataStore). 새 의존성 없음.

**Spec:** `docs/superpowers/specs/2026-10-22-llm-voice-chat-design.md`의 "차량 정보 질문", "먼저 말하기", "구현 순서 1번"절. (같은 문서의 LLM, 마이크, 항상 듣기는 다른 계획이다.)

## Global Constraints

- 기존 프로젝트를 이어서 쓴다: minSdk 23, targetSdk 35, JDK 17, 패키지 `com.pleosplayground.drivequest`. 빌드는 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`로 한다.
- 이하 `SRC = app/src/main/java/com/pleosplayground/drivequest`, `TEST = app/src/test/java/com/pleosplayground/drivequest`.
- **가짜 신호만.** 새로 읽는 값은 모두 읽기 전용이고 없으면 `null`이다. 목적지를 바꾸는 기능은 만들지 않는다. 서버, 계정, LLM, 마이크는 이 계획에서 하지 않는다.
- 신호가 `null`이거나 NaN·음수·무한대면 "모름"으로 처리한다. 모든 신호가 없어도 앱은 죽지 않는다.
- 숫자는 정수로 말한다("약 120킬로미터"). **판단(충분한지)은 코드가 하고**, 문장은 정해진 문장이다.
- 방치·재촉하는 죄책감 문구 금지. 알림은 지시가 아니라 알림 말투.
- 먼저 말하기는 음성(TTS)과 말풍선으로만 하고 화면 조작을 요구하지 않는다. 주행 중에도 말한다.
- 질문 입력은 이 계획에서는 개발자 도구의 "말 입력" 칸이다. 실제 마이크는 LLM 계획에서 붙인다.
- 커밋 메시지는 한글(타입 접두사는 영어 유지), 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` 줄을 붙인다.
- 에뮬레이터는 켜지 않는다(사용자 요청: 리소스를 많이 쓴다). 검증은 JVM 단위 테스트와 `assembleDebug`로 한다.

## 이 계획에서 정한 수치와 규칙 (스펙이 정한 것 + 내 판단)

| 항목 | 값 |
|---|---|
| 연료 판정 | `UNKNOWN`: 갈 수 있는 거리나 남은 거리가 없거나 유한한 0 이상 값이 아님. `ENOUGH`: 갈 수 있는 거리 ≥ 남은 거리 × 1.1 + 5. `TIGHT`: ENOUGH는 아니지만 갈 수 있는 거리 ≥ 남은 거리. `SHORT`: 그보다 모자람 |
| 먼저 말하기 조건 | `FUEL_SHORT`: 판정이 `SHORT`. `LOW_RANGE`: 갈 수 있는 거리 < 50km. `NEARLY_THERE`: 목적지가 있고 남은 거리 ≤ 1km |
| 우선순위 | `FUEL_SHORT` > `LOW_RANGE` > `NEARLY_THERE` > 주행 시작 인사 |
| 말하는 간격 | 모든 자동 발화(알림과 주행 시작 인사) 사이 최소 180000ms. 주행 시작 인사는 기존 600000ms 규칙도 유지 |
| 같은 이유 반복 | 조건이 풀렸다가 다시 생기거나 목적지 이름이 바뀌어야 다시 말한다. 간격 때문에 못 말한 알림은 그대로 대기했다가 간격이 지나면 말한다 |
| 연료 알림 중복 | `FUEL_SHORT`를 말하면 같은 연료 이야기인 `LOW_RANGE`도 함께 말한 것으로 친다 |
| 알림 문장 | `FUEL_SHORT` "목적지까지 연료가 모자랄 것 같아요", `LOW_RANGE` "연료가 얼마 안 남았어요", `NEARLY_THERE` "거의 다 왔어요!" |
| "먼저 말하기" 스위치 | 기본 켜짐. 설정 대화상자가 아직 없으므로 개발자 도구에 두고 앱을 닫으면 켜짐으로 돌아간다(저장 안 함) |

## Review Focus

각 줄의 테스트는 해당 Task에 포함됨.
1. 값이 `null`·NaN·음수·무한대 → 판정 `UNKNOWN`, 알림 없음, 크래시 없음 (Task 1, Task 3).
2. 판정 경계: 갈 수 있는 거리가 남은 거리와 정확히 같을 때, 정확히 `남은 거리 × 1.1 + 5`일 때 (Task 1).
3. 질문 분류: 빈 문자열, 공백만, 관련 없는 문장, 한 문장에 연료와 목적지가 같이 있을 때의 우선순위 (Task 2).
4. 먼저 말하기 반복 방지: 같은 조건 반복, 180000ms 경계, 조건 해소 뒤 재무장, 새 목적지 재무장, 간격 때문에 막힌 알림이 나중에 나가는지 (Task 3, Task 4).
5. 질문의 쿨다운 안 응답 무시, "먼저 말하기" 꺼짐이면 침묵, 주행 중에도 질문 답변 가능 (Task 4).

## File Structure

| 파일 | 책임 |
|---|---|
| `SRC/core/Sample.kt` | 새 필드 4개 추가 (수정) |
| `SRC/core/TripInfo.kt` | `TripInfo`, `tripInfo()`, `FuelVerdict`, `fuelVerdict()` (신규) |
| `SRC/core/FakeSignalSource.kt` | `setTrip`, 새 프리셋 2개 (수정) |
| `SRC/core/CarQuestions.kt` | 질문 분류, 정해진 답, 못 알아들었을 때의 대사 (신규) |
| `SRC/core/Proactive.kt` | 먼저 말하기 판단, 알림 문장 (신규) |
| `SRC/ui/PetViewModel.kt` | 연결 (수정) |
| `SRC/ui/PetScreen.kt` | 개발자 도구 컨트롤 (수정) |
| `TEST/ui/TestFakes.kt` | 가짜 저장소와 가짜 말하기 도우미를 공유 (신규, 기존 테스트에서 옮김) |

---

### Task 1: 차량 정보 값과 연료 판정, 가짜 신호 확장

**Files:**
- Modify: `SRC/core/Sample.kt`, `SRC/core/FakeSignalSource.kt`
- Create: `SRC/core/TripInfo.kt`
- Test: `TEST/core/TripInfoTest.kt`, `TEST/core/FakeSignalSourceTest.kt`(새 케이스 추가), `TEST/core/SampleTest.kt`(기본값 케이스 추가)

**Interfaces:**
- Produces:
```kotlin
// Sample에 추가 (기본값 null)
val rangeKm: Float? = null,                 // 연료나 배터리로 갈 수 있는 거리
val destinationName: String? = null,
val distanceToDestinationKm: Float? = null,
val etaMinutes: Int? = null,

data class TripInfo(
    val rangeKm: Float?, val destinationName: String?,
    val distanceKm: Float?, val etaMinutes: Int?,
)
fun tripInfo(s: Sample): TripInfo

enum class FuelVerdict { ENOUGH, TIGHT, SHORT, UNKNOWN }
fun fuelVerdict(rangeKm: Float?, distanceKm: Float?): FuelVerdict

// FakeSignalSource에 추가
fun setTrip(rangeKm: Float?, destinationName: String?, distanceKm: Float?, etaMinutes: Int?)
// Preset에 추가: LONG_TRIP, RUNNING_OUT
```
새 프리셋 값(나머지 항목은 `HEALTHY`와 같게 `tireOk`·`doorsClosed`·`windowsClosed`·`wiperOk` = true): `LONG_TRIP` = `fuelPercent 60`, `rangeKm 200f`, `destinationName "집"`, `distanceToDestinationKm 80f`, `etaMinutes 70`. `RUNNING_OUT` = `fuelPercent 15`, `rangeKm 40f`, `destinationName "집"`, `distanceToDestinationKm 120f`, `etaMinutes 100`. 기존 프리셋 값은 바꾸지 않는다(새 필드는 null).

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `SampleTest.new_trip_fields_default_to_null`: `Sample()`의 `rangeKm`, `destinationName`, `distanceToDestinationKm`, `etaMinutes`가 모두 `null`.
  - `TripInfoTest.trip_info_copies_the_four_values`: `Sample(rangeKm = 120f, destinationName = "회사", distanceToDestinationKm = 35f, etaMinutes = 40)` → `TripInfo(120f, "회사", 35f, 40)`. 기본 `Sample()` → 전부 `null`.
  - `fuel_verdict_unknown_when_a_value_is_missing_or_invalid`: `(null, 10f)`, `(10f, null)`, `(NaN, 10f)`, `(10f, NaN)`, `(-1f, 10f)`, `(10f, -1f)`, `(Float.POSITIVE_INFINITY, 10f)`, `(10f, Float.POSITIVE_INFINITY)` → 모두 `UNKNOWN`. (Review Focus 1)
  - `fuel_verdict_boundaries`: `(100f, 100f)` → `TIGHT`(정확히 남은 거리와 같음). 남은 거리 100이면 `100 × 1.1 + 5 = 115`이므로 `(115f, 100f)` → `ENOUGH`, `(114.9f, 100f)` → `TIGHT`, `(99.9f, 100f)` → `SHORT`. (Review Focus 2)
  - `fuel_verdict_with_zero_distance`: `(0f, 0f)` → `TIGHT`, `(5f, 0f)` → `ENOUGH`.
  - `FakeSignalSourceTest`: `preset_long_trip_sets_trip_values`, `preset_running_out_sets_trip_values`(위 값), `existing_presets_have_no_trip_info`(`HEALTHY`, `LOW_FUEL`, `NEEDS_CARE`, `UNSUPPORTED`의 새 필드 4개가 null), `set_trip_updates_the_four_values_and_keeps_the_rest`(`setTrip(50f, "회사", 20f, 25)` 후 `fuelPercent`와 `tireOk`가 그대로, `setTrip(null, null, null, null)`이면 4개가 null).
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*TripInfoTest*" --tests "*FakeSignalSourceTest*" --tests "*SampleTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 Interfaces대로 구현한다.** `fuelVerdict`는 두 값이 모두 non-null이고 `isFinite()`이며 0 이상일 때만 판정한다.
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest assembleDebug` → PASS.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 주행 가능 거리와 목적지 값, 연료 판정 추가"`

---

### Task 2: 질문 분류와 정해진 답

**Files:**
- Create: `SRC/core/CarQuestions.kt`
- Test: `TEST/core/CarQuestionsTest.kt`

**Interfaces:**
- Consumes: `TripInfo`, `FuelVerdict`, `fuelVerdict` (Task 1), `Face`
- Produces:
```kotlin
enum class CarQuestion { RANGE, DESTINATION, ENOUGH }
fun classifyQuestion(text: String): CarQuestion?
fun answerFor(q: CarQuestion, trip: TripInfo): String
fun notUnderstoodLine(face: Face): String
```

분류 규칙(먼저 공백과 `? ! . ,`를 모두 지우고 소문자로 바꾼 글자에서 찾는다): 
1. **`ENOUGH`**: `충분`, `모자라`, `부족`, `버틸` 중 하나가 있고 `연료`, `기름`, `배터리`, `충전`, `목적지`, `도착` 중 하나가 있거나, `연료`(또는 `기름`·`배터리`)와 `목적지`가 둘 다 있을 때.
2. **`DESTINATION`**: 위가 아니고 `목적지`, `도착`, `몇분`, `몇킬로` 중 하나가 있을 때.
3. **`RANGE`**: 위가 아니고 `연료`, `기름`, `배터리`, `충전` 중 하나가 있을 때.
4. 그 밖에는 `null`.

정해진 답(숫자는 `roundToInt()`): 
- `RANGE`: 갈 수 있는 거리가 있으면 "지금 연료로 약 N킬로미터 갈 수 있어요", 없으면 "갈 수 있는 거리를 잘 모르겠어요".
- `DESTINATION`: 목적지 이름이나 남은 거리가 없으면 "목적지가 설정 안 돼 있어요". 있으면 "{이름}까지 약 N킬로미터 남았어요", 도착 예정 시간(분)이 있으면 뒤에 ", 대략 M분 걸려요"를 붙인다.
- `ENOUGH`: 목적지 이름이나 남은 거리가 없으면 "목적지가 설정 안 돼 있어요". 판정이 `UNKNOWN`이면 "갈 수 있는 거리를 몰라서 판단하기 어려워요", `ENOUGH`이면 "네, 목적지까지 연료는 충분해요", `TIGHT`이면 "빠듯해요. 목적지까지 간신히 갈 수 있어요", `SHORT`이면 "아니요, 목적지까지 연료가 모자랄 것 같아요".
- `notUnderstoodLine`: 행복 "음… 잘 모르겠어요", 시무룩 "...잘 모르겠어요", 배고픔 "배고파서 잘 모르겠어요...".

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `range_questions`: "연료 얼마나 가?", "기름 얼마나 남았어", "배터리 얼마나 남았어?", "충전량 얼마야" → `RANGE`.
  - `destination_questions`: "목적지까지 얼마나 남았어?", "언제 도착해?", "몇 분 걸려?", "몇 킬로 남았어" → `DESTINATION`.
  - `enough_questions`: "목적지까지 연료 충분해?", "연료 모자라지 않을까?", "기름 부족해?", "연료로 목적지까지 갈 수 있어?" → `ENOUGH`. "배터리로 도착할 수 있어?"는 `충분` 같은 단어가 없고 연료류와 `목적지`가 함께 있지 않으므로 규칙 2에 따라 `DESTINATION`이 된다. 이 결과를 그대로 단언한다.
  - `ambiguity_priority`: "목적지까지 연료로 얼마나 남았어?"(연료와 목적지) → `ENOUGH`.
  - `unrelated_and_empty_are_null`: "", "   ", "안녕", "오늘 날씨 어때?", "노래 틀어줘" → `null`. (Review Focus 3)
  - `punctuation_and_spacing_are_ignored`: "연  료  얼 마 나 가 ?!" → `RANGE`.
  - `answers_with_values`: `TripInfo(199.6f, "집", 80.4f, 70)`로 `RANGE` → "지금 연료로 약 200킬로미터 갈 수 있어요", `DESTINATION` → "집까지 약 80킬로미터 남았어요, 대략 70분 걸려요", 도착 예정 시간이 `null`이면 "집까지 약 80킬로미터 남았어요".
  - `answers_without_values`: `TripInfo(null, null, null, null)`로 `RANGE` → "갈 수 있는 거리를 잘 모르겠어요", `DESTINATION`·`ENOUGH` → "목적지가 설정 안 돼 있어요".
  - `enough_answers_follow_the_verdict`: `(200f, "집", 80f, 70)` → "네, 목적지까지 연료는 충분해요", `(85f, "집", 80f, 70)` → "빠듯해요. 목적지까지 간신히 갈 수 있어요", `(40f, "집", 120f, 100)` → "아니요, 목적지까지 연료가 모자랄 것 같아요", `(null, "집", 80f, 70)` → "갈 수 있는 거리를 몰라서 판단하기 어려워요".
  - `not_understood_lines_differ_by_face`: 세 표정의 문장이 비어 있지 않고 서로 다르다.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*CarQuestionsTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 규칙과 문장 그대로 구현한다.**
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 차량 정보 질문 분류와 정해진 답 추가"`

---

### Task 3: 먼저 말하기 판단

**Files:**
- Create: `SRC/core/Proactive.kt`
- Test: `TEST/core/ProactiveTest.kt`

**Interfaces:**
- Consumes: `TripInfo`, `FuelVerdict`, `fuelVerdict` (Task 1)
- Produces:
```kotlin
enum class Alert { FUEL_SHORT, LOW_RANGE, NEARLY_THERE }

data class AlertState(
    val shortArmed: Boolean = true,
    val lowArmed: Boolean = true,
    val nearArmed: Boolean = true,
    val lastSpokeMs: Long? = null,
    val lastDestination: String? = null,
)
data class AlertDecision(val state: AlertState, val alert: Alert?)

fun decideAlert(prev: AlertState, trip: TripInfo, nowMs: Long, canSpeak: Boolean): AlertDecision
fun AlertState.spoke(nowMs: Long): AlertState     // 주행 시작 인사 같은 다른 자동 발화도 간격에 포함시킨다
fun alertLine(alert: Alert): String
const val ALERT_GAP_MS = 180_000L
```

규칙(표의 값 그대로): 
1. 조건 계산: `shortNow = fuelVerdict(...) == SHORT`, `lowNow = rangeKm`이 유한하고 `< 50f`, `nearNow = destinationName != null`이고 `distanceKm`이 유한하고 `>= 0`이며 `<= 1f`.
2. 재무장: `trip.destinationName != prev.lastDestination`이면 세 `Armed`를 모두 `true`로 하고 `lastDestination`을 갱신한다. 각 조건이 `false`이면 그 `Armed`를 `true`로 되돌린다.
3. 발화 가능 조건: `canSpeak`이고 (`lastSpokeMs == null` 또는 `nowMs - lastSpokeMs >= ALERT_GAP_MS`).
4. 후보 우선순위: `FUEL_SHORT`(`shortNow && shortArmed`) > `LOW_RANGE`(`lowNow && lowArmed`) > `NEARLY_THERE`(`nearNow && nearArmed`). 발화 가능하면 첫 후보를 말하고 그 `Armed`를 `false`, `lastSpokeMs = nowMs`로 한다. `FUEL_SHORT`를 말하면 `lowArmed`도 `false`로 한다.
5. 발화 가능하지 않으면 `alert = null`이고 `Armed`는 재무장 규칙 외에는 건드리지 않는다(막힌 알림은 대기한다).

- [ ] **Step 1: 실패하는 테스트 작성.** 헬퍼로 `TripInfo(range, "집", distance, eta)`를 만든다.
  - `short_fires_once_and_disarms`: 처음 `decideAlert(AlertState(), TripInfo(40f, "집", 120f, 100), 1_000, true)` → `FUEL_SHORT`, `shortArmed == false`, `lowArmed == false`, `lastSpokeMs == 1_000`. 같은 조건으로 `1_000 + 400_000`에 다시 → `null`.
  - `priority_short_over_low_over_near`: 모자람과 낮은 거리가 동시 → `FUEL_SHORT`. 모자람 없이 `range 40`, 목적지 없음 → `LOW_RANGE`. `range 500`, 남은 거리 0.8 → `NEARLY_THERE`.
  - `gap_blocks_and_alert_waits`: `FUEL_SHORT`를 말한 직후 60_000ms 뒤 `NEARLY_THERE` 조건이 생기면 `null`이고 `nearArmed`는 그대로 `true`. `lastSpokeMs + 180_000 - 1`에서도 `null`, 정확히 `lastSpokeMs + 180_000`에서는 `NEARLY_THERE`. (Review Focus 4)
  - `cannot_speak_changes_nothing_but_rearming`: `canSpeak = false`이면 `null`이고 `Armed` 값이 바뀌지 않는다.
  - `rearm_after_recovery`: 모자람 → 말함 → 조건이 풀림(충분한 값) → 다시 모자람 → 간격이 지난 뒤 `FUEL_SHORT`를 또 말한다. (Review Focus 4)
  - `rearm_on_new_destination`: 같은 모자람 조건에서 목적지 이름이 "집" → "회사"로 바뀌고 간격이 지나면 `FUEL_SHORT`를 또 말한다.
  - `unknown_and_invalid_values_never_alert`: `TripInfo(null, null, null, null)`, `TripInfo(Float.NaN, "집", Float.NaN, null)`, `TripInfo(-5f, "집", -5f, null)`, `TripInfo(Float.POSITIVE_INFINITY, "집", Float.NEGATIVE_INFINITY, null)` → `alert == null`, 크래시 없음. (Review Focus 1)
  - `near_requires_a_destination`: 목적지 이름이 없고 남은 거리가 0.5이면 `NEARLY_THERE`가 아니다.
  - `spoke_marks_the_time_only`: `AlertState().spoke(5_000)`는 `lastSpokeMs == 5_000`이고 나머지 값은 그대로.
  - `alert_lines`: 세 문장이 표와 정확히 같다.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*ProactiveTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 시그니처와 규칙대로 구현한다.**
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest` → PASS.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 연료 부족과 도착 임박을 먼저 알리는 판단 규칙 추가"`

---

### Task 4: ViewModel 연결

**Files:**
- Modify: `SRC/ui/PetViewModel.kt`, `TEST/ui/PetViewModelTest.kt`(가짜 도우미를 `TestFakes.kt`로 옮김)
- Create: `TEST/ui/TestFakes.kt`
- Test: `TEST/ui/PetViewModelTripTest.kt`

**Interfaces:**
- Consumes: `TripInfo`, `tripInfo`, `FakeSignalSource.setTrip`, `Preset.LONG_TRIP`, `Preset.RUNNING_OUT` (Task 1), `classifyQuestion`, `answerFor`, `notUnderstoodLine` (Task 2), `AlertState`, `decideAlert`, `spoke`, `alertLine`, `ALERT_GAP_MS` (Task 3), 기존 `PetState.talk`, `Narrator`
- Produces (`PetViewModel`에 추가):
```kotlin
val trip: StateFlow<TripInfo>
val proactiveEnabled: StateFlow<Boolean>      // 기본 true
fun setProactive(enabled: Boolean)
fun ask(text: String)                          // 질문 입력, 주행 중에도 가능
fun setTrip(rangeKm: Float?, destinationName: String?, distanceKm: Float?, etaMinutes: Int?)
```

동작:
- `trip`은 `source.state`가 바뀔 때마다 `tripInfo(sample)`로 갱신한다(처음 값 포함).
- **먼저 말하기**: 읽기가 끝난 뒤 `source.state`를 구독하는 곳에서, 그리고 `setProactive(true)`가 불릴 때, `decideAlert(alertState, trip, clock(), proactiveEnabled.value)`를 부르고 결과 상태를 저장한다. `alert`가 있으면 `say(alertLine(alert))`.
- **간격 공유**: 주행 시작 인사를 말하면 `alertState = alertState.spoke(now)`로 알림 간격에 포함한다. 주행 시작 인사는 `now - alertState.lastSpokeMs`가 `ALERT_GAP_MS` 이상일 때만 말한다(기존 600000ms 규칙도 그대로 요구).
- **`ask(text)`**: `act` 안에서 `_pet.value.talk(clock(), currentFace())`를 쿨다운과 기분 처리에 그대로 쓴다. 결과 `reply`가 `null`(쿨다운 안)이면 아무것도 하지 않는다. 아니면 상태를 저장하고, `classifyQuestion(text)`가 있으면 `say(answerFor(q, trip.value))`, 없으면 `say(notUnderstoodLine(currentFace()))`. 기존 `talk()`(버튼, 인사)는 그대로 둔다.
- `setTrip`은 `act { source.setTrip(...) }`.
- 개발 편의: `ask`와 `talk`가 같은 `talkReadyAtMs` 쿨다운을 공유한다.
- `// ponytail:` 주석으로 "대화 중에는 먼저 말하지 않는 규칙은 LLM 계획에서 추가한다"를 남긴다.

- [ ] **Step 1: 테스트 도우미 이동.** `PetViewModelTest.kt`의 `FakeRepo`와 `FakeNarrator`를 `TEST/ui/TestFakes.kt`로 옮기고(`internal`), `PetViewModelTest`가 그것을 쓰게 한다. `./gradlew testDebugUnitTest --tests "*PetViewModelTest*"`가 이동 전과 같은 13개로 통과해야 한다(이동만 하므로 새 실패 테스트는 없다).
- [ ] **Step 2: 실패하는 테스트 작성** (`PetViewModelTripTest`, 기존과 같은 `StandardTestDispatcher` + `advanceUntilIdle` 패턴, 바뀌는 `clock`).
  - `trip_follows_the_signal_source`: `applyPreset(LONG_TRIP)` 후 `trip.value == TripInfo(200f, "집", 80f, 70)`.
  - `ask_range_answers_from_trip_info`: `LONG_TRIP`에서 `ask("연료 얼마나 가?")` → 말한 줄이 `[ "지금 연료로 약 200킬로미터 갈 수 있어요" ]`.
  - `ask_destination_without_destination_says_not_set`: 기본 `HEALTHY`에서 `ask("목적지까지 얼마나 남았어?")` → "목적지가 설정 안 돼 있어요".
  - `ask_enough_follows_the_verdict`: `RUNNING_OUT`에서 `ask("목적지까지 연료 충분해?")` 의 답은 "아니요, 목적지까지 연료가 모자랄 것 같아요"(먼저 말하기 알림이 먼저 나온 뒤이므로 마지막 줄을 본다).
  - `ask_unrecognized_says_not_understood`: `ask("오늘 날씨 어때?")` → `notUnderstoodLine(face)`.
  - `ask_works_while_driving`: 기본 `RESTRICTED`에서 `ask("연료 얼마나 가?")`가 답한다.
  - `ask_respects_the_talk_cooldown`: 같은 clock에서 `ask`를 두 번 → 한 줄, 기분 +5는 한 번. `talk()` 직후의 `ask`도 같은 쿨다운 안이면 답하지 않는다. (Review Focus 5)
  - `running_out_speaks_fuel_short_once`: `applyPreset(RUNNING_OUT)` → 말한 줄이 `[ alertLine(FUEL_SHORT) ]` 한 개. clock을 `ALERT_GAP_MS * 3` 늘리고 `setTrip(40f, "집", 119f, 99)`로 새 상태를 내보내도 추가로 말하지 않는다(`LOW_RANGE`도 함께 말한 것으로 처리됨). (Review Focus 4)
  - `nearly_there_speaks`: `LONG_TRIP` 후 `setTrip(200f, "집", 0.8f, 1)` → `alertLine(NEARLY_THERE)`를 말한다.
  - `alerts_are_silent_when_disabled_and_speak_when_enabled`: `setProactive(false)` → `RUNNING_OUT`에서 말한 줄 없음. `setProactive(true)` → `alertLine(FUEL_SHORT)`를 말한다. (Review Focus 5)
  - `alert_gap_is_shared_with_the_drive_start_greeting`: `setParked(true)`, `setParked(false)`로 주행 시작 인사를 한 줄 말하게 한 직후 같은 clock에서 `RUNNING_OUT` → 알림은 아직 말하지 않는다. clock을 `ALERT_GAP_MS` 늘리고 `setTrip(40f, "집", 120f, 101)`로 상태를 바꾸면 `FUEL_SHORT`를 말한다. (Review Focus 4)
  - `drive_start_greeting_is_skipped_right_after_an_alert`: `RUNNING_OUT`으로 알림을 말한 직후 같은 clock에서 `setParked(true)`, `setParked(false)` → 주행 시작 인사를 말하지 않는다.
  - `missing_values_do_not_break_alerts_or_answers`: `applyPreset(UNSUPPORTED)`에서 `ask("목적지까지 연료 충분해?")` → "목적지가 설정 안 돼 있어요", 먼저 말하기 줄 없음. (Review Focus 1)
- [ ] **Step 3: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*PetViewModelTripTest*"` → 컴파일 실패.
- [ ] **Step 4: 위 동작대로 `PetViewModel`을 구현한다.** 기존 동작(`persist`, `loaded.join()` 규칙, 쿨다운, 주행 시작 인사의 600000ms 간격)은 바꾸지 않는다.
- [ ] **Step 5: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest assembleDebug` → PASS(기존 `PetViewModelTest` 13개 포함).
- [ ] **Step 6: 커밋.** `git add -A && git commit -m "feat: 차량 정보 질문 답변과 먼저 말하기를 ViewModel에 연결"`

---

### Task 5: 개발자 도구 컨트롤

**Files:**
- Modify: `SRC/ui/PetScreen.kt`

**Interfaces:**
- Consumes: `PetViewModel`의 `trip`, `proactiveEnabled`, `setProactive`, `ask`, `setTrip` (Task 4), `Preset.entries`(새 프리셋 포함)

개발자 도구(정차 모드에서만 보임, 기존 `ponytail:` 주석 영역)에 추가한다:
- 프리셋 칩은 `Preset.entries`를 쓰므로 `LONG_TRIP`, `RUNNING_OUT`이 자동으로 보인다(별도 변경 없음, 확인만).
- **"말 입력" 한 줄 칸과 "보내기" 버튼**: 보내면 `vm.ask(text)`를 부르고 칸을 비운다(마이크 대신).
- **갈 수 있는 거리 슬라이더**(0..600 km, `trip.rangeKm` 기준)와 **남은 거리 슬라이더**(0..300 km, `trip.distanceKm` 기준): 값이 없으면 비활성이고 "없음"을 표시한다. 움직이면 현재 `trip` 값에 해당 항목만 바꿔 `vm.setTrip(...)`을 부른다. 슬라이더는 `trip`에서 값을 파생하고 로컬 복사본을 두지 않는다(이전에 연료 슬라이더에서 고친 문제와 같다).
- **"먼저 말하기" 스위치**: `proactiveEnabled`와 `setProactive`에 연결.
- 가로·세로 레이아웃, 주행 제한 모드에서 개발자 도구가 숨는 규칙은 그대로.

- [ ] **Step 1: 위 컨트롤을 구현한다.** 화면 로직은 ViewModel에 이미 있으므로 단위 테스트는 두지 않는다.
- [ ] **Step 2: 컴파일 확인.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS, BUILD SUCCESSFUL. 에뮬레이터는 켜지 않는다. 화면 확인은 사용자가 요청할 때 따로 한다(보고서에 "에뮬레이터 확인 안 함"을 적는다).
- [ ] **Step 3: 커밋.** `git add -A && git commit -m "feat: 개발자 도구에 차량 정보 입력과 먼저 말하기 스위치 추가"`
