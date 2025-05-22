package com.wenziyue.uid.segment;

import java.util.List;

/**
 * 访问 leaf_alloc 表的接口
 *
 * @author wenziyue
 */
public interface SegmentIdDao {

    /**
     * 获取下一段号段最大值（返回 max_id）
     * 示例：当前 max=1000，step=100，则返回 1100
     */
    long nextMaxId(String bizTag, int step);

    List<String> getAllTags();
}
