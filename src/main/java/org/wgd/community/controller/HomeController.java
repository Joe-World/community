package org.wgd.community.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.model.pojo.Page;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.impl.DiscussPostServiceImpl;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.service.impl.UserServiceImpl;
import org.wgd.community.model.pojo.DiscussPost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class HomeController {
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private DiscussPostServiceImpl discussPostService;

    @Autowired
    private RedisServiceImpl redisService;

    @GetMapping("/")
    public String root() {
        return "forward:/index";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index(Model model, Page page,
                        @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        // 方法调用钱,SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以,在thymeleaf中可以直接访问Page对象中的数据.
        page.setRows(discussPostService.count(null));
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService.pageList(null, page.getOffset(), page.getLimit(),orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.getUser(Integer.valueOf(post.getUserId()));
                map.put("user", user);

                // type就是1-帖子
                long likedCount = redisService.countEntityLike(EntityTypeEnum.POST.getType(), post.getId());
                map.put("likeCount", likedCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);

        return "/index";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}
