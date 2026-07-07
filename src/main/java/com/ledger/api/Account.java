package com.ledger.api;
public class Account {
    private final String id;
    private final String owner;
    private long balance;

    public Account(String id, String owner, long initialBalance) {
        this.id = id;
        this.owner = owner;
        this.balance = initialBalance;
    }

    public long getBalance() {
        return balance;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금액은 0보다 커야 합니다");
        }
        balance = balance + amount;
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("출금액은 0보다 커야 합니다");
        }
        if (balance < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다");
        }
        balance = balance - amount;
    }
}
