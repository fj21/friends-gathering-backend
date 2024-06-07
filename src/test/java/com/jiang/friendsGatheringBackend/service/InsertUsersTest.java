package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.mapper.UserMapper;
import com.jiang.friendsGatheringBackend.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class InsertUsersTest {

    @Resource
    UserService userService;

    @Resource
    UserMapper userMapper;

    @Test
    void insertUser(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<User> userList = new ArrayList<>();
        final int Insert_Num = 1000;
        for(int i=0;i<Insert_Num;i++){
            User user = new User();
            user.setUserAccount("jiang..");
            user.setUserPassword("12345678");
            user.setAvatarUrl("https://pic-saving-bucket.oss-cn-chengdu.aliyuncs.com/img/image-20240530232845785.png");
            user.setUserRole(0);
            user.setEmail("...@qq.com");
            user.setPhone("123");
            user.setUserStatus(0);
            user.setPlanetCode("1111");
            user.setTags("[]");
            userList.add(user);
            userMapper.insert(user);
        }
        // userService.saveBatch(userList,100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
        // total:10w  batchSize: 1w  开启mybatis日志 125.866s
        // total:10w  batchSize: 1w  关闭mybatis日志 119.455s
        // total:10w  batchSize: 1000  关闭mybatis日志 111.250s
        // total:10w  batchSize: 1000  关闭mybatis日志  将stopWatch放在userList的生成之后 121.552s
        // total:1000  batchSize: 100  开启mybatis日志 2.938s
        // total:1000  batchSize: 100  关闭mybatis日志 2.611s
        // total:1000  非批量插入        开启mybatis日志 9.190s
    }
}
