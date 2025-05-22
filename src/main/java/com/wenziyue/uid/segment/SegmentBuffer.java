package com.wenziyue.uid.segment;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SegmentBuffer 是号段分配的核心调度结构
 * - 管理 current / next 两段
 * - 控制段切换
 * - 控制异步加载状态
 *
 * @author wenziyue
 */
@Data
public class SegmentBuffer {

    /**
     * 业务标识
     */
    private String key;

    /**
     * 双 buffer：segments[0] 为 current，segments[1] 为 next
     */
    private final Segment[] segments =  new Segment[2];

    /**
     * 当前使用的 segment index（0 或 1）
     */
    private volatile int currentPos;

    /**
     * 是否初始化完成
     */
    private volatile boolean initOk;

    /**
     * 下一段是否准备好了（如果 ready，可以切段）
     */
    private volatile boolean nextReady;

    /**
     * 是否有后台线程正在加载下一段
     */
    private final AtomicBoolean threadRunning = new AtomicBoolean(false);

    /**
     * 并发控制锁（可选：第二阶段可以先不使用）
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SegmentBuffer(String key) {
        this.key = key;
        this.segments[0] = new Segment(this);
        this.segments[1] = new Segment(this);
    }

    // 常用操作

    /**
     * 获取当前使用的 Segment
     * @return Segment
     */
    public Segment getCurrent() {
        return segments[currentPos];
    }

    /**
     * 获取下一个 Segment
     * @return Segment
     */
    public Segment getNext() {
        return segments[nextPos()];
    }

    /**
     * 另一个 Segment 的 index
     * @return index 0或1
     */
    public int nextPos() {
        return (currentPos + 1) % 2;
    }

    /**
     * 切换位置 currentPos
     */
    public void switchPos() {
        currentPos = nextPos();
    }


}
