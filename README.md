# 차 타마고치 (Car Tamagotchi)

내 차가 곧 키우는 캐릭터인 **PLEOS Connect용 차량 앱**입니다. 차의 실제 상태(연료, 타이어, 문·창문, 와이퍼, 운전 습관)가 캐릭터의 **밥·건강·기분**으로 보이고, 차를 잘 돌볼수록 캐릭터가 행복해집니다. 방치해도 죽거나 가출하지 않고, 시무룩해질 뿐입니다.

> 이 저장소는 PLEOS Playground API로 만드는 앱을 실험하는 프로젝트입니다. 지금은 **가짜 차량 신호**로 동작하며, 실제 Pleos Vehicle SDK는 아직 연결하지 않았습니다.

## 지금 되는 것

- **방 안에 앉아 있는 캐릭터**: 토끼 귀 민트 자동차가 방(창문, 러그, 나무 바닥) 안에 있습니다. 행복·시무룩·배고픔 세 가지 표정과 연출(별, 눈물과 먹구름, 꼬르륵), 숨쉬기·깜빡임 같은 움직임이 있습니다.
- **밥·건강·기분 게이지**: 하트 4칸으로 벽 표지판에 표시됩니다.
  - 밥 = 연료 또는 EV 배터리 잔량
  - 건강 = 타이어 압력, 문·창문 닫힘, 와이퍼 상태
  - 기분 = 부드러운 제동·조향, 시간 경과(30분마다 1점 감소), 쓰다듬기·말 걸기
- **돌봄**: 정차 중에 *쓰다듬기*, 언제든 *말 걸기*(TTS로 대답). 연타는 쿨다운(5초)으로 막습니다.
- **주행 제한 준수**: 주행 중(`RESTRICTED`)에는 캐릭터와 말풍선, 말 걸기 버튼만 보이고 화면 조작을 요구하지 않습니다. 가로 화면(차량용)에서도 한 화면에 들어옵니다.
- **신호가 없어도 동작**: 지원하지 않는 신호(`null`)는 건너뛰고 나머지로 계산합니다.
- **저장**: 기분 상태는 기기에 로컬 저장(DataStore)됩니다. 서버와 계정은 없습니다.
- **개발자 도구**: 정차 상태에서 연료 슬라이더, 상태 프리셋, 주행 시뮬레이션으로 신호를 움직여 볼 수 있습니다.

## 실행해 보기

필요한 것: JDK 17, Android SDK(compileSdk 35), Android 에뮬레이터 또는 기기(API 23 이상).

```bash
# 프로젝트 루트에 local.properties를 만들어 SDK 경로를 지정
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# JDK 17로 실행 (기본 JDK가 17이 아니면 JAVA_HOME을 지정)
export JAVA_HOME=/path/to/jdk-17

./gradlew testDebugUnitTest   # 단위 테스트
./gradlew installDebug        # 연결된 기기·에뮬레이터에 설치
```

처음 열면 주행 중 상태라 캐릭터와 말 걸기 버튼만 보입니다. "정차" 스위치를 켜면 쓰다듬기와 개발자 도구가 나타납니다.

## 구조

```
app/src/main/java/com/pleosplayground/drivequest/
├── core/   순수 Kotlin 로직 (Android 의존 없음, JVM 단위 테스트)
│   ├── Sample.kt, VehicleSignalSource.kt, FakeSignalSource.kt   차량 신호와 가짜 신호
│   ├── Pet.kt, PetState.kt                                      게이지, 표정, 시간 경과, 쿨다운, 대사
│   └── UxGate.kt                                                주행 제한 판단
├── data/   PetStore.kt        DataStore 저장
└── ui/     PetViewModel.kt, PetScreen.kt, RoomScene.kt, PetFace.kt, Speaker.kt, MainActivity.kt
```

- 신호 입력은 `VehicleSignalSource` 인터페이스 뒤에 있습니다. 지금은 `FakeSignalSource`만 구현되어 있고, 실제 Pleos SDK 연동은 이 인터페이스 구현을 추가하는 방식으로 붙일 계획입니다.
- 게임 로직은 모두 `core/`의 순수 함수라서 에뮬레이터 없이 테스트합니다(현재 61개).

## 설계 문서

`docs/superpowers/` 아래에 한국어 설계와 계획이 있습니다.

- 설계: [차 타마고치](docs/superpowers/specs/2026-10-22-car-tamagotchi-design.md), [LLM 음성 대화](docs/superpowers/specs/2026-10-22-llm-voice-chat-design.md), [캐릭터 6종과 처음 고르기](docs/superpowers/specs/2026-10-22-species-selection-design.md)
- 계획: [차 타마고치 구현 계획](docs/superpowers/plans/2026-10-22-car-tamagotchi.md), [차량 정보 질문과 먼저 말하기](docs/superpowers/plans/2026-10-22-vehicle-info-and-proactive.md)
- 화면 스타일 참고 목업: [`docs/superpowers/specs/assets/chiikawa-room-mockup.html`](docs/superpowers/specs/assets/chiikawa-room-mockup.html) (브라우저로 열기, 실제 앱 화면이 아니라 디자인 시안입니다)
- `2026-10-22-drive-quest-rpg-*` 문서는 처음에 시도했다가 방향을 바꾼 RPG 안의 기록입니다.

## 앞으로 할 일 (아직 구현되지 않음)

1. 차량 정보 질문과 먼저 말하기: 연료로 얼마나 가는지, 목적지까지 얼마나 남았는지 답하고, 연료 부족과 도착 임박을 먼저 알려줍니다. (구현 계획 작성됨)
2. 캐릭터 6종(토끼, 고양이, 곰, 강아지, 여우, 오리)과 처음 고르기
3. 계정(Claude API 키) 연결과 LLM 음성 대화
4. 호출어("토끼야")로 부르는 항상 듣기

## 알아둘 점

- 실제 Pleos Connect 차량에서 어떤 신호를 읽을 수 있는지는 차종마다 다르고(`checkXXXCapabilities`로 확인), 공식 문서만으로는 확인하지 못한 항목이 있습니다. 그래서 가짜 신호로만 검증했습니다.
- 서드파티 앱은 차량 속성을 **읽기만** 할 수 있다는 점을 전제로 설계했고, 차량 제어는 하지 않습니다.
- 주행 중에는 화면 조작을 요구하지 않는 것이 이 앱의 규칙입니다(`CarUxRestrictions`를 따르는 방향).
