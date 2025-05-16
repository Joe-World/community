package org.wgd.community;

import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.wgd.community.mapper.DiscussPostMapper;
import org.wgd.community.mapper.es.DiscussRepository;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.service.impl.DiscussPostServiceImpl;
import org.wgd.community.util.JacksonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class EsTest {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussRepository discussRepository;

    //    @Qualifier("client")
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private DiscussPostServiceImpl discussPostService;


    //判断某id的文档（数据库中的行）是否存在
    @Test
    public void testExist() throws InterruptedException {
//        discussRepository.deleteAll();
        elasticsearchRestTemplate.indexOps(IndexCoordinates.of("discusspost")).delete();
        elasticsearchRestTemplate.save(discussPostService.getAll());

    }



    @Test
    public void testExit() {



        // 清空es数据库中的脏数据
        try {
            discussRepository.deleteAll();


            Thread.sleep(200000);
        } catch (Exception e) {

        }finally {

        }


//        elasticsearchRestTemplate.save(discussPostService.getAll());
    }

    //一次保存一条数据
    @Test
    public void testInsert() {
        //把id为241的DiscussPost的对象保存到discusspost索引（es的索引相当于数据库的表）
//        discussRepository.save(discussPostMapper.selectByPrimaryKey(241));
//        discussRepository.save(discussPostMapper.selectByPrimaryKey(110));
//        discussRepository.deleteAll();
//        discussRepository.saveAll(discussPostMapper.pageList(null, 0, 20));
        for (int i = 1; i < 9; i++) {
            discussRepository.save(discussPostMapper.selectByPrimaryKey(110 + i));
        }
//        discussRepository.saveAll(discussPostMapper.pageList(null,0))
    }


    //一次保存多条数据
    @Test
    public void testInsertList() {
        //把id为101的用户发的前100条帖子（List<DiscussPost>）存入es的discusspost索引（es的索引相当于数据库的表）
//        discussRepository.saveAll(discussPostMapper.selectAll(101, 0, 100));
    }

    //
//    //通过覆盖原内容，来修改一条数据
//    @Test
//    public void testUpdate() {
//        DiscussPost post = discussMapper.selectDiscussPostById(230);
//        post.setContent("我是新人,使劲灌水。");
//        post.setTitle(null);//es中的title会设为null
//        discussRepository.save(post);
//    }
//
//    //修改一条数据
//    //覆盖es里的原内容 与 修改es中的内容 的区别：String类型的title被设为null，覆盖的话，会把es里的该对象的title也设为null；UpdateRequest，修改后该对象的title不变
//    @Test
//    void testUpdateDocument() throws IOException {
//        UpdateRequest request = new UpdateRequest("discusspost", "109");
//        request.timeout("1s");
//        DiscussPost post = discussMapper.selectDiscussPostById(230);
//        post.setContent("我是新人,使劲灌水.");
//        post.setTitle(null);//es中的title会保存原内容不变
//        request.doc(JSON.toJSONString(post), XContentType.JSON);
//        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
//        System.out.println(updateResponse.status());
//    }
//
//    //删除一条数据和删除所有数据
//    @Test
//    public void testDelete() {
//        discussRepository.deleteById(109);//删除一条数据
//        //discussRepository.deleteAll();//删除所有数据
//    }
//
    //不带高亮的查询
    @Test
    public void noHighlightQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

        //构建搜索条件
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
//                //在discusspost索引的title和content字段中都查询“互联网寒冬”
//                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
//                // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
//                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//                .from(0)// 指定从哪条开始查询
//                .size(10);// 需要查出的总记录条数

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                //在discusspost索引的title和content字段中都查询“互联网寒冬”
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
                // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
                .from(0)// 指定从哪条开始查询
                .size(10);// 需要查出的总记录条数


        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(JacksonUtil.getBeanToJson(searchResponse));
//
        List<DiscussPost> list = new LinkedList<>();
//        for (SearchHit hit : searchResponse.getHits().getHits()) {
//            DiscussPost discussPost = JacksonUtil.getJsonToBean(hit.getSourceAsString(), DiscussPost.class);
//            System.out.println(discussPost);
//            list.add(discussPost);
//        }
        for (SearchHit hit : searchResponse.getHits()) {
//            DiscussPost discussPost = JacksonUtil.getJsonToBean(hit.getSourceAsString(), DiscussPost.class);
            DiscussPost discussPost = JacksonUtil.getJsonToBeanIgnoreUnkown(hit.getSourceAsString(), DiscussPost.class);
            System.out.println(discussPost);
        }

    }

//    @Test
//    public void testSearchByRepository() {
//        BaseQuery baseQuery = new NativeSearchQueryBuilder()
//                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                .withPageable(PageRequest.of(0, 10))
//                .withHighlightFields(
//                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
//                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
//                ).build();
//
//        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
//        // 底层获取得到了高亮显示的值, 但是没有返回.
//
//        Page<DiscussPost> page = discussRepository.search(baseQuery);
//
//        System.out.println(page.getTotalElements());
//        System.out.println(page.getTotalPages());
//        System.out.println(page.getNumber());
//        System.out.println(page.getSize());
//        for (DiscussPost post : page) {
//            System.out.println(post);
//        }
//    }


    //带高亮的查询
    @Test
    public void highlightQuery() throws Exception {
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");

        //构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .from(0)// 指定从哪条开始查询
                .size(10)// 需要查出的总记录条数
                .highlighter(highlightBuilder);//高亮

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<DiscussPost> list = new LinkedList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
//            DiscussPost discussPost = JacksonUtil.getJsonToBean(hit.getSourceAsString(), DiscussPost.class);
            String replace = hit.getSourceAsString().replace("\"_class\":\"org.wgd.community.model.pojo.DiscussPost\",", "");
            DiscussPost discussPost = JacksonUtil.getJsonToBean(replace, DiscussPost.class);
            // 处理高亮显示的结果
            HighlightField titleField = hit.getHighlightFields().get("title");
            if (titleField != null) {
                discussPost.setTitle(titleField.getFragments()[0].toString());
            }
            HighlightField contentField = hit.getHighlightFields().get("content");
            if (contentField != null) {
                discussPost.setContent(contentField.getFragments()[0].toString());
            }
            System.out.println(discussPost);
            list.add(discussPost);
        }
    }

}
