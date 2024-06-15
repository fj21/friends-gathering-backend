package com.jiang.friendsGatheringBackend.service;

import com.jiang.friendsGatheringBackend.util.AlgrithomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class AlgrithomUtilsTest {



    @Test
    void test(){
        //- 用户A：[Java，大一，男]
        //- 用户B：[Java，大二，女]
        //- 用户C：[Python，大二，女]
        //- 用户D：[Java，大一，女]
        List<String> tagNameList = Arrays.asList("Java", "大一", "男");
        List<String> tagNameList1 = Arrays.asList("Java", "大二", "女");
        List<String> tagNameList2 = Arrays.asList("Python", "大二", "女");
        List<String> tagNameList3 = Arrays.asList("Java", "大一", "女");
        // score1 -2
        int score1 = AlgrithomUtils.minDistance(tagNameList, tagNameList1);
        // score2 -3
        int score2 = AlgrithomUtils.minDistance(tagNameList, tagNameList2);
        // score3 -1
        int score3 = AlgrithomUtils.minDistance(tagNameList, tagNameList3);

        System.out.println(score1);
        System.out.println(score2);
        System.out.println(score3);
    }
}
