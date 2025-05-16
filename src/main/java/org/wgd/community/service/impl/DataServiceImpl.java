package org.wgd.community.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.wgd.community.service.DataService;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataServiceImpl implements DataService {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    @Override
    public void saveUV(String ip) {
        String key = RedisKeyUtils.getUVKey(sdf.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(key, ip);
    }

    /**
     * 获取区间网站帖子独立访客
     * @param start
     * @param end
     * @return
     */
    @Override
    public long getUV(Date start, Date end) {
        // 入参判空
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        if (start.after(end)) {
            throw new IllegalArgumentException("开始日期不能大于结束日期");
        }

        List<String> dateList = new ArrayList<>();
        // 获取从起止到截至日期所有的日期key
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        // 累加起止日期，直到超过截至日期
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtils.getUVKey(sdf.format(calendar.getTime()));
            dateList.add(key);
            calendar.add(Calendar.DATE,1);
        }
        String unionKey = RedisKeyUtils.getUVKey(sdf.format(start), sdf.format(end));
        // 合并区间日期，并去重
        redisTemplate.opsForHyperLogLog().union(unionKey,dateList.toArray());

        return redisTemplate.opsForHyperLogLog().size(unionKey);
    }


    @Override
    public void saveDAU(int userId) {
        String key = RedisKeyUtils.getDAUKey(sdf.format(new Date()));
        // 将userId作为bitmap的偏移量
        redisTemplate.opsForValue().setBit(key, userId, true);
    }

    /**
     * 获取区间网站帖子活跃用户数
     * @param start
     * @param end
     * @return
     */
    @Override
    public long getDAU(Date start, Date end) {
        // 入参判空
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        // 整理该日期范围内的key，byte[]是因为bitmap后续操作都要将key转byte数组
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtils.getDAUKey(sdf.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        return (long) redisTemplate.execute((RedisCallback) (con) -> {
            String redisKey = RedisKeyUtils.getDAUKey(sdf.format(start), sdf.format(end));
            // 对这些bitmap进行or运算
            // 需要将key转byte数组
            con.bitOp(RedisStringCommands.BitOperation.OR,
                    redisKey.getBytes(), keyList.toArray(new byte[0][0]));
            // 需要将key转byte数组
            return con.bitCount(redisKey.getBytes());
        });
    }
}
