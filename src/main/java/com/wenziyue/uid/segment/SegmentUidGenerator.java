package com.wenziyue.uid.segment;

import com.wenziyue.uid.properties.UidGeneratorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wenziyue
 */
@Component
@RequiredArgsConstructor
public class SegmentUidGenerator {

    private final SegmentIdDao dao;
    private final UidGeneratorProperties props;

    /** 本地号段 */
    private final AtomicLong current = new AtomicLong(0);
    private volatile long max = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public long nextId() {
        while (true) {
            long id = current.incrementAndGet();
            if (id < max) {
                return id;
            }
            fetchSegment();    // 取下一段
        }
    }

    private void fetchSegment() {
        if (!lock.tryLock()) return;
        try {
            if (current.get() < max) return; // 双检
            long newMax = dao.nextMaxId(props.getBizTag(), props.getStep());
            long newMin = newMax - props.getStep() + 1;
            current.set(newMin - 1);
            max = newMax;
        } finally {
            lock.unlock();
        }
    }
}
