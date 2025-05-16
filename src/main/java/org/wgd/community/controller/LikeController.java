package org.wgd.community.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.common.enums.YesOrNo;
import org.wgd.community.event.EventProducer;
import org.wgd.community.model.pojo.Event;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.RedisService;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.util.HostHolder;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController extends BaseInfoProperties {
    @Autowired
    private RedisServiceImpl redisService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId,int entityUserId ,int postId) {
        User user = hostHolder.getUser();

        // 点赞
        redisService.like(user.getId(), entityType, entityId,entityUserId);

        // 数量
        long likeCount = redisService.countEntityLike(entityType, entityId);
        // 状态
        int likeStatus = redisService.getEntityLikedStatus(user.getId(), entityType, entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 发送站内信-kafka异步
        // 只有当点赞才会触发事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            // 生产
            eventProducer.fireEvent(event);
        }

        if (entityType == EntityTypeEnum.POST.getType()) {
            // 将需要重新计算帖子分数的postid存入redis中
            String redisKey = RedisKeyUtils.getPostScoreKey();
            redisUtil.sSet(redisKey,postId);
        }

        return GraceJSONResult.okJson(map);
    }
}
