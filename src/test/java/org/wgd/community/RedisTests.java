package org.wgd.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.wgd.community.common.enums.YesOrNo;
import org.wgd.community.util.RedisUtil;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisUtil redisUtil;


    // 统计20万个重复数据的独立总数.
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";

        for (int i = 1; i <= 1000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        System.out.println(redisTemplate.opsForHyperLogLog().size("test:hll:01"));
        System.out.println("aaaaaaaaaaaaaaa");
    }
}