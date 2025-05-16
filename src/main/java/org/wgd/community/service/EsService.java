package org.wgd.community.service;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.wgd.community.model.pojo.DiscussPost;

import java.util.List;

public interface EsService {
    public void save(DiscussPost post);

    public void remove(int id);

    public void update(DiscussPost post);

    public SearchPage<DiscussPost> pageDiscussPost(String keyword, int current, int limit);
}
