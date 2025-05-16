package org.wgd.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wgd.community.model.pojo.DiscussPost;

@Mapper
public interface DiscussPostMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(DiscussPost discussPost);

    DiscussPost selectByPrimaryKey(Integer id);

    List<DiscussPost> selectAll();

    int updateByPrimaryKey(DiscussPost record);

    /**
     * (根据userid),分页显示帖子
     *
     * @param userId   考虑到后面，复用方法会需要userId
     * @param pageNo   分页-当前页第一条的id
     * @param pageSize 分页-每页最大条数
     * @return
     */
    List<DiscussPost> pageList(Integer userId, int pageNo, int pageSize,int orderMode);

    /**
     * 计数所有帖子
     *
     * @param userId 用户id
     * @return
     */
    int count(@Param("userId") Integer userId);

    int updateCommentCount(int id, int commentCount);

    int updateType(int id, int type);

    int updateStatus(int id, int status);

    int updateScore(int id, double score);

    /**
     * 分页查询用户发表的帖子
     * @param userId 用户ID
     * @param offset 帖子分页起始条数
     * @param limit  每页显示的帖子条数
     * @param orderMode 是否是显示热门帖子
     * @return
     */
    List<DiscussPost> selectDiscussPostsByPage(int userId, int offset, int limit,int orderMode);

    /**
     * 查询用户发表的帖子总数
     * @param userId 用户ID
     * @return
     */
    int selectDiscussPostCount(@Param("userId") int userId);
}