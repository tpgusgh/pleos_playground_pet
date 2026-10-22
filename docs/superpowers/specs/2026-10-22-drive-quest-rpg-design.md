# Drive Quest RPG 설계

## 의도
운전 습관이 곧 캐릭터 성장이 되는 Pleos Connect 앱. 안전하고 부드럽게 운전할수록 캐릭터가 강해진다. 목표는 재미와 안전운전 유도의 결합.

## 핵심 구조
- **주행 중 (수집 전용)**: 화면 조작 없음. 백그라운드에서 Vehicle SDK 신호를 읽어 점수화. 종료 시 TTS로 결과만 읽어줌.
- **정차 후 (RPG)**: 캐릭터 육성, 장비 강화, 자동 전투, 보스전.
- **주행 제한 상태**: `CarUxRestrictions`가 제한이면 읽기 전용 요약만 표시. 플랫폼이 준 상태만 따르고 지역 분기 금지.

## 목표 / 비목표 / 안티 목표
- 목표: Pleos API를 최대한 넓게 활용. 모든 신호는 `checkXXXCapabilities` 후 사용, 미지원이면 건너뜀.
- 비목표: 서버, 계정, 차량 제어(쓰기). 모든 기능은 읽기만 사용.
- 안티 목표: 과속이나 급가속이 이득이 되면 실패. 주행 중 화면 조작을 유도하면 실패.

## Pleos API → 게임 매핑
| API | 게임 요소 |
|---|---|
| Brake | 부드러운 제동 = 방어력, 급제동 = 피해 |
| Steering wheel | 부드러운 조향 = 민첩, 급조향 = 감점 |
| Turn signal | 방향 전환 시 깜빡이 = 콤보 |
| Odometer | 거리 = 경험치 (일일 상한) |
| Fuel / EV battery | 효율 = 마나·체력 회복, 회생제동 보너스 |
| Driving mode | 에코 모드 = 버프 (스포츠는 보상 없음, 페널티 없음) |
| Safety | 안전벨트 시작 보너스, ADAS 경고 = 피해 |
| Light | 야간 헤드라이트 = 야간 버프 |
| Wiper | 비 = 폭풍 던전 이벤트 |
| Tire | 압력 정상 = 장비 내구도 보너스 |
| Door / Window / Seat / Side mirror | 출발 전 체크리스트 퀘스트 (문·창문 닫힘 등), 읽기만 |
| HVAC | 쾌적 온도 유지 = 소소한 버프 |
| Toll | 통행료 = 골드 소모 이벤트 "관문" |
| Car info | 차종 = 캐릭터 클래스·외형 시드 |
| Display | 주야간 테마 연동 |
| TextToSpeech | 주행 종료 요약, 전투 결과 낭독 |
| SpeechToText / Gleo | 음성 명령 ("전투 시작", "상태 알려줘") |
| NaviHelper (내비 장착 차량만) | 목적지 = 원정 퀘스트, 충전소 = 마을 |
| FusedLocation (차종별) | 지역 던전 |
| 속도 | AAOS 표준 속성으로 가능한지 스파이크에서 확인. 확정 전까지 의존하지 않음 |

## 만드는 방식 (내 판단)
- Kotlin + Jetpack Compose, Vehicle SDK 2.3.2 (`ai.pleos.playground:Vehicle`), minSdk 23, targetSdk 35, JDK 17.
- 저장은 기기 로컬 (DataStore). 서버 없음.
- 신호 입력은 `VehicleSignalSource` 하나로 감싸고 구현은 둘: Pleos 실제 구현, 책상 개발용 가짜 신호. Pleos 에뮬레이터 없이도 게임 로직을 돌려보기 위함.
- 점수 계산과 전투는 순수 Kotlin 함수로 분리해 단위 테스트.

## 남은 위험
- SDK Maven 접근과 Pleos Connect AVD는 프로젝트 생성 후 신청해야 함. 없으면 가짜 신호로만 검증 가능.
- Entertainment 카테고리 심사 가능 여부는 문서로 판단 불가.
- 샘플 README의 SDK 버전이 문서마다 다름 (2.0.3 / 2.3.x). 빌드 시 최신 확인.

## 빌더에게 맡기는 것
화면 디자인, 밸런스 수치, 몬스터/장비 구성.
