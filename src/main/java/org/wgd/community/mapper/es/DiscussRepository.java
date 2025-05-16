package org.wgd.community.mapper.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.wgd.community.model.pojo.DiscussPost;

public interface DiscussRepository extends ElasticsearchRepository<DiscussPost,Integer> {
}
