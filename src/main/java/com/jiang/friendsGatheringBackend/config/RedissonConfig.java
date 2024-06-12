package com.jiang.friendsGatheringBackend.config;

import lombok.Data;
import org.apache.coyote.http11.filters.SavedRequestInputFilter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    /**
     * 配置 Redisson -单节点
     * @return
     */
    @Bean
    public RedissonClient redissonClient(){
        //1.创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s",host,port);
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);
        //2.创建实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
