# ledger-api

mini-ledger(순수 Java 원장)를 Spring Boot HTTP API로 감싼 학습 프로젝트.
도메인 로직(검증→실행→기록, 멱등성, 원자성, 복식부기)은
[mini-ledger](https://github.com/Taewoo-Won/mini-ledger)에서 수정 없이 이식.

## Endpoints
- `GET /health` — 서버 상태 확인
- `POST /transfer` — 이체 (멱등성 키 필수). 같은 키 재시도 = 결과 한 번만 반영.

## 실행
`./gradlew bootRun` → localhost:8080

## 한계 (의도된 학습 단계)
인메모리 — 재시작 시 상태 소멸. 영속성은 다음 단계(WAL)에서 직접 구현.
