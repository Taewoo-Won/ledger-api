package com.ledger.api;
import java.time.LocalDateTime;

public class Transaction {
    private long id;
    private TransactionType type;
    private String from;
    private String to;
    private long amount;
    private LocalDateTime timestamp;

    public Transaction(long id, TransactionType type, String from, String to, long amount) {
        this.id = id;
        this.type = type;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
