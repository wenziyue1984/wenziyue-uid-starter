package com.wenziyue.uid.config;

import com.wenziyue.uid.properties.UidGeneratorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author wenziyue
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeafTableInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UidGeneratorProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isAutoCreateTable()) {
            log.info("[LeafTableInitializer] autoCreateTable=false, 跳过 leaf_alloc 初始化");
            return;
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM leaf_alloc WHERE biz_tag = ?",
                    Integer.class,
                    properties.getBizTag()
            );

            if (count == null || count == 0) {
                jdbcTemplate.update("INSERT INTO leaf_alloc (biz_tag, max_id, step, update_time) VALUES (?, ?, ?, NOW())",
                        properties.getBizTag(), properties.getInitId(), properties.getStep());
                log.info("[LeafTableInitializer] 已初始化 leaf_alloc 数据, bizTag={}, startId={}.", properties.getBizTag(), properties.getInitId());
            } else {
                log.info("[LeafTableInitializer] leaf_alloc 中已存在 bizTag={}, 跳过初始化.", properties.getBizTag());
            }
        } catch (Exception e) {
            log.warn("[LeafTableInitializer] 启动时初始化 leaf_alloc 数据失败，可能是表还未建好，可忽略", e);
        }
    }
}
