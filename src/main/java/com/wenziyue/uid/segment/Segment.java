package com.wenziyue.uid.segment;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 表示一段号段：从 value 到 max（闭区间）
 * 提供线程安全的发号操作
 *
 * @author wenziyue
 */
@Data
public class Segment {

    /**
     * 当前发号位置（初始为 start - 1）
     * 每次调用 nextId() 时通过 getAndIncrement() 获取
     */
    private final AtomicLong value = new AtomicLong(0);

    /**
     * 当前段最大值（由数据库控制）
     */
    private volatile long max;

    /**
     * 当前段步长（用于观察使用率或日志）
     */
    private volatile int step;

    /**
     * 回指所属的 SegmentBuffer，用于后续扩展
     */
    private final SegmentBuffer buffer;

    public Segment(SegmentBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * 返回剩余未使用数量（当前使用进度）
     */
    public long getIdle() {
        return max - value.get();
    }

    @Override
    public String toString() {
        return "Segment{value=" + value.get() + ", max=" + max + ", step=" + step + '}';
    }
}
