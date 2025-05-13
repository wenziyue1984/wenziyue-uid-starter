-- 号段表
CREATE TABLE IF NOT EXISTS leaf_alloc (
                                          biz_tag        VARCHAR(128)  NOT NULL COMMENT '业务标识',
    max_id         BIGINT        NOT NULL COMMENT '当前号段最大值',
    step           INT           NOT NULL DEFAULT 1000 COMMENT '号段步长',
    update_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (biz_tag)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 节点表，可选（我们先不用雪花，但预留）
CREATE TABLE IF NOT EXISTS leaf_worker (
                                           id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           host_name      VARCHAR(64)  NOT NULL,
    port           VARCHAR(16)  NOT NULL,
    type           TINYINT      NOT NULL,
    launch_date    TIMESTAMP    NOT NULL,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;