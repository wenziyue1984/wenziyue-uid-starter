package com.wenziyue.uid.utils;

import com.wenziyue.uid.segment.SegmentUidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author wenziyue
 */
@Component
@RequiredArgsConstructor
public class UidUtils {

    private final SegmentUidGenerator generator;

    public long nextId() {
        return generator.nextId();
    }
}
