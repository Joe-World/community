package org.wgd.community.service;

import org.wgd.community.model.pojo.Message;

import java.util.List;

public interface MessageService {
    public List<Message> pageConversationsByUserId(int userId, int offset, int limit);

    public int countConversations(int userId);

    public List<Message> pageLettersByConversationId(String conversationId, int offset, int limit);

    public int countLettersByConversationId(String conversationId);

    public int countUnreadInfo(int userId, String conversationId);

    public int add(Message message);

    public int updateMessagesByIds(List<Integer> ids);


    public Message getLatestMessage(int userId, String topic);

    public int countMessage(int userId, String topic);

    public int countUnReadSystemInfo(int userId, String topic);

    public List<Message> listMessageByTopic(int userId, String topic, int offset, int limit);

}
