package org.wgd.community.event;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.wgd.community.model.pojo.Event;
import org.wgd.community.util.JacksonUtil;

@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 涉及到三个主题生产
     * @param event
     */
    public void fireEvent(Event event) {
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JacksonUtil.getBeanToJson(event));
    }
}
