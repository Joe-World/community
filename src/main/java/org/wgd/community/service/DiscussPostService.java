package org.wgd.community.service;

import org.wgd.community.model.dto.DiscussPostDto;
import org.wgd.community.model.pojo.DiscussPost;

import java.util.List;

public interface DiscussPostService {
    List<DiscussPost> pageList(Integer userId, int pageNo, int pageSize,int orderMode);

    int count(Integer userId);

    int save(DiscussPost post);

    DiscussPost getById(Integer id);

    public int updateCommentCount(int id, int commentCount);

    int updateType(int id, int type);

    int updateStatus(int id, int status);

    int updateScore(int id, double score);

    List<DiscussPost> getAll();
}
