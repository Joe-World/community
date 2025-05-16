package org.wgd.community.service;


import java.util.List;
import java.util.Map;

public interface RedisService {
    public void like(int userId, int entityType, int entityId, int userLikedId);

    public long countEntityLike(int entityType, int entityId);

    public int getEntityLikedStatus(int userId, int entityType, int entityId);

    public long countUserLiked(int userId);

    public void follow(int userId, int entityType, int entityId);

    public long countUserFollow(int userId, int entityType);

    public long countUserFan(int entityType, int entityId);

    public boolean isFollowed(int userId, int entityType, int entityId);

    public List<Map<String, Object>> getFollows(int userId, int begin, int limit);

    public List<Map<String, Object>> getFans(int userId, int begin, int limit);
}
