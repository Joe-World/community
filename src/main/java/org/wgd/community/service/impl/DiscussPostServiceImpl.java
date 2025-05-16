package org.wgd.community.service.impl;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.wgd.community.controller.interceptor.SensitiveFilter;
import org.wgd.community.mapper.DiscussPostMapper;
import org.wgd.community.model.dto.DiscussPostDto;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.service.DiscussPostService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DiscussPostServiceImpl implements DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // LoadingCache:同步缓存，多个线程同时访问数据，数据没有，线程排队等候
    // Caffeine核心接口: Cache, LoadingCache-同步, AsyncLoadingCache-异步：支持并发

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;


    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build((key) ->{
                    // 参数判空
                    if (key == null || key.length() == 0) {
                        throw new IllegalArgumentException("参数错误!");
                    }

                    String[] params = key.split(":");
                    // 判空 + 判断俩个值是否都有
                    if (params == null || params.length != 2) {
                        throw new IllegalArgumentException("参数错误!");
                    }

                    // 页码
                    int offset = Integer.valueOf(params[0]);
                    // 每页限制
                    int limit = Integer.valueOf(params[1]);

                    // 二级缓存: Redis -> mysql

                    log.debug("load post list from DB.");

                    // 数据入本地缓存
                    return discussPostMapper.pageList(null, offset, limit, 1);
                });

        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build((key) -> {
                    log.debug("load post rows from DB.");
                    return discussPostMapper.count(key);
                });
    }

    @Override
    public List<DiscussPost> pageList(Integer userId, int pageNo, int pageSize,int orderMode) {
        // 只有当热门api，才从本地缓存取
        if (userId == null && orderMode == 1) {
            // key 定义为 ：
            return postListCache.get(pageNo + ":" + pageSize);
        }

        log.debug("load post list from DB.");

        return discussPostMapper.pageList(userId, pageNo, pageSize,orderMode);
    }

    @Override
    public int count(Integer userId) {
        if (userId == null) {
            // key定义为：
            return postRowsCache.get(0);
        }

        log.debug("load post rows from DB.");
        return discussPostMapper.count(userId);
    }

    @Override
    public int save(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insert(post);
    }

    @Override
    public DiscussPost getById(Integer id) {
        return discussPostMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    @Override
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    @Override
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    @Override
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

    @Override
    public List<DiscussPost> getAll() {
        return discussPostMapper.selectAll();
    }

    /**
     * 分页查询(用户)帖子列表
     *  使用caffeine缓存帖子列表
     * @param userId
     * @param offset
     * @param limit
     * @return List<DiscussPost>
     */
    public List<DiscussPost> findDiscussPostList(int userId,int offset,int limit,int orderMode) {
        if (userId==0&&orderMode==1){
            return postListCache.get(offset + ":" +limit);
        }
        log.debug("load post list from DB.================================>>");
        return discussPostMapper.selectDiscussPostsByPage(userId,offset,limit,orderMode);
    }

    /**
     * 根据用户id查询用户帖子总数
     *  使用caffeine缓存帖子列表
     * @param userId
     * @return
     */
    public int findDiscussPostCount(int userId){
        if (userId==0){
            return postRowsCache.get(userId);
        }

        log.debug("load post rows from db.=================================>>");
        return discussPostMapper.selectDiscussPostCount(userId);
    }

}
