package com.jiang.friendsGatheringBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 自定义 redisTemplate 解决默认redisTemplate模板采用 JDK自带的序列化方法引起的乱码问题
 *
 * @author jiang
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        //配置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 设置序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer();
        // key 和 hashKey 均采用 string 序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // value 和 hashValue 均采用 JSON 序列化
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        //执行这个函数初始化 redisTemplate
        //redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
