package com.wenziyue.uid.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UID 生成器的配置属性类。
 *
 * @author wenziyue
 */
@Data
@ConfigurationProperties(prefix = "wenziyue.uid")
public class UidGeneratorProperties {

//    /**
//     * UID 生成策略，支持 "segment" 或 "snowflake"。
//     */
//    private String mode = "segment";

    /**
     * 业务标签，用于区分不同的业务线。
     */
    private String bizTag = "default";

    /**
     * 步长，适用于 Segment 模式。
     */
    private int step = 1000;

    /**
     * 初始 ID，适用于 Segment 模式。
     */
    private long initId = 1L;

//    /**
//     * 工作节点 ID，适用于 Snowflake 模式。
//     */
//    private long workerId = 1L;
//
//    /**
//     * 数据中心 ID，适用于 Snowflake 模式。
//     */
//    private long datacenterId = 1L;

    /**
     * 是否自动创建表，适用于 Segment 模式。
     */
    private boolean autoCreateTable = true;

}
