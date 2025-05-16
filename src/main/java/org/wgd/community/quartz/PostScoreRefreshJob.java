package org.wgd.community.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.service.DiscussPostService;
import org.wgd.community.service.impl.EsServiceImpl;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class PostScoreRefreshJob extends BaseInfoProperties implements Job {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisServiceImpl redisService;

    @Autowired
    private EsServiceImpl esService;

    /**
     * 初始时间，计算权重的，越新的帖子权重越大
     */
    private static Date code;

    static {
        try {
            // 最喜欢的2018年的夏天，6月7日正是我高考那天
            code = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-06-07 00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String key = RedisKeyUtils.getPostScoreKey();
        // 获取待更新分数的所有userId
        BoundSetOperations boundSetOperations = redisTemplate.boundSetOps(key);

        if (boundSetOperations.size() == 0) {
            log.info("[任务取消]：没有需要更新的帖子");
            return;
        }

        log.info("[任务开始]：待刷新帖子 - " + boundSetOperations.size());
        while (boundSetOperations.size() > 0) {
            // pop 顺便清理set集合，所以就不需要在删除了
            this.refresh((Integer) boundSetOperations.pop());
        }
        log.info("[任务结束]：帖子分数刷新完毕！");
    }

    private void refresh(Integer postId) {
        DiscussPost post = discussPostService.getById(postId);

        if (post == null) {
            log.error("该帖子不存在: id = " + postId);
            return;
        }

        if(post.getStatus() == 2){
            log.error("帖子已被删除");
            return;
        }

        // 是否精华
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getCommentCount();
        // 点赞数量
        long likeCount = redisService.countEntityLike(EntityTypeEnum.POST.getType(), postId);

        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - code.getTime()) / (1000 * 3600 * 24);
        // 更新帖子分数
        discussPostService.updateScore(postId, score);
        // 同步搜索数据
        post.setScore(score);
        esService.save(post);
    }
}
