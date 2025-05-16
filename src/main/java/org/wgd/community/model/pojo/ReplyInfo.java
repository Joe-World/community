package org.wgd.community.model.pojo;


public class ReplyInfo extends Comment{
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + getId() +
                ", userId=" + getUserId() +
                ", entityType=" + getEntityType() +
                ", entityId=" + getEntityId() +
                ", targetId=" + getTargetId() +
                ", content='" + getContent() + '\'' +
                ", status=" + getStatus() +
                ", createTime=" + getCreateTime() +
                ", title="+ getTitle() +
                '}';
    }
}
