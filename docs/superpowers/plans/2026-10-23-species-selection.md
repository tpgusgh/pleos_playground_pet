# 캐릭터 6종과 처음 고르기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 토끼 한 종이던 캐릭터를 6종(토끼, 고양이, 곰, 강아지, 여우, 오리)으로 늘리고, 처음 정차했을 때 고르기 화면에서 친구를 골라 시작하게 한다. 고른 종에 따라 모습, 정해진 대사, 호출어, LLM 성격 설명이 달라진다.

**Architecture:** 종 표와 종별 대사, 저장된 id 해석은 Android 의존 없는 순수 Kotlin(`core/Species.kt`)에 두고 JVM 단위 테스트한다. 토끼의 기존 대사 함수(`greeting`, `driveStartLine`, `notUnderstoodLine`)는 그대로 두고 새 함수 `lineFor(species, kind, face)`가 토끼일 때 그 함수들을 부른다. 저장은 기존 DataStore 파일의 별도 키 `species`, `PetViewModel`이 고른 종을 들고 대사와 고르기 화면 표시 여부를 정한다. `PetFace`는 종을 받아 팔레트, 귀·머리 장식, 안테나 장식, 코·얼굴 특징만 바꿔 그린다(몸통, 표정, 연출, 움직임은 모든 종 공통).

**Tech Stack:** 기존 프로젝트 그대로 (Kotlin 2.0.0, Compose BOM 2024.10.01, coroutines 1.9.0, DataStore Preferences 1.1.1). 새 의존성 없음.

**Spec:** `docs/superpowers/specs/2026-10-22-species-selection-design.md` (기반: `docs/superpowers/specs/2026-10-22-car-tamagotchi-design.md`)

## Global Constraints

- 기존 프로젝트를 이어서 쓴다: minSdk 23, targetSdk 35, JDK 17, 패키지 `com.pleosplayground.drivequest`. Gradle은 항상 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`로 실행한다.
- 이하 `SRC = app/src/main/java/com/pleosplayground/drivequest`, `TEST = app/src/test/java/com/pleosplayground/drivequest`.
- `core/`는 순수 Kotlin이다. `core/Species.kt`에 Android·Compose import를 넣지 않는다(색은 ARGB `Long`, Compose `Color`로의 변환은 `ui/`에서).
- 새 의존성, 새 이미지·폰트 파일을 추가하지 않는다. 그림은 지금처럼 Compose Canvas로만 그린다.
- 게이지·규칙·쿨다운은 모든 종이 같다. 종마다 다른 능력치, 해금, 여러 마리 키우기는 하지 않는다.
- 주행 중(`UxState.RESTRICTED`)에는 고르기 화면을 띄우지 않고 고르기·바꾸기 요청도 무시한다.
- 죄책감을 주거나 재촉하는 대사는 쓰지 않는다. 대사는 아래 표의 문장을 그대로 쓴다(구현자가 새로 짓지 않는다).
- 기존 테스트 115개는 수정 없이 그대로 통과해야 한다(`TestFakes.kt`의 `FakeRepo`에 새 기능을 더하는 것만 허용).
- 커밋: 메시지는 한글(타입 접두사는 영어), 마지막 줄은 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. 예: `git add -A && git commit -m "feat: ..." -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"`.
- 저장소는 GitHub 공개 저장소다. 커밋은 저장소의 기본 git 사용자(`tpgusgh`)로 한다. `git -c user.name=... -c user.email=...`나 `--author`를 쓰지 않는다. 어떤 Task에서도 push하지 않는다.
- 스크린샷과 UI 덤프는 저장소 밖(`$TMPDIR/species-shots/`)에 저장하고 커밋하지 않는다.
- **에뮬레이터는 Task 3~6의 화면 확인 단계에서만 켠다**(사용자 요청: 리소스를 많이 쓴다). Task 1, 2, 7은 에뮬레이터 없이 JVM 테스트와 `assembleDebug`로만 확인한다. 켠 Task는 끝날 때 반드시 끄고 꺼진 것을 확인한다.

### 에뮬레이터 확인 절차 (Task 3~6 공통)

```bash
ADB=$HOME/Library/Android/sdk/platform-tools/adb
mkdir -p $TMPDIR/species-shots
# 켜기 (백그라운드로 실행)
$HOME/Library/Android/sdk/emulator/emulator -avd demo -no-snapshot &
$ADB wait-for-device
until [ "$($ADB shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
# 설치와 실행
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew installDebug
$ADB shell pm clear com.pleosplayground.drivequest        # 처음 상태가 필요할 때만 (고르기 화면은 처음 상태에서만 뜬다)
$ADB shell am start -n com.pleosplayground.drivequest/.ui.MainActivity
sleep 5                                                    # 시작 직후 약 4초는 스플래시가 보인다
# 화면 찍기와 누를 위치 찾기
$ADB exec-out screencap -p > $TMPDIR/species-shots/<이름>.png
$ADB shell uiautomator dump /sdcard/ui.xml && $ADB shell cat /sdcard/ui.xml   # 글자의 bounds로 좌표를 찾는다
$ADB shell input tap <x> <y>
# 가로로 볼 때 / 세로로 되돌릴 때
$ADB shell settings put system accelerometer_rotation 0 && $ADB shell settings put system user_rotation 1
$ADB shell settings put system user_rotation 0
# 끄기 (Task의 마지막 단계, 실패한 경우에도)
$ADB emu kill
sleep 5; pgrep -fl qemu || echo "emulator stopped"         # qemu 프로세스가 남아 있으면 다시 확인하고 보고한다
```

- 개발자 도구, 쓰다듬기, "친구 바꾸기"는 "정차" 스위치를 켜야 보인다. 표정은 개발자 도구 프리셋으로 바꾼다: `HEALTHY` → 행복, `NEEDS_CARE` → 시무룩, `LOW_FUEL` → 배고픔.
- 찍은 스크린샷은 Read 도구로 직접 보고 판단한다. 보지 못한 항목은 보고서에 그대로 적는다.

## 이 계획에서 정한 값 (스펙이 빌더에게 맡긴 것 + 내 판단)

### 종 표 (`core/Species.kt`, 순서도 이대로)

| enum | `id` | `displayName` | `callName` | `ears` | `charm` | `tagline` (카드 한 줄) | `personality` (LLM용, 스펙 문구 그대로) |
|---|---|---|---|---|---|---|---|
| `RABBIT` | `rabbit` | 토끼 | 토끼야 | `BUNNY` | `HEART` | 다정하고 조금 수줍어요 | 다정하고 조금 수줍음. 부드러운 존댓말 |
| `CAT` | `cat` | 고양이 | 냥이야 | `CAT` | `STAR` | 도도한데 은근 애교쟁이 | 도도한데 은근 애교. 가끔 "냥" |
| `BEAR` | `bear` | 곰 | 곰돌아 | `BEAR` | `HONEY` | 느긋하고 든든해요 | 느긋하고 든든. 천천히 말하고 먹는 얘기를 좋아함 |
| `DOG` | `dog` | 강아지 | 멍이야 | `DOG` | `BONE` | 활발하고 늘 신나요 | 활발하고 신남. 느낌표가 많음 |
| `FOX` | `fox` | 여우 | 여우야 | `FOX` | `FLAME` | 영리하고 장난꾸러기 | 영리하고 장난기 많음 |
| `DUCK` | `duck` | 오리 | 오리야 | `DUCK` | `DROP` | 엉뚱하고 순해요 | 엉뚱하고 순함. 짧게 말함 |

### 종별 색 (ARGB `Long`, 외곽선은 모든 종 `#6B5648` 그대로)

| 종 | `body` 몸통 | `shade` 몸통 그늘 | `ear` 귀·머리 장식 | `inner` 귀 안쪽·여우 귀 끝·오리 부리 |
|---|---|---|---|---|
| 토끼 | `0xFFBFEEDD` | `0xFF9EDCC5` | `0xFFA8E3CC` | `0xFFFFC9D6` |
| 고양이 | `0xFFFFD6E0` | `0xFFF5B7C9` | `0xFFFFC7D5` | `0xFFFFF6E2` |
| 곰 | `0xFFE9CDA8` | `0xFFD6B089` | `0xFFDDBB93` | `0xFFFFF6E2` |
| 강아지 | `0xFFC9E4F8` | `0xFFA7CEEE` | `0xFFA9CBEA` | `0xFFFFF6E2` |
| 여우 | `0xFFFFD2A8` | `0xFFF6B37F` | `0xFFFFC28E` | `0xFF8C5E4A` |
| 오리 | `0xFFFFF0A6` | `0xFFF2D676` | `0xFFF9E08A` | `0xFFFFB46B` |

토끼 네 값은 지금 `PetFace.kt`의 `Mint`, `MintShade`, `EarFill`, `PinkGlass`와 같다(토끼 그림이 바뀌지 않는 근거).

### 종별 대사 (모든 문장 그대로, 표정 순서 행복 / 시무룩 / 배고픔)

토끼 줄은 기존 함수의 문장이다. 토끼는 기존 함수를 그대로 부르므로 문장을 다시 입력하지 않는다(`음…`의 `…`는 U+2026).

| 종 | 종류 | `HAPPY` | `SULKY` | `HUNGRY` |
|---|---|---|---|---|
| 토끼 | 인사 `GREETING` | 안녕! 오늘도 같이 달려요 | ...안녕. 조금 시무룩해요 | 안녕... 배고파요 |
| 토끼 | 주행 시작 `DRIVE_START` | 출발이야? 좋아요! | ...출발해요? | 배고픈데... 출발해요? |
| 토끼 | 못 알아들음 `NOT_UNDERSTOOD` | 음… 잘 모르겠어요 | ...잘 모르겠어요 | 배고파서 잘 모르겠어요... |
| 고양이 | 인사 | 왔냥? 조금은 반갑다냥 | ...흥. 오늘은 별로다냥 | 배고프다냥... |
| 고양이 | 주행 시작 | 출발? 같이 가 줄게냥 | ...출발이냥. | 배고픈데 출발이냥... |
| 고양이 | 못 알아들음 | 음? 무슨 말이냥 | ...모르겠다냥 | 배고파서 모르겠다냥... |
| 곰 | 인사 | 안녀엉~ 오늘도 든든하게 가자 | 음... 오늘은 조금 처지네 | 꿀 생각이 나네... 배고파 |
| 곰 | 주행 시작 | 천천히, 든든하게 출발~ | 음... 출발하는구나 | 배고프지만... 출발~ |
| 곰 | 못 알아들음 | 음... 잘 모르겠네~ | 음... 모르겠어 | 배고파서... 생각이 안 나 |
| 강아지 | 인사 | 안녕! 안녕! 너무 좋아! | 안녕... 오늘은 기운이 없어 | 배고파! 그래도 안녕! |
| 강아지 | 주행 시작 | 출발이다! 신난다! | 출발... 힘내 볼게! | 배고프지만 출발! |
| 강아지 | 못 알아들음 | 앗! 잘 모르겠어! | 으응... 모르겠어 | 배고파서 모르겠어! |
| 여우 | 인사 | 안녕~ 오늘은 뭐 하고 놀까? | ...오늘은 장난칠 기분 아냐 | 배고파서 꾀가 안 나와 |
| 여우 | 주행 시작 | 출발! 재밌는 일 생기겠다 | ...출발이구나 | 배고픈데... 출발이야? |
| 여우 | 못 알아들음 | 흠, 수수께끼인걸? | ...잘 모르겠어 | 배고파서 머리가 안 돌아 |
| 오리 | 인사 | 꽥! 안녕! | ...꽥. 시무룩 | 꽥... 배고파 |
| 오리 | 주행 시작 | 꽥! 출발! | ...출발, 꽥 | 배고파... 출발? |
| 오리 | 못 알아들음 | 꽥? 몰라! | ...몰라, 꽥 | 배고파서 몰라... |

알림 문장(`alertLine`)과 차량 질문의 답(`answerFor`)은 종과 무관하게 지금 문장 그대로다.

### 동작 규칙

| 항목 | 값 |
|---|---|
| 저장 | 기존 DataStore 파일 `drivequest`의 새 키 `species`(문자열)에 `Species.id`. 기존 `pet` 키는 그대로 |
| 저장된 값 해석 | `speciesFromId`: 표의 `id`와 정확히 같을 때만 그 종. `null`, 빈 문자열, 모르는 값, 대소문자·공백이 다른 값은 `null`("안 고름") |
| 안 고른 동안 | 실제 동작·대사·그림은 토끼. `speciesChosen = false` |
| 저장소 실패 | 종 읽기에서 `IOException` → "안 고름"(토끼, 정차하면 고르기 화면). 종 쓰기에서 `IOException` → 무시하고 고른 종은 이번 실행 동안 유지(다음 실행에서 다시 고르기 화면이 뜰 수 있다, `ponytail:` 주석으로 남김) |
| 고르기 화면 표시 | `needsPicker = 종 읽기가 끝났음 && 안 고름 && ux == PARKED`. 읽기가 끝나기 전에는 항상 `false` |
| `chooseSpecies(id)` | `act` 안에서: 주행 중이면 무시, 모르는 id면 무시, **이미 골랐으면 무시**(확정 두 번 누르기 방지). 아니면 종을 정하고 새 `PetState(settledAtMs = clock())`로 초기화, 말풍선(`line`)을 `null`로, 종 저장, 기분 저장 |
| `changeSpecies(id)` | `act` 안에서: 주행 중이면 무시, 모르는 id면 무시, **지금 종과 같으면 무시**. 아니면 `chooseSpecies`와 같은 초기화·저장 |
| 초기화 후 | 기분 70, 쿨다운 없음(바로 말 걸기 가능), 주행 시작 인사의 10분 간격 기록(`lastGreetMs`)과 알림 상태는 그대로 둔다 |
| 대사 선택 | 말 걸기·주행 시작 인사·못 알아들음 모두 `lineFor(species.value, kind, 지금 표정)`. 말 걸기의 표정은 기분이 오르기 **전** 표정(지금 `talk()`와 같다) |
| 접근성 설명 | `petDescription(face, species)` = "행복한 {이름} 캐릭터", "시무룩한 {이름} 캐릭터", "배고픈 {이름} 캐릭터" (예: "행복한 곰 캐릭터"). 지금의 "…차 캐릭터"를 대체한다 |
| 바꾸기 확인 대화상자 | 제목 "친구를 바꿀까요?", 본문 "기분이 처음부터 다시 시작돼요", 확인 "바꾸기", 취소 "취소" |

### 고르기 화면 (`ui/SpeciesPicker.kt`)

| 항목 | 값 |
|---|---|
| 자리 | `PetScreen`의 방(`RoomScene`)과 개발자 도구 자리를 대신한다. "정차" 스위치 줄은 그대로 아래에 보인다 |
| 제목 | "함께 달릴 친구를 골라 주세요" (`Line` 색, 굵게, 20sp) |
| 카드 | `FlowRow`(가운데 정렬, 간격 12dp), 카드 너비 140dp, 배경 `0xFFFFF7E8`, 테두리 3dp `Line`, 모서리 22dp. 안에 `PetFace(Face.HAPPY, 종, Modifier.height(72.dp))`, 이름(16sp 굵게), `tagline`(12sp). 고른 카드는 배경 `0xFFFFE8A3`. 카드 전체가 `Role.Button`이고 `semantics(mergeDescendants = true)`, `selected` 상태를 가진다 |
| 크게 보기 | 고른 종이 있으면 제목 아래에 `PetFace(Face.HAPPY, 고른 종, Modifier.height(160.dp))`와 "{이름} · {tagline}" |
| 확정 버튼 | `PillButton("이 아이로 시작", Pink)`. **고른 종이 있을 때만 보인다**(고르지 않고 확정할 수 없다) |
| 닫기 | 바꾸기 모드에서만 `PillButton("닫기", Yellow)`. 처음 고르기에는 닫기가 없다 |
| 선택 상태 | `rememberSaveable`에 종 `id` 문자열로 둔다. 바꾸기 모드는 지금 종이 미리 선택된 채 열린다 |

### 종별 그림 (`ui/PetFace.kt`, 설계 단위는 지금과 같은 200x200 상자)

| `EarStyle` | 귀·머리 장식 | 각도 (`ear`는 지금 값: 행복 10, 배고픔 30, 시무룩 118) | 코·얼굴 특징 |
|---|---|---|---|
| `BUNNY` | 지금 `mirrorEar` 그대로(색만 `ear`/`inner`) | `ear` × 1 (지금 그대로) | 지금 작은 코(5 x 3.6) |
| `CAT` | 둥근 끝 삼각형, 밑변 30이 (px ± 15, 56)에, 끝은 (px에서 바깥쪽 6, 20). 안쪽 작은 삼각형 `inner` | `ear` × 0.3 | 둥근 삼각형 코(폭 6, 중심 (100, 121)), 양쪽 수염 3가닥(`Fine`, 볼 쪽 x 74 → 56, y 124 / 129 / 134에서 바깥으로 살짝 벌어짐) |
| `BEAR` | 반지름 13 원, 중심 (px에서 바깥쪽 4, 42). 안쪽 반지름 7 원 `inner` | `ear` × 0.1 | 타원 코 9 x 6, 중심 (100, 122) |
| `DOG` | 처진 귀: 22 x 46 물방울 모양이 (px에서 바깥쪽 10, 44)에 매달려 차체 옆으로 늘어짐. 색 `ear` | 들림 `perk = ((118 − ear) / 108).coerceIn(0, 1)`, 귀를 바깥 위로 `45° × perk` 들어 올림(행복이면 들리고 시무룩이면 처짐) | 타원 코 10 x 7, 중심 (100, 122), 흰 점 하이라이트 |
| `FOX` | `CAT`보다 길고 좁은 삼각형(밑변 28, 끝 y 12). 귀 위쪽 35%를 `inner`(진한 갈색)로 칠함(`clipPath`), 안쪽 삼각형은 `0xFFFFF6E2` | `ear` × 0.3 | `CAT`과 같은 코, 수염 없음 |
| `DUCK` | 귀 없음. 지붕 왼쪽 (86, 38)에 작은 깃털 3개(물방울 모양, 높이 14 / 12 / 10, 기울기 −25° / −5° / +15°), 색 `ear` | 흔들림(`sway`)만 | 크림 입마개 자리를 `inner`(주황 부리)로 칠하고 코 대신 콧구멍 점 2개(반지름 1.2, (96, 122)와 (104, 122)) |

- 왼쪽 귀는 오른쪽 귀 모양을 x = px 기준으로 좌우 뒤집어 그린다. 지금처럼 오른쪽 귀에만 `earTwitch`를 더한다.
- 안테나 장식(중심 (100, 14), 크기 약 16 x 16, 외곽선 `Fine`): `HEART` 지금 하트 `0xFFFF8FA8` / `STAR` 다섯 꼭짓점 별 `0xFFFFE08A` / `HONEY` 꿀방울(`drop` 모양 높이 14) `0xFFFFC94D` / `BONE` 뼈다귀(16 x 6 둥근 막대와 양끝 원 4개) `0xFFFFF6E2` / `FLAME` 불꽃(바깥 `0xFFFF9E6B`, 안쪽 작은 불꽃 `0xFFFFE08A`) / `DROP` 물방울(`drop` 모양 높이 14) `0xFFBFE6FF`.
- 숨쉬기·깜빡임·표정·연출(별, 먹구름, 꼬르륵), 바퀴, 번호판 하트, 볼터치는 모든 종 공통이다.

## Review Focus

각 줄의 테스트나 확인은 해당 Task에 포함됨.
1. 주행 중에는 고르기 화면이 뜨지 않고 `chooseSpecies`·`changeSpecies`도 무시된다. 안 고른 채 주행하면 토끼로 말한다 (Task 2, Task 4 화면 확인).
2. 확정을 두 번 누르면 첫 선택만 적용·저장된다 (Task 2).
3. 저장된 id가 깨졌거나 모르는 값이면 "안 고름"으로 토끼가 되고 크래시하지 않는다 (Task 1, Task 2).
4. 종 읽기 실패는 토끼로, 종 쓰기 실패는 고른 종 유지로 계속 동작한다 (Task 2).
5. 저장된 종을 읽기 전에 고르기 화면이 잠깐 뜨지 않는다 (Task 2).
6. 고른 직후 주행을 시작하면 주행 시작 인사가 새 종의 문장이다 (Task 2).
7. 중간에 바꾸면 호출어, 대사, 말풍선이 한 번에 바뀌고 기분이 70으로 돌아간다 (Task 2).
8. 토끼 대사와 색은 지금과 바이트 단위로 같고 기존 테스트 115개가 그대로 통과한다 (Task 1, 모든 Task의 전체 테스트).
9. 토끼 그림은 바뀌기 전과 같은 모습이다 (Task 3 전후 스크린샷).

## File Structure

| 파일 | 책임 |
|---|---|
| `SRC/core/Species.kt` | `Species`, `EarStyle`, `Charm`, `LineKind`, `speciesFromId`, `lineFor`, `petDescription`, 5종 대사 표 (신규) |
| `SRC/data/PetStore.kt` | `PetRepository`에 종 읽기·쓰기 추가, `species` 키 (수정) |
| `SRC/ui/PetViewModel.kt` | 종 상태, 고르기·바꾸기, 종별 대사 사용 (수정) |
| `SRC/ui/PetFace.kt` | 종 매개변수, 팔레트, 귀·장식·코 (수정) |
| `SRC/ui/RoomScene.kt` | `species` 전달, `PillButton`을 `internal`로 (수정) |
| `SRC/ui/SpeciesPicker.kt` | 고르기 화면 (신규) |
| `SRC/ui/PetScreen.kt` | 고르기 화면 표시, "친구 바꾸기"와 확인 대화상자 (수정) |
| `TEST/core/SpeciesTest.kt` | 종 표·대사·id 해석 (신규) |
| `TEST/ui/TestFakes.kt` | `FakeRepo`에 종 저장 추가 (수정) |
| `TEST/ui/PetViewModelSpeciesTest.kt` | ViewModel 종 흐름 (신규) |
| `README.md` | 기능, 구조, 테스트 수, 할 일 (수정) |

---

### Task 1: 종 표와 종별 대사

**Files:**
- Create: `SRC/core/Species.kt`
- Test: `TEST/core/SpeciesTest.kt`

**Interfaces:**
- Consumes: `Face`, 기존 `greeting(face)`, `driveStartLine(face)` (`core/PetState.kt`), `notUnderstoodLine(face)` (`core/CarQuestions.kt`). 이 세 함수와 문장은 바꾸지 않는다.
- Produces:
```kotlin
enum class EarStyle { BUNNY, CAT, BEAR, DOG, FOX, DUCK }
enum class Charm { HEART, STAR, HONEY, BONE, FLAME, DROP }
enum class LineKind { GREETING, DRIVE_START, NOT_UNDERSTOOD }

enum class Species(
    val id: String,
    val displayName: String,
    val callName: String,
    val body: Long, val shade: Long, val ear: Long, val inner: Long, // ARGB
    val ears: EarStyle,
    val charm: Charm,
    val tagline: String,
    val personality: String,
) { RABBIT, CAT, BEAR, DOG, FOX, DUCK }   // 값은 "종 표"와 "종별 색" 표 그대로

fun speciesFromId(id: String?): Species?
fun lineFor(species: Species, kind: LineKind, face: Face): String
fun petDescription(face: Face, species: Species): String
```
`lineFor`는 `RABBIT`이면 기존 세 함수를 부르고, 나머지 5종은 이 파일 안의 비공개 표(형태는 자유: 중첩 `when`이나 `Map`)에서 "종별 대사" 표의 문장을 돌려준다. `callName`과 `personality`는 지금 쓰는 곳이 없다(항상 듣기·LLM 계획에서 쓴다).

- [ ] **Step 1: 실패하는 테스트 작성** (`SpeciesTest`).
  - `six_species_in_order_with_distinct_values`: `Species.entries.map { it.id }` == `["rabbit", "cat", "bear", "dog", "fox", "duck"]`, `displayName` == `["토끼", "고양이", "곰", "강아지", "여우", "오리"]`, `callName` == `["토끼야", "냥이야", "곰돌아", "멍이야", "여우야", "오리야"]`, `body` 6개가 서로 다름, `ears`와 `charm`도 각각 6개가 서로 다름.
  - `rabbit_colors_are_the_current_drawing_colors`: `RABBIT.body == 0xFFBFEEDD`, `shade == 0xFF9EDCC5`, `ear == 0xFFA8E3CC`, `inner == 0xFFFFC9D6`. (Review Focus 8)
  - `rabbit_lines_are_the_existing_lines`: 모든 `Face`에 대해 `lineFor(RABBIT, GREETING, f) == greeting(f)`, `DRIVE_START`는 `driveStartLine(f)`, `NOT_UNDERSTOOD`는 `notUnderstoodLine(f)`. 그리고 `lineFor(RABBIT, GREETING, HAPPY) == "안녕! 오늘도 같이 달려요"`. (Review Focus 8)
  - `species_lines_match_the_table`: 테스트 안에 "종별 대사" 표의 고양이~오리 45문장을 그대로 적고 `lineFor`와 하나씩 비교한다.
  - `every_line_is_short_and_distinct_per_face`: 6종 × 3종류마다 세 표정의 문장이 비어 있지 않고 길이가 20자 이하이며 서로 다르다. 6종마다 `GREETING/HAPPY != DRIVE_START/HAPPY`.
  - `species_from_id_decodes_only_exact_ids`: 6개 id는 각 종으로, `null`, `""`, `"dragon"`, `"CAT"`, `" cat"`, `"cat "`는 `null`. (Review Focus 3)
  - `description_names_face_and_species`: `petDescription(HAPPY, BEAR) == "행복한 곰 캐릭터"`, `petDescription(SULKY, CAT) == "시무룩한 고양이 캐릭터"`, `petDescription(HUNGRY, RABBIT) == "배고픈 토끼 캐릭터"`.
- [ ] **Step 2: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*SpeciesTest*"` → 컴파일 실패.
- [ ] **Step 3: 위 Interfaces와 표대로 구현한다.**
- [ ] **Step 4: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest` → PASS(기존 115개 포함).
- [ ] **Step 5: 커밋.** `feat: 캐릭터 6종 표와 종별 대사 추가`

---

### Task 2: 종 저장과 ViewModel 연결

**Files:**
- Modify: `SRC/data/PetStore.kt`, `SRC/ui/PetViewModel.kt`, `TEST/ui/TestFakes.kt`
- Test: `TEST/ui/PetViewModelSpeciesTest.kt` (신규)

**Interfaces:**
- Consumes: `Species`, `LineKind`, `lineFor`, `speciesFromId` (Task 1), 기존 `PetState.talk`, `act`, `persist`, `canPlay`
- Produces:
```kotlin
// PetRepository에 추가 (새 인터페이스를 만들지 않는다: 구현이 PetStore 하나뿐)
suspend fun loadSpecies(): Species?          // 안 골랐거나 모르는 값이면 null
suspend fun saveSpecies(species: Species)

// PetStore: SPECIES_KEY = stringPreferencesKey("species")
//   loadSpecies() = speciesFromId(store.data.first()[SPECIES_KEY]); saveSpecies는 species.id를 쓴다

// PetViewModel에 추가
val species: StateFlow<Species>              // 실제 종, 안 골랐으면 RABBIT
val speciesChosen: StateFlow<Boolean>
val needsPicker: StateFlow<Boolean>          // 종 읽기 끝남 && 안 고름 && PARKED
fun chooseSpecies(id: String)
fun changeSpecies(id: String)

// TestFakes.kt의 FakeRepo에 추가 (기존 인자와 기본값은 그대로)
//   생성자 인자 initialSpecies: Species? = null
//   val savedSpecies = mutableListOf<Species>()
//   loadSpecies(): gate를 기다리고, failLoad면 IOException, 아니면 initialSpecies
//   saveSpecies(s): savedSpecies에 넣은 뒤 failSave면 IOException
```
동작 (표 "동작 규칙" 그대로):
- 첫 읽기(`loaded` 작업) 안에서 기분보다 먼저 종을 읽는다. `IOException`이면 `null`. 끝나면 "종 읽기 끝남" 표시를 켠다.
- `talk()`: 지금 표정을 먼저 구하고 `_pet.value.talk(clock(), 그 표정)`을 쿨다운·기분 처리에만 쓴다. `reply`가 `null`이면 아무것도 하지 않고, 아니면 상태 저장 후 `say(lineFor(species.value, GREETING, 그 표정))`. (`ask()`가 이미 이렇게 `talk`을 문턱으로만 쓴다.)
- 주행 시작 인사는 `lineFor(species.value, DRIVE_START, currentFace())`, `ask()`의 못 알아들음은 `lineFor(species.value, NOT_UNDERSTOOD, currentFace())`. 알림과 차량 질문 답은 그대로.
- `chooseSpecies`/`changeSpecies`는 공통 비공개 함수 하나로 초기화한다. 첫 `suspend` 호출(저장) **전에** 종 상태를 바꿔 두어, 같은 순간 두 번 눌러도 두 번째 호출이 "이미 골랐음"을 본다. 종 쓰기 `IOException`은 무시하고 `// ponytail: 종 저장 실패는 무시, 다음 실행에 고르기 화면이 다시 뜰 수 있다` 주석을 남긴다.

- [ ] **Step 1: `FakeRepo` 확장과 `PetRepository` 변경.** 위 Interfaces대로 인터페이스, `PetStore`, `FakeRepo`를 바꾼다. `./gradlew testDebugUnitTest` → 기존 115개 + Task 1 테스트가 PASS(동작 변경 없음).
- [ ] **Step 2: 실패하는 테스트 작성** (`PetViewModelSpeciesTest`, 기존과 같은 `StandardTestDispatcher` + `advanceUntilIdle` 패턴, `var now`로 바꾸는 clock, `FakeSignalSource()`, `FakeRepo`, `FakeNarrator`).
  - `not_chosen_needs_picker_only_when_parked`: `FakeRepo()` → 읽은 뒤 `needsPicker == false`(처음은 주행 중), `species == RABBIT`, `speciesChosen == false`. `setParked(true)` → `needsPicker == true`. `setParked(false)` → `false`. (Review Focus 1)
  - `not_chosen_while_driving_talks_as_the_rabbit`: 주행 중 `talk()` → 말한 줄 `["안녕! 오늘도 같이 달려요"]`. (Review Focus 1)
  - `stored_species_is_restored`: `FakeRepo(initialSpecies = BEAR)`, `setParked(true)` → `species == BEAR`, `speciesChosen == true`, `needsPicker == false`.
  - `picker_waits_for_the_stored_species`: `gate`가 닫힌 `FakeRepo(initialSpecies = CAT, gate = gate)`에서 ViewModel을 거치지 않고 `source.setUx(UxState.PARKED)`로 정차 → `needsPicker == false`. `gate.complete(Unit)` 후에도 `false`, `species == CAT`. (Review Focus 5)
  - `choose_saves_switches_and_resets_the_pet`: `FakeRepo(initial = PetState(30, 1_000, 0, 0))`, `now = 1_000`, 정차, `now = 5_000`, `chooseSpecies("dog")` → `savedSpecies == [DOG]`, `species == DOG`, `speciesChosen == true`, `needsPicker == false`, `pet.value == PetState(70, 5_000, 0, 0)`, `saved.last() == PetState(70, 5_000, 0, 0)`.
  - `choose_is_ignored_while_driving`: 주행 중 `chooseSpecies("cat")` → `species == RABBIT`, `speciesChosen == false`, `savedSpecies` 비어 있음. (Review Focus 1)
  - `double_tap_on_confirm_keeps_the_first_choice`: `setParked(true)`와 `advanceUntilIdle()` 뒤, 그 사이 진행 없이 `chooseSpecies("cat")`, `chooseSpecies("dog")`를 연달아 부르고 `advanceUntilIdle()` → `species == CAT`, `savedSpecies == [CAT]`. (Review Focus 2)
  - `unknown_id_is_ignored`: 정차, `chooseSpecies("dragon")` → `speciesChosen == false`, `savedSpecies` 비어 있음, `needsPicker == true`. (Review Focus 3)
  - `species_load_failure_falls_back_to_the_rabbit`: `FakeRepo(failLoad = true)` → `species == RABBIT`, `speciesChosen == false`, 기분 70. 정차 → `needsPicker == true`. (Review Focus 4)
  - `species_save_failure_keeps_the_choice`: `FakeRepo(failSave = true)`, 정차, `chooseSpecies("fox")` → 크래시 없음, `species == FOX`, `speciesChosen == true`, 기분 70. (Review Focus 4)
  - `change_resets_mood_and_switches_lines_at_once`: `FakeRepo(initial = PetState(40, 1_000, 0, 0), initialSpecies = RABBIT)`, `now = 1_000`, 정차, `petIt()` → 기분 50, `talk()` → 말풍선 있음. `now += 1_000`, `changeSpecies("duck")` → 기분 70, `line.value == null`, `species.value.callName == "오리야"`, `savedSpecies == [DUCK]`. 이어서 `talk()` → 마지막으로 말한 줄 `"꽥! 안녕!"`. (Review Focus 7)
  - `change_to_the_same_species_changes_nothing`: `FakeRepo(initial = PetState(40, 1_000, 0, 0), initialSpecies = CAT)`, 정차, `changeSpecies("cat")` → 기분 40, `savedSpecies` 비어 있음.
  - `change_is_ignored_while_driving`: `initialSpecies = CAT`, 주행 중 `changeSpecies("dog")` → `species == CAT`. (Review Focus 1)
  - `drive_start_after_choosing_uses_the_new_species`: 정차, `chooseSpecies("dog")`, `setParked(false)` → 말한 줄 `["출발이다! 신난다!"]`. (Review Focus 6)
  - `talk_and_ask_use_the_chosen_species_lines`: `initialSpecies = BEAR`에서 `talk()` → `"안녀엉~ 오늘도 든든하게 가자"`. 새 ViewModel(`initialSpecies = CAT`)에서 `ask("오늘 날씨 어때?")` → `"음? 무슨 말이냥"`. 같은 고양이에서 `applyPreset(RUNNING_OUT)`의 알림은 `alertLine(Alert.FUEL_SHORT)` 그대로.
- [ ] **Step 3: 실행해 실패 확인.** `./gradlew testDebugUnitTest --tests "*PetViewModelSpeciesTest*"` → 컴파일 실패.
- [ ] **Step 4: 위 동작대로 구현한다.** 기존 규칙(`loaded.join()`, 쿨다운, 600000ms 인사 간격, 알림 간격 공유)은 바꾸지 않는다.
- [ ] **Step 5: 통과 확인.** 같은 명령 → PASS. 이어서 전체 `./gradlew testDebugUnitTest assembleDebug` → PASS(기존 `PetViewModelTest`, `PetViewModelTripTest` 수정 없이).
- [ ] **Step 6: 커밋.** `feat: 고른 종 저장과 종별 대사를 ViewModel에 연결`

---

### Task 3: 그림을 종으로 매개변수화 (토끼는 그대로)

**Files:**
- Modify: `SRC/ui/PetFace.kt`, `SRC/ui/RoomScene.kt`, `SRC/ui/PetScreen.kt`

**Interfaces:**
- Consumes: `Species`, `petDescription` (Task 1), `PetViewModel.species` (Task 2)
- Produces:
```kotlin
@Composable fun PetFace(face: Face, species: Species, modifier: Modifier = Modifier)   // species 기본값 없음
fun RoomScene(face: Face, species: Species, line: String?, /* 나머지 인자 그대로 */)   // face 바로 뒤에 추가
// PetFace.kt 내부: private class CarStyle(species: Species) — body, shade, ear, inner를 Compose Color(Long)로
```
- `PetFace` 안에서 `remember(species) { CarStyle(species) }`를 만들고 그리는 함수들에 넘긴다. `Mint`, `MintShade`, `EarFill`, `PinkGlass` 상수는 `CarStyle` 값으로 대체하고 지운다. 나머지 상수(외곽선, 유리, 볼, 입, 바퀴, 연출 색)는 그대로.
- 이 Task에서는 **모든 종이 토끼 귀와 하트 장식, 토끼 코**로 그려지고 색만 다르다. 귀·장식·코는 Task 5, 6에서 종별로 바꾼다.
- 접근성 설명은 `petDescription(face, species)`.
- `PetScreen`은 `vm.species`를 모아 `RoomScene`에 넘긴다.

- [ ] **Step 1: 바꾸기 전 스크린샷.** 에뮬레이터 확인 절차로 에뮬레이터를 켜고(이 Task 끝까지 켜 둔다) 지금 코드를 설치한다. 정차 스위치를 켜고 `HEALTHY`, `NEEDS_CARE`, `LOW_FUEL` 세 표정을 `before-happy.png`, `before-sulky.png`, `before-hungry.png`로 찍는다.
- [ ] **Step 2: 위 Interfaces대로 구현한다.** 단위 테스트할 새 순수 로직은 없다(색 값은 Task 1의 `rabbit_colors_are_the_current_drawing_colors`가 지킨다).
- [ ] **Step 3: 컴파일과 테스트.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS, BUILD SUCCESSFUL.
- [ ] **Step 4: 바꾼 뒤 스크린샷과 비교.** `installDebug` 후 같은 세 표정을 `after-*.png`로 찍고 Read로 전후를 나란히 본다. 확인: ① 토끼의 색, 귀, 하트 장식, 표정, 연출이 전과 같다 ② `uiautomator dump`에서 설명이 "행복한 토끼 캐릭터"(시무룩, 배고픔도 각각)로 나온다. (Review Focus 9)
- [ ] **Step 5: 에뮬레이터 끄기.** `adb emu kill` 후 `pgrep -fl qemu`에 아무것도 나오지 않는지 확인한다.
- [ ] **Step 6: 커밋.** `refactor: 캐릭터 그림이 종의 색을 받아 그리도록 변경`

---

### Task 4: 고르기 화면과 친구 바꾸기

**Files:**
- Create: `SRC/ui/SpeciesPicker.kt`
- Modify: `SRC/ui/PetScreen.kt`, `SRC/ui/RoomScene.kt` (`PillButton`을 `private` → `internal`, 그 외 변경 없음)

**Interfaces:**
- Consumes: `Species.entries`, `PetFace(face, species, modifier)` (Task 3), `PetViewModel.species`, `needsPicker`, `chooseSpecies`, `changeSpecies` (Task 2), `PillButton`, `Line`, `Pink`/`Yellow` 색 (`RoomScene.kt`, 필요하면 `internal`로)
- Produces:
```kotlin
@Composable
fun SpeciesPicker(
    initial: Species?,                 // 처음 고르기는 null, 바꾸기는 지금 종
    onConfirm: (Species) -> Unit,
    onClose: (() -> Unit)?,            // null이면 닫기 버튼 없음 (처음 고르기)
    modifier: Modifier = Modifier,
)
```
`PetScreen` 동작:
- 로컬 상태 `changing`(바꾸기 화면 열림, `rememberSaveable`)과 `pendingChange: Species?`(확인 대기, `remember`).
- `showPicker = needsPicker || (changing && parked)`. 보이면 방과 개발자 도구 대신 `SpeciesPicker`, 정차 스위치 줄은 그대로 아래에 둔다.
- 처음 고르기: `SpeciesPicker(null, onConfirm = { vm.chooseSpecies(it.id) }, onClose = null)`.
- 바꾸기: 개발자 도구에 `Line` 색 `Button` "친구 바꾸기" → `changing = true`. 그 위에 `// ponytail: 설정 대화상자가 생기면(LLM 계획) 그쪽으로 옮긴다` 주석. `SpeciesPicker(species, onConfirm = { if (it == species) changing = false else pendingChange = it }, onClose = { changing = false })`.
- `pendingChange`가 있으면 `AlertDialog`(문구는 "동작 규칙" 표). "바꾸기" → `vm.changeSpecies(it.id)`, `pendingChange = null`, `changing = false`. "취소" → `pendingChange = null`(고르기 화면은 열린 채).
- `LaunchedEffect(parked)`: 정차가 풀리면 `changing = false`, `pendingChange = null`(주행 중에 바꾸기 화면·대화상자가 남지 않게).

- [ ] **Step 1: 위대로 구현한다.** 로직은 ViewModel(Task 2)에서 테스트했으므로 화면 단위 테스트는 두지 않는다.
- [ ] **Step 2: 컴파일과 테스트.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS.
- [ ] **Step 3: 에뮬레이터 확인.** 에뮬레이터를 켜고 설치한 뒤 `pm clear`로 처음 상태에서 실행한다. 확인하며 각 화면을 찍는다:
  ① 처음 실행(주행 중)에는 고르기 화면 없이 토끼 방이 보인다.
  ② 정차 스위치를 켜면 고르기 화면: 제목, 6장의 카드(각자 색, 행복 표정, 이름, 한 줄), 확정 버튼 없음.
  ③ 고르기 화면에서 정차 스위치를 끄면 토끼 방으로 돌아간다. 다시 켜면 고르기 화면. (Review Focus 1)
  ④ "곰" 카드를 누르면 카드가 노랗게 되고 위에 크게 보이며 "이 아이로 시작"이 나타난다. 누르면 곰 색의 방이 보인다.
  ⑤ `am force-stop` 후 다시 실행하고 정차해도 고르기 화면이 뜨지 않고 곰이다(저장 확인).
  ⑥ 개발자 도구 "친구 바꾸기" → 곰이 미리 선택된 고르기 화면, "닫기" 있음. "오리" → "이 아이로 시작" → 대화상자 "기분이 처음부터 다시 시작돼요" → "취소"면 그대로, 다시 확정 후 "바꾸기"면 오리 색 방과 개발자 도구의 "기분 70".
  ⑦ 가로로 돌려 고르기 화면이 잘리지 않고 스크롤로 모든 카드와 버튼에 닿는다. 세로로 되돌린다.
  보지 못한 항목은 그대로 보고한다.
- [ ] **Step 4: 에뮬레이터 끄기.** `adb emu kill`, `pgrep -fl qemu`로 꺼진 것 확인.
- [ ] **Step 5: 커밋.** `feat: 처음 고르기 화면과 개발자 도구의 친구 바꾸기 추가`

---

### Task 5: 종별 귀와 머리 장식

**Files:**
- Modify: `SRC/ui/PetFace.kt`

**Interfaces:**
- Consumes: `Species.ears`, `EarStyle` (Task 1), `CarStyle` (Task 3), 지금 `ear`/`sway`/`earTwitch` 값
- Produces: `drawCar` 안의 귀 그리기를 `when (species.ears)`로 나눈다. `BUNNY` 가지는 지금 `mirrorEar` 그대로 둔다. 나머지 5가지는 "종별 그림" 표의 모양·위치·각도. 귀는 지금처럼 몸통보다 먼저 그려 몸통 뒤에 있게 한다(오리 깃털도 같음). 표의 좌표가 화면에서 어색하면 ±4 설계 단위 안에서 고치고 보고서에 적는다.

- [ ] **Step 1: 구현한다.** 순수 로직이 없으므로 단위 테스트는 없다. `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS.
- [ ] **Step 2: 에뮬레이터 확인.** 에뮬레이터를 켜고 설치한다(처음 상태면 아무 종이나 골라 시작). 개발자 도구 "친구 바꾸기"로 6종을 차례로 고르며 각 종을 `HEALTHY`(행복)와 `NEEDS_CARE`(시무룩)에서 찍는다(12장, `ears-<id>-happy.png` 등). 확인: ① 귀 모양이 표대로 종마다 구별된다 ② 시무룩이면 토끼 귀는 크게, 고양이·여우는 조금 눕고, 강아지 귀는 행복에서 들리고 시무룩에서 처진다 ③ 귀·깃털이 안테나, 먹구름, 별 연출과 겹쳐 가려지지 않는다 ④ 토끼는 Task 3과 같다 ⑤ 외곽선 두께와 색이 몸통과 같다.
- [ ] **Step 3: 에뮬레이터 끄기.** `adb emu kill`, `pgrep -fl qemu`로 꺼진 것 확인.
- [ ] **Step 4: 커밋.** `feat: 종별 귀와 머리 장식 그리기`

---

### Task 6: 안테나 장식과 얼굴 특징

**Files:**
- Modify: `SRC/ui/PetFace.kt`

**Interfaces:**
- Consumes: `Species.charm`, `Charm`, `Species.ears` (Task 1), `CarStyle` (Task 3), 기존 `heart`, `drop` 경로 함수
- Produces: `antenna`의 장식을 `when (species.charm)`으로, `face`의 입마개 색과 코를 `when (species.ears)`로 나눈다("종별 그림" 표와 안테나 장식 목록 그대로). 장식 경로는 `CarPaths`처럼 `PetFace`마다 한 번만 만든다(그리는 중에 `Path`를 새로 만들지 않는다). 입·눈·눈썹·볼·눈물·침은 모든 종 공통이고 오리는 부리 위에 그대로 그린다.

- [ ] **Step 1: 구현한다.** `./gradlew testDebugUnitTest assembleDebug` → 전체 PASS.
- [ ] **Step 2: 에뮬레이터 확인.** 에뮬레이터를 켜고 6종을 차례로 바꿔 `HEALTHY`, `NEEDS_CARE`, `LOW_FUEL` 세 표정을 찍는다(18장). 그리고 처음 상태(`pm clear`)에서 정차해 고르기 화면을 한 장 찍는다. 확인: ① 장식 6가지(하트, 별, 꿀방울, 뼈다귀, 불꽃, 물방울)가 구별되고 안테나 끝에 붙어 흔들린다 ② 고양이 수염, 곰·강아지 코, 여우 코, 오리 부리와 콧구멍이 보인다 ③ 세 표정의 입과 침, 눈물이 코·부리·수염과 겹쳐 지저분하지 않다 ④ 고르기 화면 카드 6장이 서로 다른 모습으로 보인다 ⑤ 몇 초 간격 두 장으로 숨쉬기·깜빡임이 모든 종에서 움직인다(한 종이면 충분) ⑥ `adb logcat -d | grep -i "AndroidRuntime"`에 앱 크래시가 없다.
- [ ] **Step 3: 에뮬레이터 끄기.** `adb emu kill`, `pgrep -fl qemu`로 꺼진 것 확인.
- [ ] **Step 4: 커밋.** `feat: 종별 안테나 장식과 얼굴 특징 그리기`

---

### Task 7: README 갱신과 최종 확인

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 전체 확인.** 에뮬레이터 없이 `./gradlew clean testDebugUnitTest assembleDebug` → 전체 PASS. 테스트 수는 JUnit XML의 합으로 구한다: `grep -ho 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | grep -o '[0-9]*' | paste -sd+ - | bc` (이 계획 전에는 115).
- [ ] **Step 2: README를 고친다.**
  - "지금 되는 것" 첫 항목을 6종 기준으로 바꾸고("토끼, 고양이, 곰, 강아지, 여우, 오리 중 고른 친구가 방에 있습니다…"), 새 항목 **캐릭터 6종과 처음 고르기**를 더한다: 처음 정차하면 고르기 화면이 뜨고 주행 중에는 뜨지 않으며 고르기 전에는 토끼, 종마다 색·귀·장식·말투가 다르고 게이지·규칙은 같다, 개발자 도구의 "친구 바꾸기"로 바꾸면 기분이 처음부터 시작된다.
  - "개발자 도구" 항목에 "친구 바꾸기"를 더한다. "처음 열면…" 문단에 "정차 스위치를 처음 켜면 친구를 고르는 화면이 나옵니다"를 더한다.
  - 구조 트리: `core/`에 `Species.kt  캐릭터 6종 표, 종별 대사, 저장된 종 해석`, `ui/` 줄에 `SpeciesPicker.kt`를 더한다.
  - "현재 115개"를 Step 1에서 구한 수로 바꾼다.
  - 설계 문서의 "계획" 목록에 `[캐릭터 6종과 처음 고르기](docs/superpowers/plans/2026-10-23-species-selection.md)`를 더한다.
  - "앞으로 할 일"에서 1번(캐릭터 6종)을 지우고 번호를 다시 매긴다: 1. 계정(Claude API 키) 연결과 LLM 음성 대화(고른 종의 성격으로 말하기) 2. 호출어(고른 친구의 이름: "토끼야", "냥이야" 등)로 부르는 항상 듣기.
- [ ] **Step 3: 커밋.** `docs: README를 캐릭터 6종과 처음 고르기 구현에 맞게 갱신`
