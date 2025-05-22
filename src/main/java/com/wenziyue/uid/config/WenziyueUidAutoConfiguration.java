package com.wenziyue.uid.config;

import com.wenziyue.uid.core.IdGen;
import com.wenziyue.uid.properties.UidGeneratorProperties;
import com.wenziyue.uid.segment.SegmentBuffer;
import com.wenziyue.uid.segment.SegmentIdDao;
import com.wenziyue.uid.segment.SegmentIdDaoImpl;
import com.wenziyue.uid.segment.SegmentIdGeneratorImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author wenziyue
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UidGeneratorProperties.class)
public class WenziyueUidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SegmentIdDaoImpl segmentIdDao(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        return new SegmentIdDaoImpl(jdbc);
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("segment-pool-");
        executor.initialize(); // ✅ 记得初始化
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(name = "segmentUidScheduler")
    public ScheduledExecutorService segmentUidScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("segment-uid-cache-refresh");
            t.setDaemon(true);
            return t;
        });
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public SegmentIdGeneratorImpl segmentIdGenerator(
//            UidGeneratorProperties properties,
//            SegmentIdDao segmentIdDao,
//            ThreadPoolTaskExecutor taskExecutor,
//            ScheduledExecutorService segmentUidScheduler
//    ) {
//        return new SegmentIdGeneratorImpl(
////                new ConcurrentHashMap<>(),
//                properties,
//                segmentIdDao,
//                taskExecutor,
//                segmentUidScheduler
//        );
//    }

    @Bean
    @ConditionalOnMissingBean
    public IdGen idGen(SegmentIdGeneratorImpl segmentIdGenerator) {
        return segmentIdGenerator;
    }

}
