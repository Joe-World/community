package org.wgd.community.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.event.EventProducer;
import org.wgd.community.model.pojo.Comment;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.model.pojo.Event;
import org.wgd.community.service.CommentService;
import org.wgd.community.service.impl.CommentServiceImpl;
import org.wgd.community.service.impl.DiscussPostServiceImpl;
import org.wgd.community.util.HostHolder;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController extends BaseInfoProperties {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostServiceImpl discussPostService;

    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.insert(comment);

        // 触发站内信-交给kafka异步处理
        // 组装评论event
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        // 根据entityId找到真正接受消息的用户
        if (comment.getEntityType() == EntityTypeEnum.POST.getType()) {
            DiscussPost target = discussPostService.getById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == EntityTypeEnum.COMMENT.getType()) {
            Comment target = commentService.getComment(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        // 调用生产者发送消息
        eventProducer.fireEvent(event);


        // 若评论的是帖子，才会真正的修改帖子评论数，也就是需要修改es中的post实体
        if (comment.getEntityType() == EntityTypeEnum.POST.getType()) {
            // 触发发帖事件
            event = new Event()
                    .setTopic(TOPIC_UPDATE_ES)
                    .setUserId(comment.getUserId())
                    .setEntityType(EntityTypeEnum.POST.getType())
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);

            // 将需要重新计算帖子分数的postid存入redis中
            String redisKey = RedisKeyUtils.getPostScoreKey();
            redisUtil.sSet(redisKey,discussPostId);
        }


        return "redirect:/discuss/detail/" + discussPostId;
    }

}
