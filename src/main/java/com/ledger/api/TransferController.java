package com.ledger.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
public class TransferController {

    private final Bank bank = new Bank();
    private final WalWriter wal = new WalWriter("wal.log");

    public TransferController() {
        bank.createAccount("a", "이름", 2100);
        bank.createAccount("b", "이름", 3500);
        bank.createAccount("platform", "플랫폼", 0);

        java.util.Set<Long> unfinished = new WalReader("wal.log").unfinishedSeqs();
        if (unfinished.isEmpty()) {
            System.out.println("[WAL] 미완료 이체 없음 — 정상 종료 상태에서 시작");
        } else {
            System.out.println("[WAL] 미완료 이체 발견: " + unfinished
                    + " — BEGIN 후 COMMIT을 못 남긴 연산이다. ABORT로 마감한다");
            for (WalEntry e : new WalReader("wal.log").readAll()) {
                if ("BEGIN".equals(e.phase()) && unfinished.contains(e.seq())) {
                    wal.append(new WalEntry(e.seq(), "ABORT", e.fromId(), e.toId(),
                                            e.amount(), e.idempotencyKey()));
                    System.out.println("[WAL] seq " + e.seq() + " -> ABORT 기록");
                }
            }
        }
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {
        long seq = wal.nextSeq();
        wal.append(new WalEntry(seq, "BEGIN", request.fromId(), request.toId(),
                                request.amount(), request.idempotencyKey()));
        bank.transfer(request.fromId(), request.toId(), request.amount(), request.idempotencyKey());
        wal.append(new WalEntry(seq, "COMMIT", request.fromId(), request.toId(),
                                request.amount(), request.idempotencyKey()));
        return "a: " + bank.getAccount("a").getBalance()
             + ", b: " + bank.getAccount("b").getBalance()
             + ", platform: " + bank.getAccount("platform").getBalance();
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("INSUFFICIENT_BALANCE", e.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("ACCOUNT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("INVALID_AMOUNT", e.getMessage()));
    }
}
