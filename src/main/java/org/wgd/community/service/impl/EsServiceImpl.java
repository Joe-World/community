package org.wgd.community.service.impl;

import org.checkerframework.checker.units.qual.A;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.wgd.community.mapper.DiscussPostMapper;
import org.wgd.community.mapper.es.DiscussRepository;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.service.EsService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class EsServiceImpl implements EsService {
    @Autowired
    private DiscussRepository discussRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private DiscussPostServiceImpl discussPostService;

    @PostConstruct
    private void initEsAllPosts() {
        // 清空
        elasticsearchRestTemplate.indexOps(IndexCoordinates.of("discusspost")).delete();

        // 同步所有帖子
        elasticsearchRestTemplate.save(discussPostService.getAll());
    }

    @Override
    public void save(DiscussPost post) {
        discussRepository.save(post);
    }

    @Override
    public void remove(int id) {
        discussRepository.deleteById(id);
    }

    @Override
    public void update(DiscussPost post) {
        discussRepository.save(post);
    }

    @Override
    public SearchPage<DiscussPost> pageDiscussPost(String keyword, int current, int limit) {
        // 构建查询条件
        NativeSearchQuery searchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC),
                        (SortBuilders.fieldSort("score").order(SortOrder.DESC)),
                        (SortBuilders.fieldSort("createTime").order(SortOrder.DESC)))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // 得到查询结果
        SearchHits<DiscussPost> search = elasticsearchRestTemplate.search(searchQueryBuilder, DiscussPost.class);
        // 将其结果返回并进行分页
        SearchPage<DiscussPost> page = SearchHitSupport.searchPageFor(search, Page.empty().getPageable());

        if (!page.isEmpty()) {
            for (SearchHit<DiscussPost> discussPostSearch : page) {
                DiscussPost discussPost = discussPostSearch.getContent();
                // 取高亮 可能标题中有多个匹配，只需要第一个即可
                List<String> title = discussPostSearch.getHighlightFields().get("title");
                if (title != null) {
                    discussPost.setTitle(title.get(0));
                }
                List<String> content = discussPostSearch.getHighlightFields().get("content");
                if (content != null) {
                    discussPost.setContent(content.get(0));
                }
            }
        }

        return page;
    }
}
