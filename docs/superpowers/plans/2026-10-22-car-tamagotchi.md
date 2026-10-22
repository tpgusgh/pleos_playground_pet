# 차 타마고치 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 가짜 차량 신호로 토끼 귀형 자동차 캐릭터의 밥·건강·기분을 보여주고, 쓰다듬기와 말 걸기로 돌보는 타마고치 앱.

**Architecture:** 기존 Android 프로젝트에서 RPG 코드(영웅, 전투, 점수)를 걷어내고 신호 입력을 "현재 차 상태 스트림"으로 바꾼다. 게이지 계산, 표정 결정, 시간 경과, 쿨다운은 Android 의존 없는 순수 Kotlin으로 두고 JVM 단위 테스트한다. ViewModel이 신호, 저장소, TTS를 묶고 Compose 화면이 이를 그린다.

**Tech Stack:** Kotlin 2.0.0, Jetpack Compose (BOM 2024.10.01), kotlinx-coroutines 1.9.0, DataStore Preferences 1.1.1, AGP 8.8.2, Gradle 8.10.2 (기존 프로젝트 그대로).

**Spec:** `docs/superpowers/specs/2026-10-22-car-tamagotchi-design.md`

## Global Constraints

- 기존 프로젝트를 이어서 쓴다: minSdk 23, targetSdk 35, compileSdk 35, JDK 17, 패키지 `com.pleosplayground.drivequest`. 빌드는 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`로 한다.
- 이하 `SRC = app/src/main/java/com/pleosplayground/drivequest`, `TEST = app/src/test/java/com/pleosplayground/drivequest`.
- 서버, 계정, 차량 제어(쓰기), 전투·성장, 죽음·가출, 백그라운드 서비스, 실제 Pleos SDK, 실제 음성 인식은 하지 않는다. **가짜 신호만.**
- 신호가 `null`이면 미지원으로 보고 그 항목은 건너뛴다. 모든 신호가 `null`이어도 앱은 죽지 않는다.
- 게이지는 0~100. 표정은 **행복 / 시무룩 / 배고픔**, 우선순위는 배고픔 > 시무룩 > 행복.
- 방치하면 시무룩해질 뿐이다. 죄책감을 주는 문구는 쓰지 않는다.
- 주행 제한(`UxState.RESTRICTED`)에서는 화면 조작을 요구하지 않는다. 쓰다듬기는 정차(`PARKED`)에서만, **말 걸기는 주행 중에도** 가능.
- 속도·가속 항은 두지 않는다. 급제동·급조향은 기분을 내린다.
- 커밋 메시지는 한글(타입 접두사는 영어 유지), 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` 줄을 붙인다.
- 앱 라벨은 "차 타마고치", 패키지 이름은 유지.

## 이 계획에서 정한 수치 (스펙이 빌더에게 맡긴 값)

| 항목 | 값 |
|---|---|
| 급제동 기준 / 급조향 기준 | `brakeDecel > 4.0f` / `steeringRate > 120f` |
| 기분 변화 (샘플당) | 급제동 −5, 급조향 −3, 부드러운 제동(0 < 감속 ≤ 4.0) +1, 부드러운 조향(0 < 조향 ≤ 120) +1. 한 샘플에 둘 다 있으면 합산 |
| 기분 범위 / 초기값 | 0~100 / 70 |
| 시간 감소 | 30분마다 −1 (하루 약 −48), 바닥 0. 돌봄과 무관한 일정한 속도 |
| 표정 | 밥 < 20 → 배고픔. 아니면 건강 < 50 또는 기분 < 40 → 시무룩. 아니면 행복 |
| 쓰다듬기 / 말 걸기 | 기분 +10 / +5, 쿨다운 각 5000ms |
| 주행 시작 자동 인사 최소 간격 | 600000ms (10분) |

## Review Focus

각 줄의 테스트는 해당 Task에 포함됨.
1. 모든 신호가 `null` → 게이지 숨김, 표정은 기분만으로 결정, 크래시 없음 (Task 2, Task 4).
2. 시계가 뒤로 감기거나 저장 시각이 0(처음 실행) → 감소 없음, 기분이 갑자기 0이 되지 않음 (Task 3).
3. 손상된 저장 문자열, 범위 밖 기분, 저장소 읽기·쓰기 실패 → 기본 상태로 계속 동작 (Task 3, Task 4).
4. 쓰다듬기·말 걸기 연타 → 쿨다운 안에서는 변화 없음 (Task 3).
5. 주행 제한 중 쓰다듬기는 무시, 자동 인사는 최소 간격 안에서 반복되지 않음 (Task 4).

## File Structure

| 파일 | 책임 |
|---|---|
| `SRC/core/Sample.kt` | 현재 차 상태 한 틱 (재구성) |
| `SRC/core/VehicleSignalSource.kt` | `ux`, `state` 스트림 인터페이스 (재구성) |
| `SRC/core/FakeSignalSource.kt` | 프리셋, 연료, 주행 시뮬레이션 (재구성) |
| `SRC/core/UxGate.kt` | `UxState`, `canPlay` (축소) |
| `SRC/core/Pet.kt` | 밥·건강·기분 변화·표정 (신규) |
| `SRC/core/PetState.kt` | 기분 상태, 시간 경과, 쿨다운, 대사, 저장 문자열 (신규) |
| `SRC/data/PetStore.kt` | DataStore 저장소 (HeroStore 대체) |
| `SRC/ui/PetViewModel.kt` | 신호·저장·TTS를 묶는 ViewModel (GameViewModel 대체) |
| `SRC/ui/PetFace.kt`, `SRC/ui/PetScreen.kt`, `SRC/ui/MainActivity.kt` | 캐릭터 그리기와 화면 |
| `SRC/ui/Speaker.kt` | 유지 (`Narrator`, `Speaker`) |

---

### Task 1: RPG 코드 제거와 신호 모델 재구성

**Files:**
- Delete: `SRC/core/Hero.kt`, `SRC/core/Battle.kt`, `SRC/core/DriveScorer.kt`, `SRC/data/HeroStore.kt`, `SRC/ui/GameViewModel.kt`, `SRC/ui/GameScreens.kt`, `TEST/core/HeroTest.kt`, `TEST/core/BattleTest.kt`, `TEST/core/DriveScorerTest.kt`, `TEST/ui/GameViewModelTest.kt`
- Modify: `SRC/core/Sample.kt`, `SRC/core/VehicleSignalSource.kt`, `SRC/core/FakeSignalSource.kt`, `SRC/core/UxGate.kt`, `SRC/ui/MainActivity.kt`(자리표시 화면), `app/src/main/AndroidManifest.xml`(라벨), `TEST/core/SampleTest.kt`, `TEST/core/FakeSignalSourceTest.kt`, `TEST/core/UxGateTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class Sample(
    val tMs: Long = 0,
    val brakeDecel: Float? = null,    // m/s², 제동 감속도
    val steeringRate: Float? = null,  // deg/s, 절댓값
    val fuelPercent: Int? = null,     // 연료 또는 EV 배터리 잔량, 0~100
    val tireOk: Boolean? = null,
    val doorsClosed: Boolean? = null,
    val windowsClosed: Boolean? = null,
    val wiperOk: Boolean? = null,
)

enum class UxState { PARKED, RESTRICTED }
fun canPlay(ux: UxState): Boolean            // PARKED만 true

interface VehicleSignalSource {
    val ux: StateFlow<UxState>
    val state: StateFlow<Sample>
}

enum class Preset { HEALTHY, LOW_FUEL, NEEDS_CARE, UNSUPPORTED }
enum class DriveStyle { SMOOTH, HARSH }

class FakeSignalSource(private val tickDelayMs: Long = 0) : VehicleSignalSource {
    fun setUx(state: UxState)
    fun setFuel(percent: Int?)
    fun applyPreset(preset: Preset)     // 정적 항목만 바꾸고 brake/steer는 null, ux는 유지
    suspend fun drive(style: DriveStyle)
}
```

프리셋 값: `HEALTHY` = 연료 80, 나머지 true. `LOW_FUEL` = 연료 10, 나머지 true. `NEEDS_CARE` = 연료 55, `tireOk`·`doorsClosed`·`windowsClosed` false, `wiperOk` true. `UNSUPPORTED` = 모든 항목 null. 초기 상태는 `HEALTHY`, 초기 `ux`는 `RESTRICTED`.
`drive`: 20틱. `SMOOTH` = 틱 `i % 4 == 3`에서 `brakeDecel = 2.0f`(아니면 null), `steeringRate = 20f`. `HARSH` = 틱 `i % 5 == 4`에서 `brakeDecel = 6.0f`, `steeringRate`는 `i % 3 == 0`이면 150f 아니면 30f. 매 틱 `tMs`는 호출을 가로질러 증가하는 카운터(StateFlow가 같은 값을 합치지 않게), 각 틱 뒤 `tickDelayMs > 0`이면 `delay`, 아니면 `yield()`. 끝나면 `brakeDecel`·`steeringRate`를 null로 되돌린 샘플을 한 번 더 낸다. 정적 항목은 보존한다.

- [ ] **Step 1: 실패하는 테스트 작성** (`runTest`, 틱 수집은 `UnconfinedTestDispatcher`로 `state`를 구독해 리스트에 쌓는다).
  - `SampleTest.default_sample_has_all_signals_unsupported`: `Sample()`의 `brakeDecel`, `fuelPercent`, `tireOk`가 모두 `null`.
  - `FakeSignalSourceTest`: `initial_state_is_healthy`(연료 80, 네 항목 true, `brakeDecel` null), `ux_defaults_to_restricted_and_follows_setUx`, `preset_low_fuel_sets_fuel_10_and_keeps_rest_ok`, `preset_needs_care_marks_tire_doors_windows_bad`(셋 false, `wiperOk` true), `preset_unsupported_nulls_everything`(`tMs` 외 전부 null), `preset_keeps_ux`(`setUx(PARKED)` 후 `applyPreset` → 여전히 PARKED), `set_fuel_updates_state_and_null_clears_it`, `smooth_drive_has_no_hard_brakes_but_some_brakes`(수집한 샘플에 `brakeDecel > 4.0f` 0개, non-null `brakeDecel` 1개 이상), `harsh_drive_has_at_least_two_hard_brakes_and_sharp_steers`(`> 4.0f` 2개 이상, `steeringRate > 120f` 1개 이상), `drive_ends_with_quiet_sample_and_keeps_fuel`(끝난 뒤 `state.value`의 `brakeDecel`·`steeringRate` null, `fuelPercent` 유지).
  - `UxGateTest.only_parked_can_play`만 남긴다(`summaryLine` 테스트는 삭제).
- [ ] **Step 2: 삭제 대상 파일을 지운다.** 지운 뒤 `./gradlew testDebugUnitTest --tests "*FakeSignalSourceTest*"` → 컴파일 실패(`Preset`, `DriveStyle`, 새 `Sample` 필드 없음).
- [ ] **Step 3: 위 Interfaces대로 `Sample`, `VehicleSignalSource`, `FakeSignalSource`, `UxGate`를 구현한다.** `UxGate.kt`에서 `summaryLine`을 지운다. `MainActivity`는 `setContent { Text("차 타마고치") }` 자리표시 화면으로 줄이고(ViewModel·Speaker 참조 제거), 매니페스트 `android:label`을 "차 타마고치"로 바꾼다.
- [ ] **Step 4: 통과 확인.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "refactor: RPG 코드를 걷어내고 차 상태 스트림 신호 모델로 재구성"`

---

### Task 2: 게이지, 기분 변화, 표정

**Files:**
- Create: `SRC/core/Pet.kt`
- Test: `TEST/core/PetTest.kt`

**Interfaces:**
- Consumes: `Sample` (Task 1)
- Produces:
```kotlin
enum class Face { HAPPY, SULKY, HUNGRY }
fun food(s: Sample): Int?                       // fuelPercent를 0~100으로 고정, null이면 null
fun health(s: Sample): Int?                     // tireOk/doorsClosed/windowsClosed/wiperOk 중 non-null 항목 중 true의 비율(%) 내림, 전부 null이면 null
fun moodDelta(s: Sample): Int                   // 위 표의 규칙, NaN·0 이하·무한대 입력은 무시
fun faceFor(food: Int?, health: Int?, mood: Int): Face
```

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `food_clamps_and_passes_null`: 연료 150 → 100, −5 → 0, null → null.
  - `health_is_percent_of_known_items_that_are_ok`: 네 항목 모두 true → 100, 둘 true 둘 false → 50, 하나만 non-null이고 true → 100, 하나만 non-null이고 false → 0, 전부 null → null.
  - `hard_brake_lowers_mood_by_5`: `brakeDecel = 5f` → −5. `smooth_brake_raises_mood_by_1`: `2f` → +1.
  - `sharp_steer_lowers_mood_by_3`: `steeringRate = 150f` → −3. `smooth_steer_raises_mood_by_1`: `30f` → +1.
  - `brake_and_steer_in_one_sample_add_up`: `brakeDecel = 5f, steeringRate = 150f` → −8.
  - `nan_negative_zero_and_infinite_readings_are_ignored`: 각각 `Float.NaN`, `-1f`, `0f`, `Float.POSITIVE_INFINITY`를 `brakeDecel`과 `steeringRate`에 넣으면 0. (Review Focus 1)
  - `all_null_sample_has_zero_delta`: `Sample()` → 0.
  - `face_hungry_when_food_below_20`: `faceFor(19, 100, 100)` → HUNGRY, `faceFor(20, 100, 100)` → HAPPY.
  - `face_sulky_when_health_below_50_or_mood_below_40`: `faceFor(100, 49, 100)`, `faceFor(100, 100, 39)` → SULKY, `faceFor(100, 50, 40)` → HAPPY.
  - `hungry_beats_sulky`: `faceFor(0, 0, 0)` → HUNGRY.
  - `face_with_all_gauges_hidden_uses_mood_only`: `faceFor(null, null, 80)` → HAPPY, `faceFor(null, null, 10)` → SULKY. (Review Focus 1)
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*PetTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 시그니처와 "이 계획에서 정한 수치"로 구현한다.** 감속·조향 값이 `isFinite()`이고 0보다 클 때만 센다.
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 밥·건강 게이지와 기분 변화, 표정 결정 추가"`

---

### Task 3: 기분 상태, 시간 경과, 쿨다운, 대사, 저장 문자열

**Files:**
- Create: `SRC/core/PetState.kt`
- Test: `TEST/core/PetStateTest.kt`

**Interfaces:**
- Consumes: `Face` (Task 2)
- Produces:
```kotlin
data class PetState(
    val mood: Int = 70,
    val settledAtMs: Long = 0,      // 감소를 어디까지 반영했는지 가리키는 기준 시각 (스펙의 "마지막 돌봄 시각" 역할)
    val petReadyAtMs: Long = 0,
    val talkReadyAtMs: Long = 0,
)
data class TalkResult(val state: PetState, val reply: String?)

fun PetState.settle(nowMs: Long): PetState
fun PetState.applyDriving(delta: Int, nowMs: Long): PetState
fun PetState.pet(nowMs: Long): PetState
fun PetState.talk(nowMs: Long, face: Face): TalkResult
fun greeting(face: Face): String
fun driveStartLine(face: Face): String
fun PetState.encode(): String          // "mood,settledAtMs,petReadyAtMs,talkReadyAtMs"
fun decodePet(raw: String?): PetState  // 형식 오류·범위 밖·null이면 PetState()
```

규칙: `settle` = `settledAtMs <= 0`(처음 실행)이거나 `nowMs <= settledAtMs`(시계가 뒤로 감김)면 감소 없이 `settledAtMs = nowMs`. 아니면 `points = (nowMs - settledAtMs) / 1_800_000`, `mood = max(0, mood - points)`, `settledAtMs += points * 1_800_000` (30분 미만이면 기준 시각을 그대로 둬서 자주 열어도 감소가 사라지지 않게 한다). `applyDriving`/`pet`/`talk`는 먼저 `settle`한 뒤 적용하고 기분은 0~100으로 고정한다. `pet`은 `nowMs < petReadyAtMs`면 상태 변화 없음, 아니면 기분 +10, `petReadyAtMs = nowMs + 5000`. `talk`는 `nowMs < talkReadyAtMs`면 `reply = null`, 아니면 기분 +5, `talkReadyAtMs = nowMs + 5000`, `reply = greeting(face)`.
대사: `greeting` = 행복 "안녕! 오늘도 같이 달려요", 시무룩 "...안녕. 조금 시무룩해요", 배고픔 "안녕... 배고파요". `driveStartLine` = 행복 "출발이야? 좋아요!", 시무룩 "...출발해요?", 배고픔 "배고픈데... 출발해요?". 디코더는 필드 4개, 모두 정수, 음수 없음, `mood`가 0~100일 때만 받는다.

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `first_run_does_not_decay`: `PetState().settle(10_000_000_000)` → `mood == 70`, `settledAtMs == 10_000_000_000`. (Review Focus 2)
  - `decay_is_1_per_30_minutes_with_floor`: `PetState(70, 1_000)`을 `1_000 + 4 * 3_600_000`에 settle → 62. `PetState(5, 1_000)`을 24시간 뒤 settle → 0.
  - `clock_going_backwards_does_not_decay`: `PetState(70, 5_000_000).settle(1_000)` → `mood == 70`. (Review Focus 2)
  - `frequent_settles_do_not_lose_decay`: `PetState(70, 1_000)`을 29분 뒤 settle, 다시 정확히 60분 뒤 settle → 68.
  - `driving_delta_is_clamped_to_0_100`: 기분 98에 +5 → 100, 기분 3에 −5 → 0.
  - `pet_adds_10_and_sets_cooldown`: `PetState(50, 1_000).pet(2_000)` → 기분 60, `petReadyAtMs == 7_000`.
  - `pet_inside_cooldown_changes_nothing`: 위 결과를 `pet(3_000)` → 같은 상태. `pet(7_000)` → 기분 70. (Review Focus 4)
  - `talk_adds_5_and_replies_by_face`: `PetState(50, 1_000).talk(2_000, Face.HUNGRY)` → 기분 55, `reply == greeting(Face.HUNGRY)`.
  - `talk_inside_cooldown_has_no_reply_and_no_change`: 바로 `talk(3_000, ...)` → `reply == null`, 상태 동일. (Review Focus 4)
  - `lines_differ_by_face_and_are_never_empty`: 세 표정의 `greeting`·`driveStartLine`이 비어 있지 않고 표정마다 서로 다르다.
  - `encode_decode_roundtrip`: `PetState(63, 123_456, 7_000, 9_000)`.
  - `decode_garbage_returns_default`: `"abc"`, `""`, `null`, `"1,2,3"`, `"101,0,0,0"`, `"-1,0,0,0"`, `"50,-5,0,0"`, `"50,x,0,0"` → `PetState()`. (Review Focus 3)
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*PetStateTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 시그니처와 규칙으로 구현한다.**
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest` → 모두 PASS.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 기분 상태, 시간 경과, 쿨다운, 대사, 저장 문자열 추가"`

---

### Task 4: 저장소와 ViewModel

**Files:**
- Create: `SRC/data/PetStore.kt`, `SRC/ui/PetViewModel.kt`
- Test: `TEST/ui/PetViewModelTest.kt`

**Interfaces:**
- Consumes: `FakeSignalSource`, `Preset`, `DriveStyle`, `UxState`, `canPlay` (Task 1), `food`, `health`, `moodDelta`, `faceFor`, `Face` (Task 2), `PetState`, `settle`, `applyDriving`, `pet`, `talk`, `driveStartLine`, `encode`, `decodePet` (Task 3), `Narrator` (기존 `Speaker.kt`)
- Produces:
```kotlin
interface PetRepository {
    suspend fun load(): PetState
    suspend fun save(state: PetState)
}
class PetStore(context: Context) : PetRepository   // DataStore 키 하나에 encode() 문자열 저장, decodePet으로 읽기. corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }

class PetViewModel(
    source: FakeSignalSource,
    store: PetRepository,
    narrator: Narrator,
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    val pet: StateFlow<PetState>
    val food: StateFlow<Int?>
    val health: StateFlow<Int?>
    val face: StateFlow<Face>
    val ux: StateFlow<UxState>
    val line: StateFlow<String?>        // 마지막으로 한 말
    fun petIt()                         // canPlay(ux) 아니면 무시
    fun talk()                          // 주행 중에도 가능
    fun setParked(parked: Boolean)
    fun setFuel(percent: Int?)
    fun applyPreset(preset: Preset)
    fun startDrive(style: DriveStyle)
}
```

동작: 시작하면 저장소에서 읽고(`IOException`이면 `PetState()`), `settle(clock())`한 뒤 저장한다. 읽기가 끝나기 전에 들어온 동작은 읽기가 끝난 뒤에 처리한다(저장된 값을 덮어쓰지 않게). 읽은 뒤에 `source.state`를 구독해 `food`·`health`를 갱신하고 `moodDelta`가 0이 아니면 `applyDriving`으로 반영·저장한다. `face = faceFor(food, health, pet.mood)`. `ux`가 `PARKED → RESTRICTED`로 바뀌면(주행 시작) 마지막 자동 발화로부터 600000ms 이상일 때만 `narrator.speak(driveStartLine(face))`하고 `line`에 기록한다. 시작할 때의 초기 `RESTRICTED`는 주행 시작으로 치지 않는다. `talk()`는 `talk(clock(), face)` 결과의 `reply`가 있으면 `speak`하고 `line`에 기록한다. 저장 실패(`IOException`)는 삼키고 메모리 상태로 계속한다. `onCleared`에서 `narrator.shutdown()`.

- [ ] **Step 1: 실패하는 테스트 작성.** 테스트용 `FakeRepo`(저장 목록 기록, 읽기/쓰기 실패 스위치, 읽기를 막는 `CompletableDeferred` 게이트)와 `FakeNarrator`(말한 줄 기록), 바꿀 수 있는 `clock`을 쓰고, 이전 프로젝트의 `Dispatchers.setMain(StandardTestDispatcher())` + `runTest(dispatcher)` 패턴을 따른다.
  - `load_failure_falls_back_to_default_pet`: 읽기 실패 → `pet.value.mood == 70`, 크래시 없음. (Review Focus 3)
  - `save_failure_does_not_crash`: 쓰기 실패 상태에서 `setParked(true)`, `petIt()` → `pet.value.mood == 80`. (Review Focus 3)
  - `open_applies_time_decay`: 저장 상태 `PetState(70, 1_000, 0, 0)`, `clock = 1_000 + 4 * 3_600_000` → `pet.value.mood == 62`.
  - `actions_wait_for_initial_load`: 읽기를 게이트로 막고 `setParked(true)`, `petIt()` 호출 후 게이트를 열면 저장된 마지막 상태가 읽어온 상태 기반이다(저장된 `PetState(40, …)` → 마지막 저장 `mood == 50`).
  - `pet_is_ignored_while_restricted`: 기본 `RESTRICTED`에서 `petIt()` → 기분 그대로 70. (Review Focus 5)
  - `pet_raises_mood_when_parked_and_respects_cooldown`: `setParked(true)`, `petIt()`, `petIt()`(같은 clock) → 80, 한 번만 올라감. (Review Focus 4)
  - `talk_replies_while_driving`: 기본 `RESTRICTED`에서 `talk()` → `narrator.lines == [greeting(face)]`, 기분 75.
  - `talk_cooldown_blocks_repeat`: `talk()` 두 번(같은 clock) → 줄 1개.
  - `driving_start_greets_once_within_gap`: `setParked(true)`, `setParked(false)` → `[driveStartLine(...)]` 1줄. 다시 `setParked(true)`, `setParked(false)`(같은 clock) → 여전히 1줄. clock을 600000 늘린 뒤 다시 → 2줄. 처음부터 `RESTRICTED`인 것만으로는 말하지 않는다. (Review Focus 5)
  - `harsh_drive_lowers_mood_and_smooth_drive_raises_it`: `startDrive(HARSH)` 후 `pet.value.mood < 70`, 새 ViewModel에서 `startDrive(SMOOTH)` 후 `> 70`.
  - `low_fuel_shows_hungry_face`: `setFuel(10)` → `face.value == Face.HUNGRY`, `food.value == 10`.
  - `needs_care_shows_sulky_face`: `applyPreset(NEEDS_CARE)` → `health.value == 25`, `face.value == Face.SULKY`.
  - `unsupported_hides_gauges_and_keeps_going`: `applyPreset(UNSUPPORTED)` → `food.value == null`, `health.value == null`, `face.value == Face.HAPPY`(기분 70). (Review Focus 1)
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*PetViewModelTest*"` → 컴파일 실패.
- [ ] **Step 3: `PetRepository`/`PetStore`와 `PetViewModel`을 구현한다.** `PetStore`는 이전 `HeroStore`와 같은 DataStore 설정(파일 단위 delegate 하나, `applicationContext`)을 쓴다. 예전 `hero` 키는 읽지 않고 무시한다.
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest assembleDebug` → PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: 커밋.** `git add -A && git commit -m "feat: 저장소와 PetViewModel 추가 (돌봄 동작, 자동 인사, 저장 실패 대비)"`

---

### Task 5: 캐릭터 그리기, 화면, 에뮬레이터 확인

**Files:**
- Create: `SRC/ui/PetFace.kt`, `SRC/ui/PetScreen.kt`
- Modify: `SRC/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `PetViewModel` (Task 4), `Face`, `Preset`, `DriveStyle`, `UxState`, `canPlay`
- Produces:
```kotlin
@Composable fun PetFace(face: Face, modifier: Modifier = Modifier)   // Compose Canvas로 토끼 귀형 민트 자동차를 그림
@Composable fun PetScreen(vm: PetViewModel)
```
`PetFace`: `docs/superpowers/specs/assets/cute-car-mockup.html`의 토끼 귀형을 따른다(민트 몸통 #8fe3c8, 위로 솟은 귀 모양 사이드미러, 큰 눈과 하이라이트 2개, 분홍 볼터치, 바퀴 2개). 행복은 웃는 입과 쫑긋한 귀, 시무룩은 눈이 아래를 보고 눈썹이 처지며 볼터치가 흐리고 귀가 처짐, 배고픔은 입을 벌리고 땀방울.
`PetScreen`: 한 화면, 모드 둘. **주행 제한 모드**(`!canPlay(ux)`)는 `PetFace`, 마지막으로 한 말(`line`), 개발용 "말 걸기" 버튼과 "정차 중" 스위치만 보인다(개발 패널의 나머지는 숨김). **정차 모드**는 여기에 밥·건강·기분 게이지(막대와 숫자, 신호가 `null`인 게이지는 숨김), "쓰다듬기" 버튼, 개발 패널(프리셋 칩 4개, 연료 슬라이더, 주행 스타일 칩과 "주행 시작" 버튼)을 더한다. 개발 패널에는 `ponytail:` 주석으로 "실제 신호 빌드 전에 제거"를 남긴다.
`MainActivity`: `PetViewModel(FakeSignalSource(tickDelayMs = 150), PetStore(applicationContext), Speaker(applicationContext))`을 ViewModel 팩토리로 만든다. 화면은 `safeDrawingPadding`을 쓴다.

- [ ] **Step 1: `PetFace`, `PetScreen`, `MainActivity`를 구현한다.** 화면 로직은 ViewModel에 이미 있으므로 단위 테스트는 두지 않는다.
- [ ] **Step 2: 컴파일 확인.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS, BUILD SUCCESSFUL.
- [ ] **Step 3: 에뮬레이터에서 수동 확인.** AVD `demo`가 켜져 있지 않으면 `emulator -avd demo`로 띄우고 `./gradlew installDebug` 후 스크린샷이나 `uiautomator dump`로 확인한다.
  1. 처음 실행은 `HEALTHY`, 행복 표정, 게이지 100·100(연료 80)·70.
  2. `LOW_FUEL` → 배고픔 표정과 땀방울. `NEEDS_CARE` → 시무룩 표정과 처진 귀.
  3. `UNSUPPORTED` → 게이지가 숨고 크래시 없음.
  4. "주행 중"(제한 모드)에서 쓰다듬기 버튼이 없고 얼굴과 말풍선, 말 걸기만 보인다. 정차로 바꾸면 쓰다듬기가 보이고 누르면 기분이 오른다.
  5. 정차 → 주행 전환 시 TTS 인사 한 줄이 한 번만 나온다(10분 안에 다시 전환해도 반복하지 않음).
  6. 말 걸기를 누르면 대답이 나온다. 연타해도 5초 안에는 한 번만 반응한다.
  7. `HARSH` 주행 뒤 기분이 내려가고 `SMOOTH` 주행 뒤 올라간다.
  8. 앱을 강제 종료 후 다시 열어도 기분이 유지된다.
  확인하지 못한 항목(TTS 소리 등)은 그대로 보고한다.
- [ ] **Step 4: 커밋.** `git add -A && git commit -m "feat: 토끼 귀형 캐릭터 그리기와 돌봄 화면 추가"`

---

### Task 6: 다마고치 기기 스타일로 화면 개선

사용자 요청으로 추가된 Task. 로직은 건드리지 않고 겉모습만 바꾼다. 스펙의 "화면 스타일" 절을 따른다.

**Files:**
- Create: `SRC/ui/TamaDevice.kt`
- Test: `TEST/ui/TamaDeviceTest.kt`
- Modify: `SRC/ui/PetScreen.kt`, 필요하면 `SRC/ui/PetFace.kt`, `SRC/ui/MainActivity.kt`(배경)

**Interfaces:**
- Consumes: `PetViewModel`의 `pet`, `food`, `health`, `face`, `ux`, `line`, `petIt()`, `talk()` (Task 4), `PetFace` (Task 5), `canPlay`
- Produces:
```kotlin
@Composable fun TamaDevice(
    face: Face, line: String?,
    food: Int?, health: Int?, mood: Int,
    parked: Boolean,
    onPet: () -> Unit, onTalk: () -> Unit,
)
fun heartsFor(value: Int?): Int?   // null이면 null, 아니면 ceil(value/25)를 0..4로 고정한 채운 칸 수. 순수 함수
```

- [ ] **Step 1: `heartsFor`의 실패하는 테스트 작성.** `TEST/ui/TamaDeviceTest.kt`: `hearts_null_stays_null`(null → null), `hearts_round_up_in_quarters`(0 → 0, 1 → 1, 25 → 1, 26 → 2, 50 → 2, 51 → 3, 75 → 3, 76 → 4, 100 → 4), `hearts_clamp_out_of_range`(−5 → 0, 150 → 4). 실행해 실패 확인(컴파일 실패) → 구현 → 통과.
- [ ] **Step 2: 배경.** 앱 전체 배경을 어두운 남색 그라데이션(위 `#1d2333` → 아래 `#2b3350` 정도)으로 하고 별 같은 작은 점 패턴을 은은하게 깐다(Canvas, 고정 시드). 흰 배경이 남지 않게 `MainActivity`/`PetScreen`의 Surface 색도 맞춘다. 텍스트 색은 어두운 배경에서 읽히게 밝게 한다.
- [ ] **Step 3: `TamaDevice`.** 둥근 알 모양 파스텔 케이스(그라데이션), 안쪽 LCD 화면(옅은 크림/연두 계열, 안쪽 테두리·그림자, 도트 격자 느낌), LCD 안에 `PetFace`(몸통 색 대비가 낮으면 LCD 색을 조정), 화면 위쪽 아이콘 줄에 밥·건강·기분 아이콘과 하트 4칸(`heartsFor`, `null`인 항목은 아이콘째 숨김), 화면 아래쪽에 말풍선(`line`, 없으면 비움), 기기 아래에 둥근 버튼. 버튼은 `말 걸기`(항상 표시)와 `쓰다듬기`(`parked`일 때만 표시). 아이콘은 이모지나 Canvas로 그리고(외부 이미지·폰트 추가 금지), 하트 칸에는 `semantics`로 "밥 3/4칸" 같은 설명을 붙인다.
- [ ] **Step 4: `PetScreen` 재구성.** `TamaDevice`를 화면 중앙에 두고, 기존 숫자 막대 게이지를 없앤다(숫자 기분 값은 개발자 도구에 작게 표시). 개발용 컨트롤(프리셋 칩, 연료 슬라이더, 주행 스타일 칩과 "주행 시작", 정차 스위치)은 기기 아래 "개발자 도구" 영역으로 옮긴다. 주행 제한 모드에서는 기기(얼굴, 말풍선, 말 걸기 버튼)와 정차 스위치만 보인다. 정차 스위치의 라벨은 켜짐 "정차", 꺼짐 "주행 중"처럼 상태가 분명하게 읽히게 한다. 기존 동작(쓰다듬기는 정차에서만, 말 걸기는 항상, 슬라이더는 `food`와 동기화)은 그대로 유지한다. `ponytail:` 주석도 유지한다.
- [ ] **Step 5: 확인.** `./gradlew testDebugUnitTest assembleDebug` 전체 PASS. 에뮬레이터에서 스크린샷으로 확인: ① 흰 배경이 사라지고 남색 배경에 기기가 보인다 ② 행복·배고픔·시무룩 표정이 LCD 위에서 잘 보인다 ③ 하트 칸이 값과 맞는다(LOW_FUEL이면 밥 1칸, UNSUPPORTED면 밥·건강 아이콘 숨김) ④ 주행 제한 모드에는 쓰다듬기 버튼과 개발자 도구가 없다 ⑤ 정차 모드에는 쓰다듬기와 개발자 도구가 있고 쓰다듬기가 동작한다 ⑥ 앱 재시작 후에도 상태 유지. 보지 못한 항목은 그대로 보고한다.
- [ ] **Step 6: 커밋.** `git add -A && git commit -m "feat: 화면을 다마고치 기기 스타일로 개선"`

> Task 6의 알 모양 기기 겉모습은 사용자가 거절해 Task 7이 대체한다. 주행 중 하트 줄 숨김과 가로 화면 맞춤 같은 동작 요구는 유지한다.

---

### Task 7: "포근한 내 방" 스타일로 화면 교체

사용자가 "B. 포근한 내 방"(치이카와 포켓 홈 화면 느낌)을 골라서 추가된 Task. 로직은 건드리지 않고 겉모습만 바꾼다. 스펙의 "화면 스타일" 절과 목업 `docs/superpowers/specs/assets/chiikawa-room-mockup.html`의 B안을 따른다.

**Files:**
- Create: `SRC/ui/RoomScene.kt` (`heartsFor`를 `TamaDevice.kt`에서 옮겨 온다)
- Delete: `SRC/ui/TamaDevice.kt`
- Rename: `TEST/ui/TamaDeviceTest.kt` → `TEST/ui/RoomSceneTest.kt` (`git mv`, 테스트 내용은 그대로, 클래스 이름만 바꿈)
- Modify: `SRC/ui/PetScreen.kt`, `SRC/ui/PetFace.kt`(치이카와풍으로 다시 그림), 필요하면 `SRC/ui/MainActivity.kt`(시스템 바 색)

**Interfaces:**
- Consumes: `PetViewModel`의 `pet`, `food`, `health`, `face`, `ux`, `line`, `petIt()`, `talk()` (Task 4), `canPlay`
- Produces:
```kotlin
@Composable fun RoomScene(
    face: Face, line: String?,
    food: Int?, health: Int?,
    parked: Boolean,
    onPet: () -> Unit, onTalk: () -> Unit,
    modifier: Modifier = Modifier,
)
fun heartsFor(value: Int?): Int?   // 기존과 동일, 같은 테스트 통과
@Composable fun PetFace(face: Face, modifier: Modifier = Modifier)   // 시그니처 유지, 그림만 바뀜
```

- [ ] **Step 1: `heartsFor` 이동.** `heartsFor`를 `RoomScene.kt`로 옮기고 테스트 파일을 `RoomSceneTest.kt`로 `git mv` 한다. `./gradlew testDebugUnitTest --tests "*RoomSceneTest*"`가 이동 전과 같은 케이스로 통과해야 한다(이동만 하므로 새 실패 테스트는 없다).
- [ ] **Step 2: `PetFace`를 치이카와풍으로 다시 그린다.** 몸통 민트(#bfeedd), 얇은 갈색 외곽선(#6b5648, 약 3dp 비율), 위로 솟은 귀(시무룩이면 옆으로 처짐), 작은 점 눈, 옅은 분홍 볼터치, 작은 입, 흰 바퀴 두 개(외곽선). 표정: 행복(웃는 입), 시무룩(눈썹이 처지고 입꼬리 내림), 배고픔(입 벌림과 땀방울). 한국어 `contentDescription`과 시그니처는 유지한다.
- [ ] **Step 3: `RoomScene`.** 스펙 "화면 스타일"대로 그린다: 점무늬 크림 벽, 하늘색 창문, 나무 바닥, 분홍 러그, 러그 위의 `PetFace`, 캐릭터 위 말풍선(`line`, 없으면 비움), 벽 표지판(하트 4칸 `heartsFor`, 밥·건강·기분 아이콘, `parked`일 때만, `null` 항목 줄 숨김, "밥 3/4칸" 접근성 설명), 둥근 알약 버튼(`말 걸기` 항상, `쓰다듬기` `parked`일 때만, 터치 영역 48dp 이상). 외부 이미지·폰트·의존성 추가 금지, Canvas와 Compose 도형만 쓴다.
- [ ] **Step 4: `PetScreen` 재구성.** `TamaDevice` 자리에 `RoomScene`을 쓰고, 어두운 남색 배경과 별 점 패턴은 없앤다(화면 전체가 방이다). 개발자 도구와 정차 스위치는 방 아래에 두고 크림색 계열 카드로 맞춘다. 기존 동작(쓰다듬기 정차 전용, 말 걸기 항상, 슬라이더는 `food`와 동기화, 주행 제한 모드에서 하트 표지판·쓰다듬기·개발자 도구 숨김, `ponytail:` 주석)은 그대로 유지한다. 가로 화면에서는 주행 제한 모드의 방 전체와 말 걸기 버튼, 정차 스위치가 스크롤 없이 보이게 한다(2400x1080 에뮬레이터 기준 높이 약 360dp). 세로는 방이 화면 위쪽을 채우고 버튼과 개발자 도구가 아래에 온다. 시스템 바 아이콘 대비가 읽히게 한다.
- [ ] **Step 5: 확인.** `./gradlew testDebugUnitTest assembleDebug` 전체 PASS. 에뮬레이터 스크린샷으로 확인: ① 흰 배경과 알 모양 기기가 사라지고 방이 보인다 ② 행복·배고픔·시무룩 표정이 러그 위에서 잘 보이고 외곽선 스타일이 방과 어울린다 ③ 정차 모드에서 벽 표지판 하트가 값과 맞는다(LOW_FUEL이면 밥 1칸, UNSUPPORTED면 밥·건강 줄 숨김) ④ 주행 제한 모드에는 표지판, 쓰다듬기, 개발자 도구가 없다 ⑤ 정차 모드에서 쓰다듬기가 동작한다 ⑥ 가로(주행 제한 모드)에서 스크롤 없이 한 화면에 들어온다, 가로 정차 모드에서도 방이 위쪽에서 잘리지 않는다 ⑦ 에뮬레이터를 세로로 되돌려 놓는다. 보지 못한 항목은 그대로 보고한다.
- [ ] **Step 6: 커밋.** `git add -A && git commit -m "feat: 화면을 포근한 내 방 스타일로 교체"`
