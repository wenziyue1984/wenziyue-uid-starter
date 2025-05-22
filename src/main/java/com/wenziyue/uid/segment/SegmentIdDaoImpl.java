package com.wenziyue.uid.segment;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wenziyue
 */
@Repository
@RequiredArgsConstructor
public class SegmentIdDaoImpl implements SegmentIdDao{

    private final JdbcTemplate jdbc;

    /** 固定表名，不再做配置 */
    private static final String TABLE = "leaf_alloc";

    /** 拉号段：update max_id，并返回新的 max_id */
    public long nextMaxId(String bizTag, int step) {
        int updated = jdbc.update(
                "UPDATE " + TABLE + " SET max_id = max_id + ?, step = ? WHERE biz_tag = ?",
                step, step, bizTag
        );
        if (updated == 0) {           // 业务第一次使用，插一行
            jdbc.update(
                    "INSERT INTO " + TABLE + " (biz_tag, max_id, step) VALUES (?, ?, ?)",
                    bizTag, step, step
            );
        }
        return jdbc.queryForObject(
                "SELECT max_id FROM " + TABLE + " WHERE biz_tag = ?",
                Long.class,
                bizTag
        );
    }

    @Override
    public List<String> getAllTags() {
        return jdbc.queryForList("SELECT biz_tag FROM" + TABLE, String.class);
    }
}
