# Drive Quest RPG Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 가짜 차량 신호로 주행을 점수화해 캐릭터를 키우고, 정차 상태에서 자동 전투하는 Android 앱.

**Architecture:** 게임 로직(점수, 성장, 전투, 주행 제한 게이트)은 Android 의존 없는 순수 Kotlin으로 두고 JVM 단위 테스트로 검증한다. 신호는 `VehicleSignalSource` 뒤에 숨기고 이번 범위에서는 `FakeSignalSource`만 구현한다. UI는 Compose, 저장은 DataStore.

**Tech Stack:** Kotlin 2.0.0, Jetpack Compose (BOM 2024.10.01), kotlinx-coroutines 1.9.0, DataStore Preferences 1.1.1, AGP 8.8.2, Gradle 8.10.2.

**Spec:** `docs/superpowers/specs/2026-10-22-drive-quest-rpg-design.md`

## Global Constraints

- minSdk 23, targetSdk 35, compileSdk 35, JDK 17.
- 패키지 `com.pleosplayground.drivequest`. 이하 `SRC = app/src/main/java/com/pleosplayground/drivequest`, `TEST = app/src/test/java/com/pleosplayground/drivequest`.
- 서버 없음, 저장은 기기 로컬(DataStore)만.
- 차량 제어(쓰기) 없음. 신호는 읽기 전용 모델.
- 신호가 `null`이면 미지원으로 보고 점수에서 건너뜀.
- 주행 제한(`UxState.RESTRICTED`)이면 전투와 육성 화면은 조작 불가, 읽기 전용 요약만 표시.
- **이번 범위는 가짜 신호만.** Pleos Vehicle SDK 의존성과 실제 연동 코드는 넣지 않음(사용자 결정). 속도는 점수에 쓰지 않음.
- 안티 목표: 과속이나 급가속이 보상이 되면 안 됨.

## Review Focus

각 줄의 테스트는 해당 Task에 포함됨.
1. 샘플이 0개인 빈 주행 → 점수 전부 0, 크래시 없음 (Task 3).
2. 모든 신호가 `null`(전부 미지원) → 점수 0, 크래시 없음 (Task 3).
3. 주행거리계가 줄어들거나 리셋 → 감소분 무시, 거리 음수 없음 (Task 3).
4. 감속도가 NaN이거나 음수 → 무시 (Task 3).
5. 영웅 HP가 0 이하일 때 전투 → 즉시 패배, 같은 seed는 같은 결과 (Task 5). 손상된 저장 문자열 → 기본 영웅으로 복구 (Task 4).

## File Structure

| 파일 | 책임 |
|---|---|
| `SRC/core/Sample.kt` | 신호 한 틱 데이터 모델 |
| `SRC/core/VehicleSignalSource.kt` | 신호 입력 인터페이스 |
| `SRC/core/FakeSignalSource.kt` | 시나리오 기반 가짜 신호 |
| `SRC/core/DriveScorer.kt` | 샘플 목록 → `TripScore` |
| `SRC/core/Hero.kt` | 영웅, 성장, 저장 문자열 변환 |
| `SRC/core/Battle.kt` | 몬스터, 주간 보스, 자동 전투 |
| `SRC/core/UxGate.kt` | 주행 제한 게이트, TTS 문구 |
| `SRC/data/HeroStore.kt` | DataStore 저장소 |
| `SRC/ui/GameViewModel.kt`, `SRC/ui/GameScreens.kt`, `SRC/ui/MainActivity.kt`, `SRC/ui/Speaker.kt` | 화면과 TTS |

---

### Task 1: 프로젝트 골격

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `.gitignore`
- Create: `SRC/core/Sample.kt`
- Test: `TEST/core/SampleTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class Sample(
    val tMs: Long,
    val brakeDecel: Float? = null,    // m/s², 제동 감속도
    val steeringRate: Float? = null,  // deg/s, 절댓값
    val turning: Boolean? = null,
    val turnSignalOn: Boolean? = null,
    val odometerKm: Float? = null,
    val ecoMode: Boolean? = null,
    val beltOn: Boolean? = null,
    val headlightOn: Boolean? = null,
    val wiperOn: Boolean? = null,
    val tireOk: Boolean? = null,
)
```

- [ ] **Step 1: `git init` 후 Gradle 설정 작성.** 위 Tech Stack 버전, Compose 컴파일러 플러그인 `org.jetbrains.kotlin.plugin.compose`, 테스트 의존성 JUnit4. 의존성은 compose BOM, activity-compose, coroutines-android, datastore-preferences, 테스트용 coroutines-test. Pleos 의존성은 넣지 않음.
- [ ] **Step 2: 실패하는 테스트 작성** — `SampleTest.default_sample_has_all_signals_unsupported`: `Sample(tMs = 0)`의 `brakeDecel`, `odometerKm`, `beltOn`이 모두 `null`.
- [ ] **Step 3: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*SampleTest*"` → 컴파일 실패(`Sample` 없음).
- [ ] **Step 4: `Sample.kt` 구현.**
- [ ] **Step 5: 테스트 통과 확인.** 같은 명령 → PASS.
- [ ] **Step 6: 커밋.** `git add -A && git commit -m "chore: scaffold android project and Sample model"`

---

### Task 2: 신호 입력과 가짜 시나리오

**Files:**
- Create: `SRC/core/VehicleSignalSource.kt`, `SRC/core/FakeSignalSource.kt`, `SRC/core/UxGate.kt`(`UxState` enum만 우선)
- Test: `TEST/core/FakeSignalSourceTest.kt`

**Interfaces:**
- Consumes: `Sample` (Task 1)
- Produces:
```kotlin
enum class UxState { PARKED, RESTRICTED }

interface VehicleSignalSource {
    val ux: StateFlow<UxState>
    fun trip(): Flow<Sample>
}

enum class Scenario { SMOOTH, AGGRESSIVE, RAINY_NIGHT, UNSUPPORTED }

class FakeSignalSource(
    private val scenario: Scenario,
    private val tickDelayMs: Long = 0,
) : VehicleSignalSource {
    fun setUx(state: UxState)
}
```

- [ ] **Step 1: 실패하는 테스트 작성** (`runTest` 사용).
  - `smooth_has_no_hard_brakes`: `SMOOTH` 샘플 중 `brakeDecel > 4.0f`인 것이 0개.
  - `aggressive_has_at_least_two_hard_brakes`: `AGGRESSIVE`에서 `brakeDecel > 4.0f`가 2개 이상.
  - `unsupported_has_all_null_signals`: `UNSUPPORTED`의 모든 샘플에서 `tMs`를 제외한 모든 필드가 `null`.
  - `rainy_night_sets_wiper_and_headlight`: `RAINY_NIGHT`에 `wiperOn == true`와 `headlightOn == true`인 샘플이 각각 존재.
  - `ux_defaults_to_restricted_and_follows_setUx`: 초기값 `RESTRICTED`(주행 중 가정), `setUx(PARKED)` 후 `PARKED`.
  - `odometer_is_non_decreasing_and_positive_at_end`: `UNSUPPORTED`를 제외한 시나리오의 `odometerKm`은 비감소이고 마지막 값이 0 초과.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*FakeSignalSourceTest*"` → 컴파일 실패.
- [ ] **Step 3: 구현.** 시나리오별 고정 샘플 리스트(20개 내외)를 `flow { }`로 방출하고 `tickDelayMs`만큼 `delay`. `ux`는 `MutableStateFlow(RESTRICTED)`.
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS.
- [ ] **Step 5: 커밋.** `git commit -m "feat: add VehicleSignalSource and FakeSignalSource scenarios"`

---

### Task 3: 주행 점수 계산

**Files:**
- Create: `SRC/core/DriveScorer.kt`
- Test: `TEST/core/DriveScorerTest.kt`

**Interfaces:**
- Consumes: `Sample`
- Produces:
```kotlin
enum class Flag { STORM, NIGHT, ECO }

data class TripScore(
    val xp: Int, val gold: Int, val km: Float,
    val hardBrakes: Int, val smoothBrakes: Int, val sharpSteers: Int,
    val bestCombo: Int, val flags: Set<Flag>,
)

fun score(samples: List<Sample>): TripScore
```

규칙(스펙 매핑에서 정한 수치, 이 Task가 값을 확정함):
- 거리 `km` = 양의 주행거리계 증가분의 합(감소/리셋은 무시). 거리 xp = `min(km * 10, 200)` 내림.
- 첫 번째 non-null `beltOn`이 `true`면 +20 xp.
- `brakeDecel`이 NaN이거나 0 이하면 무시. `> 4.0f`면 급제동 −5 xp, 아니면 부드러운 제동 +1 xp.
- `steeringRate > 120f`이면 급조향 −3 xp.
- `turning`이 false→true로 바뀌는 순간: `turnSignalOn == true`면 콤보 +1, +2 xp. 아니면 콤보 0으로 리셋. `bestCombo`는 최대값.
- 하나라도 `ecoMode == true`면 합산 xp에 ×1.2(반올림), `Flag.ECO`. `wiperOn == true`가 있으면 `STORM`, `headlightOn == true`가 있으면 `NIGHT`.
- xp는 0 미만으로 내려가지 않음. `gold = xp / 2` + (`tireOk == true`가 한 번이라도 있으면 5).

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `empty_trip_scores_zero`: `score(emptyList())` → xp 0, gold 0, km 0f, flags 비어 있음.
  - `all_null_signals_score_zero`: 모든 신호가 `null`인 샘플 5개 → 위와 동일.
  - `distance_xp_is_10_per_km_capped_at_200`: 주행거리계 0→10 → xp 100, km 10f. 0→50 → xp 200.
  - `odometer_decrease_is_ignored`: 값 `[5, 8, 2, 4]` → km 5f (3 + 2).
  - `belt_on_gives_20`: 거리 0, `beltOn` true → xp 20.
  - `hard_brake_costs_5_smooth_brake_gives_1`: 급제동 2회(5.0f), 부드러운 3회(2.0f) → hardBrakes 2, smoothBrakes 3, 합산 −10+3 → 0으로 바닥 처리, xp 0.
  - `nan_and_negative_decel_are_ignored`: `Float.NaN`, `-1f` → hardBrakes 0, smoothBrakes 0.
  - `signaled_turns_build_combo_and_unsignaled_resets`: 신호 있는 회전 3번, 신호 없는 회전 1번, 신호 있는 회전 1번 → bestCombo 3, xp 8 (신호 있는 4번 × 2).
  - `eco_multiplies_by_1_2`: 거리 10km + eco → xp 120, flags에 ECO.
  - `wiper_and_headlight_set_flags`: flags에 STORM, NIGHT.
  - `tire_ok_adds_5_gold`: `tireOk` true 한 번, xp 0 → gold 5.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*DriveScorerTest*"` → 컴파일 실패.
- [ ] **Step 3: `score` 구현.** 샘플을 순서대로 한 번 순회하며 누적. 서명과 위 규칙이 구현을 결정함.
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS.
- [ ] **Step 5: 커밋.** `git commit -m "feat: add trip scoring"`

---

### Task 4: 영웅 성장과 저장 문자열

**Files:**
- Create: `SRC/core/Hero.kt`
- Test: `TEST/core/HeroTest.kt`

**Interfaces:**
- Consumes: `TripScore`
- Produces:
```kotlin
data class Hero(
    val level: Int = 1, val xp: Int = 0,
    val hp: Int = 100, val atk: Int = 10, val def: Int = 5, val agi: Int = 5,
)
fun Hero.gain(score: TripScore): Hero
fun Hero.encode(): String
fun decodeHero(raw: String?): Hero   // 형식 오류·null이면 Hero()
```

규칙: 레벨업에 필요한 xp는 `100 * level`. 초과분은 이월, 한 번에 여러 레벨 가능. 레벨업마다 hp +10, atk +2, def +1, agi +1. 주행 보너스로 `def += smoothBrakes / 10`, `agi += bestCombo / 3`. 인코딩은 `"level,xp,hp,atk,def,agi"`.

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `gain_below_threshold_keeps_level`: `Hero().gain(xp = 99인 점수)` → level 1, xp 99.
  - `gain_levels_up_and_carries_over`: xp 130 → level 2, xp 30, hp 110, atk 12, def 6, agi 6.
  - `gain_can_skip_multiple_levels`: xp 300 → level 3, xp 0 (100 + 200).
  - `trip_bonus_adds_def_and_agi`: smoothBrakes 20, bestCombo 6 → def +2, agi +2 (레벨업 없는 경우).
  - `encode_decode_roundtrip`: `Hero(3, 40, 120, 14, 7, 7)`.
  - `decode_garbage_returns_default`: `"abc"`, `""`, `null`, `"1,2,3"` → `Hero()`.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*HeroTest*"`.
- [ ] **Step 3: 구현.**
- [ ] **Step 4: 통과 확인.**
- [ ] **Step 5: 커밋.** `git commit -m "feat: add hero progression and codec"`

---

### Task 5: 자동 전투와 주간 보스

**Files:**
- Create: `SRC/core/Battle.kt`
- Test: `TEST/core/BattleTest.kt`

**Interfaces:**
- Consumes: `Hero`
- Produces:
```kotlin
data class Monster(val name: String, val hp: Int, val atk: Int, val def: Int, val agi: Int)
data class BattleResult(val won: Boolean, val rounds: Int, val log: List<String>)

fun fight(hero: Hero, monster: Monster, seed: Long): BattleResult
fun weeklyBoss(weekIndex: Int): Monster   // hp = 200 + 50*weekIndex, atk = 12 + 2*weekIndex, def = 6 + weekIndex, agi = 6
fun bossChallenger(hero: Hero, weekXp: Int): Hero   // atk += weekXp / 50
```

규칙: `agi`가 높은 쪽이 먼저(동률이면 영웅). 피해 = `max(1, atk - def) + Random(seed).nextInt(0, 2)`. 최대 50라운드, 넘기면 패배. 영웅 `hp <= 0`이면 `rounds = 0`, 패배.

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `fresh_hero_beats_slime`: `Monster("슬라임", 30, 5, 0, 1)` → `won == true`.
  - `fresh_hero_loses_to_week0_boss`: `fight(Hero(), weeklyBoss(0), 1L).won == false`.
  - `boosted_hero_beats_week0_boss`: `bossChallenger(Hero(5, 0, 140, 18, 9, 9), weekXp = 400)`가 `weeklyBoss(0)`을 이김.
  - `same_seed_same_result`: 같은 인자로 두 번 호출하면 `BattleResult`가 같음.
  - `dead_hero_loses_immediately`: `Hero(hp = 0)` → `won == false`, `rounds == 0`.
  - `stalemate_is_a_loss_after_50_rounds`: `Monster("벽", hp = 100000, atk = 0, def = 100, agi = 0)`, 영웅 `atk = 10` → `won == false`, `rounds == 50`.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*BattleTest*"`.
- [ ] **Step 3: 구현.**
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 밸런스 테스트 두 개가 실패하면 위 공식 값은 그대로 두고 구현 버그부터 확인한 뒤, 그래도 안 맞으면 멈추고 사용자에게 보고.
- [ ] **Step 5: 커밋.** `git commit -m "feat: add auto battle and weekly boss"`

---

### Task 6: 주행 제한 게이트와 TTS 문구

**Files:**
- Modify: `SRC/core/UxGate.kt`
- Test: `TEST/core/UxGateTest.kt`

**Interfaces:**
- Consumes: `UxState`, `TripScore`
- Produces:
```kotlin
fun canPlay(ux: UxState): Boolean            // PARKED만 true
fun summaryLine(score: TripScore, leveledUp: Boolean): String
```
문구: 한국어 한 줄. 레벨업이면 "이번 주행으로 레벨업!"을 포함, 아니면 `"경험치 ${xp} 획득"`을 포함. xp가 0이면 "이번 주행에서 얻은 경험치가 없어요"를 반환.

- [ ] **Step 1: 실패하는 테스트 작성.**
  - `only_parked_can_play`: `canPlay(PARKED) == true`, `canPlay(RESTRICTED) == false`.
  - `summary_mentions_levelup`: `leveledUp = true` → 문자열에 "레벨업" 포함.
  - `summary_shows_xp_otherwise`: xp 42, `leveledUp = false` → "경험치 42" 포함.
  - `summary_for_empty_trip`: `score(emptyList())` → "얻은 경험치가 없어요" 포함.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*UxGateTest*"`.
- [ ] **Step 3: 구현.**
- [ ] **Step 4: 통과 확인.** 이어서 전체 `./gradlew testDebugUnitTest` → 모두 PASS.
- [ ] **Step 5: 커밋.** `git commit -m "feat: add driving restriction gate and summary line"`

---

### Task 7: 저장소, ViewModel, 화면, TTS

**Files:**
- Create: `SRC/data/HeroStore.kt`, `SRC/ui/GameViewModel.kt`, `SRC/ui/GameScreens.kt`, `SRC/ui/MainActivity.kt`, `SRC/ui/Speaker.kt`
- Modify: `app/src/main/AndroidManifest.xml` (런처 액티비티 등록)

**Interfaces:**
- Consumes: 위 모든 Task의 타입
- Produces:
```kotlin
class HeroStore(context: Context) {
    suspend fun load(): Hero          // decodeHero(저장 문자열)
    suspend fun save(hero: Hero)
}
class Speaker(context: Context) { fun speak(text: String); fun shutdown() }
class GameViewModel(source: VehicleSignalSource, store: HeroStore, speaker: Speaker) : ViewModel() {
    val hero: StateFlow<Hero>
    val lastScore: StateFlow<TripScore?>
    val lastBattle: StateFlow<BattleResult?>
    val ux: StateFlow<UxState>
    fun runTrip()          // trip() 수집 → score → gain → save → speaker.speak(summaryLine)
    fun battle()           // canPlay(ux) 아니면 아무것도 안 함
}
```
화면: 영웅 스탯 카드, "주행 시작(가짜 신호)" 버튼과 시나리오 선택, 정차/주행 전환 토글(개발용), 전투 버튼과 전투 로그. `ux == RESTRICTED`이면 전투 버튼 비활성화하고 "정차 후 이용할 수 있어요" 표시. 주행 요약 카드는 항상 읽기 전용으로 표시.

- [ ] **Step 1: `HeroStore` 구현.** DataStore Preferences 키 하나에 `Hero.encode()` 문자열 저장. 단위 테스트는 두지 않음(코덱은 Task 4에서 검증, 얇은 래퍼).
- [ ] **Step 2: `Speaker` 구현.** Android `TextToSpeech`, 한국어 로케일. 초기화 전 호출은 무시.
- [ ] **Step 3: `GameViewModel` 구현.** `runTrip()`은 `source.trip().toList()` 후 `score` → `hero.gain`. 레벨업 여부는 `level` 비교. 완료 시 `speak(summaryLine(...))`.
- [ ] **Step 4: 화면과 `MainActivity` 구현.** 시나리오와 UX 토글은 `FakeSignalSource`를 직접 쥐고 조작.
- [ ] **Step 5: 컴파일 확인.** `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6: 에뮬레이터에서 수동 확인.** 일반 Android 에뮬레이터(API 34)에서 `./gradlew installDebug` 후:
  1. 시나리오 SMOOTH로 주행 시작 → 경험치 증가, 음성 안내.
  2. 주행 상태(RESTRICTED)에서 전투 버튼이 비활성화됨.
  3. 정차 토글 후 전투 실행 → 로그 표시.
  4. 앱 재시작 후 영웅 상태 유지.
  5. UNSUPPORTED 시나리오 → 경험치 0, 크래시 없음.
- [ ] **Step 7: 커밋.** `git commit -m "feat: add store, view model, screens and speaker"`
