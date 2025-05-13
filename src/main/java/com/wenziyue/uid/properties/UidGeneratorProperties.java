package com.wenziyue.uid.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UID 生成器相关配置项
 *
 * @author wenziyue
 */
@ConfigurationProperties(prefix = "wenziyue.uid")
@Getter
@Setter
public class UidGeneratorProperties {

    /**
     * 使用缓存号段还是普通号段
     * true  -> CachedUidGenerator
     * false -> DefaultUidGenerator
     */
    private boolean useCached = false;

    /**
     * 业务标识，默认 "default"
     */
    private String bizTag = "default";

    /**
     * 号段步长，默认 1000
     */
    private int step = 1000;

    /**
     * 是否依赖flyway自动创建表，默认 true
     */
    private boolean autoCreateTable = true;

    /**
     * 初始ID（默认从1开始）
     */
    private long initId = 1L;

}
