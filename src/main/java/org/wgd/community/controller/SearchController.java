package org.wgd.community.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.model.pojo.Page;
import org.wgd.community.service.impl.EsServiceImpl;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.service.impl.UserServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController extends BaseInfoProperties {

    @Autowired
    private EsServiceImpl esService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RedisServiceImpl redisService;

    // search?keyword=xxx
    @GetMapping(path = "/search")
    public String search(String keyword, Page page, Model model) {
        // 分页搜索帖子
        // page从1开始，es的from从1开始，所以需要-1
        SearchPage<DiscussPost> searchPage = esService.pageDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        List<Map<String, Object>> discussPosts = new ArrayList<Map<String, Object>>();
        if (searchPage != null) {
            for (SearchHit<DiscussPost> discussPostSearchHit : searchPage) {
                Map<String, Object> map = new HashMap<>();
                //帖子
                DiscussPost post = discussPostSearchHit.getContent();
                map.put("post", post);
                // 作者
                map.put("user", userService.getUser(post.getUserId()));
                //点赞
                map.put("likeCount", redisService.countEntityLike(EntityTypeEnum.POST.getType(), post.getId()));

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchPage == null ? 0 : (int) searchPage.getTotalElements());

        return "/site/search";
    }

}
