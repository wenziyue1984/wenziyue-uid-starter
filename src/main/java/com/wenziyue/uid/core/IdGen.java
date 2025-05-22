package com.wenziyue.uid.core;

import com.wenziyue.uid.common.Result;

/**
 * 通用的 ID 生成器接口，支持多种实现策略。
 *
 * @author wenziyue
 */
public interface IdGen {

    /**
     * 获取下一个唯一 ID
     */
    Result nextId();

    /**
     * 初始化
     */
    boolean init();
}
