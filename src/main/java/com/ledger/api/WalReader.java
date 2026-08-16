package com.ledger.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WAL 재생(replay)의 첫 걸음: 로그를 읽어 국면별로 가른다.
 * COMMIT이 있는 seq = 완료된 이체.
 * BEGIN만 있고 COMMIT이 없는 seq = 하다 만 이체(크래시 흔적).
 */
public class WalReader {

    private final Path path;

    public WalReader(String fileName) {
        this.path = Path.of(fileName);
    }

    public List<WalEntry> readAll() {
        if (!Files.exists(path)) return List.of();
        List<WalEntry> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path)) {
                if (line.isBlank()) continue;
                String[] f = line.split("\\|", -1);
                if (f.length != 6) continue;   // 깨진 줄은 건너뛴다
                out.add(new WalEntry(Long.parseLong(f[0]), f[1], f[2], f[3],
                                     Long.parseLong(f[4]), f[5]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** BEGIN은 있는데 COMMIT이 없는 seq들 = 미완료 연산. */
    public Set<Long> unfinishedSeqs() {
        Set<Long> begun = new HashSet<>();
        Set<Long> done = new HashSet<>();
        for (WalEntry e : readAll()) {
            if ("BEGIN".equals(e.phase())) begun.add(e.seq());
            else if ("COMMIT".equals(e.phase()) || "ABORT".equals(e.phase())) done.add(e.seq());
        }
        begun.removeAll(done);
        return begun;
    }
}
