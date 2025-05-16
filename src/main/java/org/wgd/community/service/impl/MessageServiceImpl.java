package org.wgd.community.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.wgd.community.common.enums.MessageStatusEnum;
import org.wgd.community.controller.interceptor.SensitiveFilter;
import org.wgd.community.mapper.MessageMapper;
import org.wgd.community.model.pojo.Message;
import org.wgd.community.service.MessageService;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Override
    public List<Message> pageConversationsByUserId(int userId, int offset, int limit) {
        return messageMapper.pageConversationsByUserId(userId, offset, limit);
    }

    @Override
    public int countConversations(int userId) {
        return messageMapper.countConversations(userId);
    }

    @Override
    public List<Message> pageLettersByConversationId(String conversationId, int offset, int limit) {
        return messageMapper.pageLettersByConversationId(conversationId, offset, limit);
    }

    @Override
    public int countLettersByConversationId(String conversationId) {
        return messageMapper.countLettersByConversationId(conversationId);
    }

    @Override
    public int countUnreadInfo(int userId, String conversationId) {
        return messageMapper.countUnreadInfo(userId, conversationId);
    }

    @Override
    public int add(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        if (message.getStatus() == null) {
            message.setStatus(MessageStatusEnum.UNREAD.getType());
        }
        return messageMapper.insert(message);
    }

    @Override
    public int updateMessagesByIds(List<Integer> ids) {
        return messageMapper.updateStatusesByIds(ids, MessageStatusEnum.READ.getType());
    }

    @Override
    public Message getLatestMessage(int userId, String topic) {
        return messageMapper.selectLatestMessage(userId, topic);
    }

    @Override
    public int countMessage(int userId, String topic) {
        return messageMapper.countMessage(userId, topic);
    }

    @Override
    public int countUnReadSystemInfo(int userId, String topic) {
        return messageMapper.countUnReadSystemInfo(userId, topic);
    }

    @Override
    public List<Message> listMessageByTopic(int userId, String topic, int offset, int limit) {
        return messageMapper.listMessages(userId, topic, offset, limit);
    }
}
