#📌 개요

본 프로젝트는 유선 USB-UART 통신을 통해 임베디드 보드와 연결하여,
보드에서 전송되는 상태 데이터를 수신하고 이를 실시간으로 화면에 시각화하는 안드로이드 애플리케이션입니다.
회사에서 제공한 통신 데이터시트를 기반으로 프로토콜을 해석하여 구현하였습니다.

#🔌 통신 방식

USB Serial (USB-to-UART) 통신
FTDI(FT232)(테스트용), CP210x(실제 기업 사용) 계열 USB-UART 변환 칩 지원
UART 설정: 115200 baud / 8 data bits / 1 stop bit / parity none (8N1)

#🔄 데이터 송수신 구조

보드에서 고정 길이 프레임(8바이트) 형태로 상태 데이터 전송
앱에서 바이트 배열 단위로 데이터 수신 및 파싱
프레임 내 각 바이트 위치에 따라 상태 정보 해석

#🖥️ UI 동작 방식

수신된 데이터의 비트 값(0/1) 을 기준으로 화면 색상 변경
0 : 정상 상태 (파란색)
1 : 비정상/미검출 상태 (흰색 또는 붉은색)
보드 상태에 따라 Scanning / Monitoring 상태를 구분하여 표시

#🔗 연결 상태 판단 로직

- UART 통신 특성상 별도의 연결 이벤트가 없기 때문에
데이터 수신 타임아웃 기반으로 연결 상태를 판단
- 일정 시간(3초) 이상 데이터가 수신되지 않을 경우
→ Disconnected 상태로 판단 및 UI 초기화
- 데이터 수신 시 즉시 Connected 상태로 갱신

#⚙️ 주요 구현 기능

USB Serial 장치 자동 탐색 및 포트 오픈
UART 통신 파라미터 설정 (baud rate, data bits 등)
바이트/비트 단위 데이터 파싱
수신 타임아웃 기반 Connect / Disconnect 상태 표시
보드 제어 명령(Byte Command) 송신 기능

#🛠 사용 기술

Android (Kotlin)
USB Serial for Android
UART (Serial Communication)
