package com.wenziyue.uid.config;

import com.wenziyue.uid.properties.UidGeneratorProperties;
import com.wenziyue.uid.segment.SegmentIdDao;
import com.wenziyue.uid.segment.SegmentUidGenerator;
import com.wenziyue.uid.utils.UidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wenziyue
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UidGeneratorProperties.class)
public class WenziyueUidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SegmentIdDao segmentIdDao(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        return new SegmentIdDao(jdbc);
    }

    @Bean
    @ConditionalOnMissingBean
    public SegmentUidGenerator segmentUidGenerator(SegmentIdDao dao, UidGeneratorProperties props) {
        return new SegmentUidGenerator(dao, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public UidUtils uidUtils(SegmentUidGenerator generator) {
        return new UidUtils(generator);
    }

}
