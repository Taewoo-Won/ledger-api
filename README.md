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
예외 → HTTP 상태코드 번역 (잔액 부족 500 → 4xx) · 영속성(8월, WAL)

## 에러 응답 형식

이체 실패 시 HTTP 400과 함께 아래 JSON을 반환한다:

    {"code": "INSUFFICIENT_BALANCE", "message": "잔액이 충분하지 않습니다"}

| code | 의미 |
|---|---|
| ACCOUNT_NOT_FOUND | 존재하지 않는 계좌 id |
| INVALID_AMOUNT | 금액이 0 이하 |
| INSUFFICIENT_BALANCE | 출금 계좌 잔액 부족 |

`code`는 기계 판별용 불변 상수(변경 안 됨), `message`는 사람에게 보여줄 문장(변경될 수 있음).
