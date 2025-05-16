package org.wgd.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.wgd.community.model.pojo.Comment;
import org.wgd.community.model.pojo.ReplyInfo;

@Mapper
public interface CommentMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Comment record);

    Comment selectByPrimaryKey(Integer id);

    List<Comment> selectAll();

    int updateByPrimaryKey(Comment record);

    List<Comment> selectByEntityTypeAndEntityId(int entityType, int entityId, int offset, int limit);

    int countByEntityTypeAndEntityId(int entityType, int entityId);

    int insertComment(Comment comment);

    int selectCountByEntity(int entityType, int entityId);


    /**
     * 根据用户id查询帖子评论回复列表
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    List<ReplyInfo> selectDiscussPostCommentByUserId(int userId, int offset, int limit);

    /**
     * 根据用户id查询帖子评论回复总数
     * @param userId
     * @return
     */
    int selectDiscussPostCommentCount(int userId);
}