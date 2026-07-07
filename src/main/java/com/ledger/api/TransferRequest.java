package com.ledger.api;

public record TransferRequest(String fromId, String toId, long amount, String idempotencyKey) {}
