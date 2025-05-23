package com.wenziyue.uid.segment;


import com.wenziyue.uid.common.Result;
import com.wenziyue.uid.common.Status;
import com.wenziyue.uid.core.IdGen;
import com.wenziyue.uid.properties.UidGeneratorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 	1.	初始化并维护一个 SegmentBuffer；
 * 	2.	实现 nextId() 方法支持高并发发号；
 * 	3.	在段即将用尽时异步加载下一段；
 * 	4.	在段用尽后自动切换；
 * 	5.	确保线程安全、行为正确。
 *
 * @author wenziyue
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SegmentIdGeneratorImpl implements IdGen {

    private final Map<String, SegmentBuffer> cache  = new ConcurrentHashMap<>();
    private final UidGeneratorProperties properties;
    private final SegmentIdDao dao;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ScheduledExecutorService segmentUidScheduler;
    /**
     * 用来判断是否初始化成功
     */
    private final AtomicBoolean  initOk = new AtomicBoolean(false);

    /**
     * IDCache未初始化成功时的异常码
     */
    private static final long EXCEPTION_ID_IDCACHE_INIT_FALSE = -1;
    /**
     * key不存在时的异常码
     */
    private static final long EXCEPTION_ID_KEY_NOT_EXISTS = -2;
    /**
     * SegmentBuffer中的两个Segment均未从DB中装载时的异常码
     */
    private static final long EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL = -3;



    /**
     * 初始化，监听ApplicationReadyEvent事件，当springboot启动后开始执行，其实也可以使用@PostConstruct
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initOnBoot() {
        if (!initOk.get()) {
            this.init();
        }
    }

    /**
     * 从 SegmentBuffer 中获取一个唯一 ID。
     * - 优先从当前号段中获取；
     * - 如果当前段快用完，异步加载下一个段；
     * - 如果当前段已用完，尝试切换到下一个段；
     * - 如果两个段都不可用，返回异常。
     */
    @Override
    public Result nextId() {
        if (!initOk.get()) {
            return new Result(EXCEPTION_ID_IDCACHE_INIT_FALSE, Status.EXCEPTION);
        }
        SegmentBuffer buffer = cache.get(properties.getBizTag());
        if (buffer == null) {
            return new Result(EXCEPTION_ID_KEY_NOT_EXISTS, Status.EXCEPTION);
        }

        // 如果buffer还没初始化，并且后台没有进行初始化,那么初始化它
        if (!buffer.isInitOk()) {
            synchronized (buffer) {
                // 首次查询数据库
                val nextMaxId = dao.nextMaxId(buffer.getKey(), properties.getStep());
                buffer.getCurrent().setMax(nextMaxId);
                buffer.getCurrent().setStep(properties.getStep());
                buffer.getCurrent().getValue().set(nextMaxId - properties.getStep());
                buffer.setCurrentPos(0);

                buffer.setNextReady(false);
                buffer.setInitOk(true);
                log.info("[Segment UID] 初始化段成功，当前段最大值：{}", nextMaxId);
                return new Result(buffer.getCurrent().getValue().incrementAndGet(), Status.SUCCESS);
            }
        }

        while (true) {
            // 加读锁
            buffer.getLock().readLock().lock();
            try {
                // 当id的值超过当前段80%的时候，开始异步准备下一段
                if (!buffer.isNextReady()
                        && buffer.getCurrent().getIdle() < buffer.getCurrent().getStep() * (properties.getPrepareNextPercent() / 100.0)
                        && buffer.getThreadRunning().compareAndSet(false, true)) {
                    taskExecutor.execute(() -> {
                        try {
                            log.info("[Segment UID] 触发异步加载下一段，当前已用百分比：{}%", properties.getPrepareNextPercent());
                            prepareNextSegment(buffer);
                        } finally {
                            // 同步状态时加写锁
                            buffer.getLock().writeLock().lock();
                            buffer.getThreadRunning().set(false);
                            buffer.setNextReady(true);
                            buffer.getLock().writeLock().unlock();
                        }
                    });
                }

                val id = buffer.getCurrent().getValue().incrementAndGet();
                if (id < buffer.getCurrent().getMax()) {
                    return new Result(id, Status.SUCCESS);
                }
            } finally {
                buffer.getLock().readLock().unlock();
            }

            // 当id为当前segment的最大值，那么切换到下一个段
            waitAndSleep(buffer);// 等一下，避免线程空转
            buffer.getLock().writeLock().lock(); // 加写锁
            try {
                // 再次尝试获取，如果还没超出 max，说明其他线程已完成切换
                val newId = buffer.getCurrent().getValue().incrementAndGet();
                if (newId < buffer.getCurrent().getMax()) {
                    return new Result(newId, Status.SUCCESS);
                }

                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                    log.info("[Segment UID] 切换号段完成，新段起始值：{}", buffer.getCurrent().getValue().get());
                } else {
                    log.error("Both two segments in {} are not ready!", buffer);
                    return new Result(EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL, Status.EXCEPTION);
                }
            } finally {
                buffer.getLock().writeLock().unlock();
            }
        }
    }

    /**
     * 准备下一段segment，buffer.getThreadRunning()的状态由调用者维护
     * @param buffer buffer
     */
    private void prepareNextSegment(SegmentBuffer buffer) {
//        buffer.getThreadRunning().set(true); //调用方法的时候已经compareAndSet了
        val nextMaxId = dao.nextMaxId(buffer.getKey(), properties.getStep());
        buffer.getNext().setMax(nextMaxId);
        buffer.getNext().setStep(properties.getStep());
        buffer.getNext().getValue().set(nextMaxId - properties.getStep());
        // 运行结束，修改运行标识
//        buffer.getThreadRunning().set(false); //调用方法的时候会在finally中修改标识，所以此处就注掉了
//        buffer.setNextReady(true);

    }

    private void waitAndSleep(SegmentBuffer buffer) {
        int roll = 0;
        while (buffer.getThreadRunning().get()) {
            roll += 1;
            if(roll > 10000) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                    break;
                } catch (InterruptedException e) {
                    log.warn("Thread {} Interrupted",Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

//    /**
//     * 等待线程运行结束
//     * @param buffer buffer
//     */
//    private void waitAndSleep(SegmentBuffer buffer) {
//        long startTime = System.currentTimeMillis();
//        long timeout = 50L; // 最长允许等待时间
//        boolean waitOver = false;
//
//        int roll = 0;
//        while (buffer.getThreadRunning().get()) {
//            roll++;
//            if (roll <= 10000) {
//                Thread.yield(); // 空转让出 CPU
//                continue;
//            }
//
//            if (System.currentTimeMillis() - startTime > timeout) {
//                waitOver = true;
//            } else {
//                try {
//                    TimeUnit.MILLISECONDS.sleep(1); // 精细 sleep
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    waitOver  = true;
//                }
//            }
//
//            if (waitOver) {
//                if (buffer.getThreadRunning().get()) {
//                    if (Thread.currentThread().isInterrupted()) {
//                        log.warn("[Segment UID] 等待期间线程被中断，下一段仍未就绪");
//                        throw new IllegalStateException("Segment waiting interrupted and next segment not ready");
//                    } else {
//                        log.warn("[Segment UID] 等待 {}ms 后下一段仍未就绪", timeout);
//                        throw new IllegalStateException("当前号段已用尽，下一段未就绪，请稍后再试");
//                    }
//                } else {
//                    break;
//                }
//            }
//        }
//    }
//

    /**
     * 采用懒加载的模式。初始化分为两步
     * 1，在项目启动时先初始化cache，里面的SegmentBuffer都是空的
     * 2，当第一次请求对应的tag的时候，再去初始化对应的SegmentBuffer
     *
     * @return 是否成功
     */
    @Override
    public boolean init() {
        updateCacheFromDb();
        initOk.set(true);
        updateCacheFromDbAtEveryMinute();
        return true;
    }

    /**
     * 通过数据库更新cache，去掉已经删除的tag，添加新的tag
     */
    private void updateCacheFromDb() {
        val allTags = dao.getAllTags();
        Set<String> currentTags = new HashSet<>(cache.keySet());
        // 新增
        Set<String> newTags = new HashSet<>(allTags);
        newTags.removeAll(currentTags);
        // 删除
        Set<String> removedTags = new HashSet<>(currentTags);
        allTags.forEach(removedTags::remove);

        newTags.forEach(tag -> cache.put(tag, new SegmentBuffer(tag)));
        removedTags.forEach(cache::remove);
    }

    /**
     * 每分钟刷新一次cache
     */
    private void updateCacheFromDbAtEveryMinute() {
        segmentUidScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateCacheFromDb();
            }
        }, 60, 60, TimeUnit.SECONDS);

    }

}
