package com.ledger.api;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Bank {
    private Map<String, Account> accounts = new HashMap<>();
    private List<Transaction> history = new ArrayList<>();
    private final Set<String> usedKeys = new HashSet<>();
    private long nextTxId = 1;
    private final Set<Long> refundedIds = new HashSet<>();
    private long feeRate = 2;

    public void createAccount(String id, String owner, long initialBalance) {
        Account account = new Account(id, owner, initialBalance);
        accounts.put(id, account);
    }

    public Account getAccount(String id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException("계좌를 찾을 수 없습니다");
        }
        return account;
    }

    public List<Transaction> getHistory() {
        return history;
    }

    public List<Transaction> getHistoryByAccount(String id) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : history) {
            if (id.equals(t.getFrom()) || id.equals(t.getTo())) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Transaction> getHistoryAbove(long minAmount) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : history) {
            if (t.getAmount() >= minAmount) {
                result.add(t);
            }
        }
        return result;
    }

    public void deposit(String id, long amount) {
        Account toAccount = getAccount(id);
        toAccount.deposit(amount);
        history.add(new Transaction(nextTxId++, TransactionType.DEPOSIT, null, id, amount));
    }

    public void withdraw(String id, long amount) {
        Account fromAccount = getAccount(id);
        fromAccount.withdraw(amount);
        history.add(new Transaction(nextTxId++, TransactionType.WITHDRAW, id, null, amount));
    }

    public void transfer(String fromId, String toId, long amount, String idempotencyKey) {
        if (usedKeys.contains(idempotencyKey)) {
            return;
        }
        Account fromAccount = getAccount(fromId);
        Account toAccount = getAccount(toId);
        long fee = amount * feeRate / 100;
        if (fromAccount.getBalance() < amount + fee) {
            throw new InsufficientBalanceException("잔액이 충분하지 않습니다");
        }
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
        history.add(new Transaction(nextTxId++, TransactionType.TRANSFER, fromId, toId, amount));
        fromAccount.withdraw(fee);
        Account platformAccount = getAccount("platform");
        platformAccount.deposit(fee);
        history.add(new Transaction(nextTxId++, TransactionType.FEE, fromId, "platform", fee));
        usedKeys.add(idempotencyKey);
        checkBalance();
    }

    public Transaction findTransaction(long txId) {
        for (Transaction t : history) {
            if (t.getId() == txId) {
                return t;
            }
        }
        return null;
    }

    public void refund(long txId) {
        Transaction original = findTransaction(txId);
        if (original == null) {
            throw new IllegalArgumentException("환불할 거래를 찾을 수 없습니다: " + txId);
        }
        if (refundedIds.contains(txId)) {
            return;
        }
        Account fromAccount = getAccount(original.getFrom());
        Account toAccount = getAccount(original.getTo());
        toAccount.withdraw(original.getAmount());
        fromAccount.deposit(original.getAmount());
        history.add(new Transaction(nextTxId++, TransactionType.REFUND, original.getTo(), original.getFrom(), original.getAmount()));
        refundedIds.add(txId);
    }

    public void checkBalance() {
        Map<String, Long> changes = new HashMap<>();
        for (Transaction t : history) {
            if (t.getFrom() != null) {
                changes.put(t.getFrom(), changes.getOrDefault(t.getFrom(), 0L) - t.getAmount());
            }
            if (t.getTo() != null) {
                changes.put(t.getTo(), changes.getOrDefault(t.getTo(), 0L) + t.getAmount());
            }
        }
        long sum = 0;
        for (long value : changes.values()) {
            sum += value;
        }
        System.out.println("변동 합계: " + sum);
        if (sum == 0) {
            System.out.println("돈 보존됨 (합 = 0)");
        } else {
            System.out.println("깨짐! 돈이 새거나 생김");
        }
    }
}
