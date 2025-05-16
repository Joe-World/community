package org.wgd.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.ResponseStatusEnum;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.event.EventProducer;
import org.wgd.community.model.pojo.Event;
import org.wgd.community.model.pojo.Page;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.UserService;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.util.HostHolder;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController extends BaseInfoProperties {
    @Autowired
    private RedisServiceImpl redisService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        redisService.follow(user.getId(), entityType, entityId);

        // 发送站内信，kafka异步
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        // 发送
        eventProducer.fireEvent(event);

        return GraceJSONResult.okJson(ResponseStatusEnum.FOLLOW_SUCCESS);
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        redisService.unfollow(user.getId(), entityType, entityId);

        return GraceJSONResult.okJson(ResponseStatusEnum.CANCEL_FOLLOW_SUCCESS);
    }

    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollows(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.getUser(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) redisService.countUserFollow(userId, EntityTypeEnum.USER.getType()));

        List<Map<String, Object>> userList = redisService.getFollows(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }

        model.addAttribute("users", userList);
        model.addAttribute("followCount", page.getRows());
        model.addAttribute("fanCount", redisService.countUserFan(EntityTypeEnum.USER.getType(), userId));

        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFans(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.getUser(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) redisService.countUserFan(EntityTypeEnum.USER.getType(), userId));

        List<Map<String, Object>> userList = redisService.getFans(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        model.addAttribute("fanCount", page.getRows());
        model.addAttribute("followCount", redisService.countUserFollow(userId, EntityTypeEnum.USER.getType()));

        return "/site/follower";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }

        return redisService.isFollowed(hostHolder.getUser().getId(), EntityTypeEnum.USER.getType(), userId);
    }

}
