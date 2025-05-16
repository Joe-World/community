package org.wgd.community.service;

import org.wgd.community.model.pojo.Comment;
import org.wgd.community.model.pojo.ReplyInfo;

import java.util.List;

public interface CommentService {
    List<Comment> pageByEntityTypeAndEntityId(int entityType, int entityId, int offset, int limit);

    int countByEntityTypeAndEntityId(int entityType, int entityId);

    int insert(Comment comment);

    int findCommentCount(int entityType, int entityId);

    Comment getComment(int commentId);

    public List<ReplyInfo> findReplyInfoList(int userId, int offset, int limit);

    public int findReplyInfoCount(int userId);
}
