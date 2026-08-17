package com.ledger.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * append-only WAL: 파일 끝에 한 줄씩 덧붙이기만 한다.
 * 기존 줄은 절대 고치지 않는다 — 그래야 로그가 "일어난 일의 순서"가 된다.
 */
public class WalWriter {

    private final Path path;
    private final AtomicLong seq;

    public WalWriter(String fileName) {
        this.path = Path.of(fileName);
        this.seq = new AtomicLong(maxSeqInLog());   // 재시작해도 로그의 마지막 번호를 이어받는다
    }

    /** 기존 로그의 최대 seq. 로그가 없으면 0. */
    private long maxSeqInLog() {
        long max = 0;
        for (WalEntry e : new WalReader(path.toString()).readAll()) {
            if (e.seq() > max) max = e.seq();
        }
        return max;
    }

    /** 이체 하나에 seq 하나. BEGIN과 COMMIT은 같은 seq를 공유한다. */
    public long nextSeq() {
        return seq.incrementAndGet();
    }

    public void append(WalEntry e) {
        String line = e.seq() + "|" + e.phase() + "|" + e.fromId() + "|" + e.toId()
                + "|" + e.amount() + "|" + e.idempotencyKey() + System.lineSeparator();
        try {
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
