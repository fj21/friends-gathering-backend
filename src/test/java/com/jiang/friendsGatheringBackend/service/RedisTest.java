package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Test
    void testRedis(){
        //增
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("jiangInt",1);
        valueOperations.set("jiangDouble",2.3);

        User user = new User();
        user.setUsername("jiangUser");
        user.setUserPassword("12345678");
        valueOperations.set("jiangUser",user);
        //查
        Object jiang = valueOperations.get("jiangInt");
        Assertions.assertTrue(1==(Integer)jiang);
        jiang = valueOperations.get("jiangDouble");
        Assertions.assertTrue(2.3==(Double)jiang);
        System.out.println(valueOperations.get("jiangUser"));

    }
}
