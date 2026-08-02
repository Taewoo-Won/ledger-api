# ledger-api

결제·정산 백엔드를 목표로 한 학습 프로젝트. 순수 Java로 만든 결제 원장(mini-ledger)을 Spring Boot HTTP API로 확장 중.

## 현재 기능
- `GET /health` — 서버 생존 확인
- `POST /transfer` — 계좌 이체 (수수료 정산 포함)
  - **멱등성**: 같은 idempotencyKey 재요청 시 중복 처리 방지 (Stripe/Toss 패턴, HTTP 검증 완료)
  - **복식부기 검증**: 매 이체마다 시스템 전체 잔액 합 = 0 확인

## 스택
Java 21 · Spring Boot · Gradle

## 실행
./gradlew bootRun
curl -X POST localhost:8080/transfer -H "Content-Type: application/json" \
  -d '{"fromId":"a","toId":"b","amount":1000,"idempotencyKey":"key-1"}'

## 다음
영속성 — append-only WAL 설계·구현 (진행 중). 예외 → HTTP 번역은 완료(아래 에러 응답 형식 참조).

## 에러 응답 형식

이체 실패 시 HTTP 400과 함께 아래 JSON을 반환한다:

    {"code": "INSUFFICIENT_BALANCE", "message": "잔액이 충분하지 않습니다"}

| code | 의미 |
|---|---|
| ACCOUNT_NOT_FOUND | 존재하지 않는 계좌 id |
| INVALID_AMOUNT | 금액이 0 이하 |
| INSUFFICIENT_BALANCE | 출금 계좌 잔액 부족 |

`code`는 기계 판별용 불변 상수(변경 안 됨), `message`는 사람에게 보여줄 문장(변경될 수 있음).

## WAL(Write-Ahead Log) 설계 근거

현재 계좌 잔액은 메모리(Map)에만 존재한다. append-only WAL을 얹기로 했고,
근거 셋은 전부 이 프로젝트에서 실측으로 확인된 한계다.

| # | 한계 | 확인 경위 |
|---|---|---|
| 1 | 원자성 한계 | 이체가 여러 단계로 나뉘어, 중간 실패 시 일부만 반영될 여지가 있다 |
| 2 | 영속성 부재 | 서버 재시작 후 잔액이 초기값으로 리셋 — 직전 이체 결과가 증발했다 |
| 3 | 롤백 불가 | 실패한 연산을 되돌릴 기록이 없어 중간 상태가 그대로 남는다 |

WAL의 원칙은 "상태를 바꾸기 전에 무엇을 바꿀지 먼저 로그에 적는다"이다.
이 로그가 있으면 재시작 시 재생(replay)으로 상태를 복원할 수 있고(2),
미완료 연산을 판별해 되돌릴 수 있다(3).
