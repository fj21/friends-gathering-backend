package com.jiang.friendsGatheringBackend.Job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiang.friendsGatheringBackend.model.domain.User;
import com.jiang.friendsGatheringBackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预缓存定时任务
 * 对于重点用户,每天定时执行缓存任务，以便每次重点用户获取用户推荐列表时都能直接从缓存中获取
 * 提高用户的体验
 *
 * @author jiang
 */
@Slf4j
public class PreCacheJob {

    @Resource
    RedissonClient redissonClient;

    @Resource
    RedisTemplate<String,Object> redisTemplate;

    @Resource
    UserService userService;

    //重点用户
    private List<Long> mainUserIdList = Arrays.asList(1L,2L);

    //每天执行，预热推荐用户
    //每天上午10:15触发
    @Scheduled(cron = "0 15 10 ? * * ")
    public void doCache(){
        RLock lock = redissonClient.getLock("friendGathering:PreCacheJob:doCache:lock");
        try {
            //只有一个线程能获取锁
            if(lock.tryLock(0,10, TimeUnit.SECONDS)){
                System.out.println("getLock"+Thread.currentThread().getId());
                ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                for(Long userId: mainUserIdList){
                    String redisKey = String.format("friendsGathering:user:recommend:%s", userId);
                    QueryWrapper<User> userWrapper = new QueryWrapper<>();
                    Page<User> userPage = getUsersForRecommendation(userId);
                    try {
                        valueOperations.set(redisKey,userPage,24,TimeUnit.HOURS);
                    }catch (Exception e){
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        }finally {
            //释放锁
            if(lock.isHeldByCurrentThread()){
                System.out.println("unlock"+Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    private Page<User> getUsersForRecommendation(Long userId) {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(2, 10), userWrapper);
        return userPage;
    }
}
