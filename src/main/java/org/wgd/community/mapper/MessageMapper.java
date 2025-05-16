package org.wgd.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wgd.community.model.pojo.Message;

@Mapper
public interface MessageMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Message record);

    Message selectByPrimaryKey(Integer id);

    List<Message> selectAll();

    int updateByPrimaryKey(Message record);

    /**
     * 分页查询会话列表
     * @param userId
     * @param no
     * @param size
     * @return
     */
    List<Message> pageConversationsByUserId(Integer userId, int no, int size);

    /**
     * 计数当前这个用户的会话数量
     * @param userId
     * @return
     */
    int countConversations(Integer userId);

    /**
     * 分页查询私信
     * @param conversationId
     * @param no
     * @param size
     * @return
     */
    List<Message> pageLettersByConversationId(@Param("conId") String conversationId, @Param("no") int no, @Param("size")int size);

    /**
     * 计数某个会话私信数量
     * @param conversationId
     * @return
     */
    int countLettersByConversationId(String conversationId);

    /**
     * 计数未读消息（会话的未读/私信的未读）
     * @param userId
     * @param conversationId
     * @return
     */
    int countUnreadInfo(Integer userId, String conversationId);


    /**
     * 批量更新消息状态
     * @param ids
     * @param status
     * @return
     */
    int updateStatusesByIds(List<Integer> ids, int status);

    /**
     * 查询某个主题下最新的通知
     * @param userId
     * @param topic
     * @return
     */
    Message selectLatestMessage(int userId, String topic);

    /**
     * 查询某个主题的通知数量
     * @param userId
     * @param topic
     * @return
     */
    int countMessage(int userId, String topic);

    /**
     * 查询某个主题未读的消息数量
     * @param userId
     * @param topic
     * @return
     */
    int countUnReadSystemInfo(int userId, String topic);

    /**
     * 查询某个主题所包含的通知列表
     * @param userId
     * @param topic
     * @param offset
     * @param limit
     * @return
     */
    List<Message> listMessages(int userId, String topic, int offset, int limit);

}