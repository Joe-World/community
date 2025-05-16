package org.wgd.community.model.pojo;

import java.util.HashMap;
import java.util.Map;

public class Event {
    /**
     * 事件的类型/消息的类型
     */
    private String topic;
    /**
     * 发消息的人
     */
    private int userId;
    /**
     * 接受消息的类型
     */
    private int entityType;
    /**
     * 接受实体的id
     */
    private int entityId;
    /**
     * 最终接受消息的用户id
     */
    private int entityUserId;
    /**
     * 储存额外的其他的数据
     */
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    /**
     * 根据返回的引用，再次修改其他属性，方便操作
     * @param topic
     * @return
     */
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    /**
     * map的setter是直接插入键值对
     * @param key
     * @param value
     * @return
     */
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
