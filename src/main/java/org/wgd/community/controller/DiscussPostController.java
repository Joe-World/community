package org.wgd.community.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.ResponseStatusEnum;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.event.EventProducer;
import org.wgd.community.model.pojo.*;
import org.wgd.community.service.CommentService;
import org.wgd.community.service.DiscussPostService;
import org.wgd.community.service.RedisService;
import org.wgd.community.service.UserService;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.util.HostHolder;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/discuss")
public class DiscussPostController extends BaseInfoProperties {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RedisServiceImpl redisService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return GraceJSONResult.errorCustomJson(ResponseStatusEnum.UN_LOGIN);
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        post.setType(0);
        post.setStatus(0);
        post.setCommentCount(0);
        post.setScore(0.0);

        discussPostService.save(post);


        // 触发更新es-交给kafka异步处理
        Event event = new Event()
                .setTopic(TOPIC_UPDATE_ES)
                // 发帖人
                .setUserId(user.getId())
                // entity类型
                .setEntityType(EntityTypeEnum.POST.getType())
                // 帖子id
                .setEntityId(post.getId());
        // 生产消息
        eventProducer.fireEvent(event);

        // 将需要重新计算帖子分数的postid存入redis中
        String redisKey = RedisKeyUtils.getPostScoreKey();
        redisUtil.sSet(redisKey, post.getId());

        // 报错的情况,将来统一处理.
        return GraceJSONResult.okJson();
    }

    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") Integer postId, Model model, Page page) {
        DiscussPost post = discussPostService.getById(postId);

        // 判断帖子是否是被删除状态
        if (post.getStatus() == 2) {
            return "/denied";
        }

        User user = userService.getUser(Integer.valueOf(post.getUserId()));
        // 点赞
        long likedCount = redisService.countEntityLike(EntityTypeEnum.POST.getType(), post.getId());
        // 点赞状态
        int likeStatus = hostHolder.getUser() == null
                ? 0
                : redisService.getEntityLikedStatus(hostHolder.getUser().getId(), EntityTypeEnum.POST.getType(), postId);
        model.addAttribute("likeStatus", likeStatus);


        model.addAttribute("post", post);
        model.addAttribute("user", user);
        model.addAttribute("likeCount", likedCount);
        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + postId);
        page.setRows(post.getCommentCount());

        // 评论: 给帖子的评论
        // 回复: 给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.pageByEntityTypeAndEntityId(
                EntityTypeEnum.POST.getType(), post.getId(), page.getOffset(), page.getLimit());
        // 评论VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 评论VO
                Map<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 作者
                commentVo.put("user", userService.getUser(comment.getUserId()));

                // 点赞
                likedCount = redisService.countEntityLike(EntityTypeEnum.COMMENT.getType(), comment.getId());
                // 点赞状态
                likeStatus = hostHolder.getUser() == null
                        ? 0
                        : redisService.getEntityLikedStatus(hostHolder.getUser().getId(), EntityTypeEnum.COMMENT.getType(), comment.getId());
                commentVo.put("likeCount", likedCount);
                commentVo.put("likeStatus", likeStatus);

                // 回复列表
                List<Comment> replyList = commentService.pageByEntityTypeAndEntityId(
                        EntityTypeEnum.COMMENT.getType(), comment.getId(), 0, Integer.MAX_VALUE);
                // 回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 作者
                        replyVo.put("user", userService.getUser(reply.getUserId()));
                        // 回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.getUser(reply.getTargetId());
                        replyVo.put("target", target);

                        // 点赞
                        likedCount = redisService.countEntityLike(EntityTypeEnum.COMMENT.getType(), reply.getId());
                        replyVo.put("likeCount", likedCount);
                        // 点赞状态
                        likeStatus = hostHolder.getUser() == null
                                ? 0
                                : redisService.getEntityLikedStatus(hostHolder.getUser().getId(), EntityTypeEnum.COMMENT.getType(), reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);

                // 回复数量
                int replyCount = commentService.countByEntityTypeAndEntityId(EntityTypeEnum.COMMENT.getType(), comment.getId());
                commentVo.put("replyCount", replyCount);

                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }

    // 置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        discussPostService.updateType(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_UPDATE_ES)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(EntityTypeEnum.POST.getType())
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return GraceJSONResult.okJson();
    }

    // 加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateStatus(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_UPDATE_ES)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(EntityTypeEnum.POST.getType())
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 将需要重新计算帖子分数的postid存入redis中
        String redisKey = RedisKeyUtils.getPostScoreKey();
        redisUtil.sSet(redisKey,id);

        return GraceJSONResult.okJson();
    }

    // 删除
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE_ES)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(EntityTypeEnum.POST.getType())
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return GraceJSONResult.okJson();
    }
}
