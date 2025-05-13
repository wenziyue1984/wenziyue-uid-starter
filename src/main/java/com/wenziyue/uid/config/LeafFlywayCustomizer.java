package com.wenziyue.uid.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 负责把 starter 自己的脚本目录追加到 Flyway 的扫描路径
 *
 * @author wenziyue
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "wenziyue.uid", name = "auto-create-table", havingValue = "true", matchIfMissing = true)
public class LeafFlywayCustomizer {

    @Bean
    public FlywayConfigurationCustomizer appendUidScriptsPath() {
        return (FluentConfiguration cfg) -> {
            // 1. 取出原有的 locations
            List<Location> list = new ArrayList<>(Arrays.asList(cfg.getLocations()));
            // 2. 追加我们自己的脚本目录
            list.add(new Location("classpath:wzyuid/db"));
            // 3. 覆盖回去
            cfg.locations(list.toArray(new Location[0]));
        };
    }
}
