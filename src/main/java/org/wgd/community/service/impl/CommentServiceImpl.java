package org.wgd.community.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.controller.interceptor.SensitiveFilter;
import org.wgd.community.mapper.CommentMapper;
import org.wgd.community.model.pojo.Comment;
import org.wgd.community.model.pojo.ReplyInfo;
import org.wgd.community.service.CommentService;
import org.wgd.community.service.DiscussPostService;

import java.util.List;


@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    @Override
    public List<Comment> pageByEntityTypeAndEntityId(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectByEntityTypeAndEntityId(entityType, entityId, offset, limit);
    }

    @Override
    public int countByEntityTypeAndEntityId(int entityType, int entityId) {
        return commentMapper.countByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int insert(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        if (comment.getTargetId() == null) {
            comment.setTargetId(0);
        }
        int rows = commentMapper.insertComment(comment);

        // 更新帖子评论数量
        if (comment.getEntityType() == EntityTypeEnum.POST.getType()) {
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }

    @Override
    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    @Override
    public Comment getComment(int commentId) {
        return commentMapper.selectByPrimaryKey(commentId);
    }

    /**
     * 根据用户id分页查询帖子回复列表
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<ReplyInfo> findReplyInfoList(int userId, int offset, int limit){
        return commentMapper.selectDiscussPostCommentByUserId(userId,offset,limit);
    }

    /**
     * 根据用户id查询用户帖子回复总数
     * @param userId
     * @return
     */
    @Override
    public int findReplyInfoCount(int userId){
        return commentMapper.selectDiscussPostCommentCount(userId);
    }
}

