package org.wgd.community.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.model.pojo.Event;
import org.wgd.community.model.pojo.Message;
import org.wgd.community.service.MessageService;
import org.wgd.community.service.impl.DiscussPostServiceImpl;
import org.wgd.community.service.impl.EsServiceImpl;
import org.wgd.community.util.JacksonUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EventConsumer extends BaseInfoProperties {
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostServiceImpl discussPostService;
    @Autowired
    private EsServiceImpl esService;

    /**
     * 一个消费者，监听消费三个主题事件
     *
     * @param record 接受相关的数据
     */
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleEventMessage(ConsumerRecord record) {
        // 判空
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }

        // 将消息josn转为java对象
        Event event = JacksonUtil.getJsonToBean(record.value().toString(), Event.class);
        // 消息格式判空
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 用来拼接最后消息内容
        Map<String, Object> content = new HashMap<>();
        // 消息触发者
        content.put("userId", event.getUserId());
        // 事件的类型 帖子/评论/人
        content.put("entityType", event.getEntityType());
        // 实体id
        content.put("entityId", event.getEntityId());

        // 若事件还有其他数据，判断事件的data是否为空，非空则把数据再次放到content里
        if (!event.getData().isEmpty()) {
            // 每次得到一个键值对
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        // 最终把content放到message里
        message.setContent(JacksonUtil.getBeanToJson(content));
        messageService.add(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_UPDATE_ES})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }

        Event event = JacksonUtil.getJsonToBean(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.getById(event.getEntityId());
        esService.save(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE_ES})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }

        Event event = JacksonUtil.getJsonToBean(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }

        esService.remove(event.getEntityId());
    }
}
