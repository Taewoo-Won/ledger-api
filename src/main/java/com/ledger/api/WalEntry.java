package com.ledger.api;

/**
 * WAL 한 줄 = 이체 하나의 한 국면.
 * phase: BEGIN(무엇을 할지 먼저 적음) 또는 COMMIT(실제 반영 완료).
 * 재시작 시 BEGIN만 있고 COMMIT이 없는 seq = 미완료 연산.
 */
public record WalEntry(
        long seq,
        String phase,
        String fromId,
        String toId,
        long amount,
        String idempotencyKey) {}
