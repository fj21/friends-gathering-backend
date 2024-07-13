package com.jiang.friendsGatheringBackend.Manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiang.friendsGatheringBackend.model.session.SessionData;
import redis.clients.jedis.Jedis;

public class RedisSessionManager {
    private Jedis jedis;
    private ObjectMapper objectMapper;
    public RedisSessionManager() {
        // Initialize Jedis connection (make sure Redis server is running and accessible)
        jedis = new Jedis("localhost");
        objectMapper = new ObjectMapper();
    }

    public void saveSession(String sessionId, SessionData sessionData) throws JsonProcessingException {
        // Construct the session key
        String sessionKey = "Session:" + sessionId;
        String sessionValue = objectMapper.writeValueAsString(sessionData);
        // Save session data in Redis
        jedis.set(sessionKey, sessionValue);

        // Optionally, set expiration time (e.g., 30 minutes)
        jedis.expire(sessionKey, 1800); // 1800 seconds = 30 minutes
    }

    public SessionData getSession(String sessioinId) throws JsonProcessingException {
        // Construct the session key
        String sessionKey = "Session:" + sessioinId;

        // Retrieve session data from Redis
        String sessionValue = jedis.get(sessionKey);
        if (sessionValue == null) {
            // Session data not found (expired or never existed)
            return null; // or throw an exception or handle as appropriate
        }
        return objectMapper.readValue(sessionValue,SessionData.class);
    }

    public void close() {
        jedis.close();
    }

    public void updateSessionExpiration(String sessionId) {
        String sessionKey = "Session:" + sessionId;
        // 设置 sessionId 对应键的过期时间为一定的秒数
        jedis.expire(sessionKey, 1800);
    }
    public void logout(String sessionId) {
        // 构建 session 在 Redis 中的 key
        String sessionKey = "Session:" + sessionId;

        // 删除 Redis 中对应的会话数据
        jedis.del(sessionKey);

        // 可以选择关闭 Redis 连接
        jedis.close();
    }

//    public static void main(String[] args) {
//        RedisSessionManager manager = new RedisSessionManager();
//
//        // Example usage:
//        String userId = "123"; // Replace with actual user ID
//        String sessionData = "example_session_data"; // Replace with actual session data
//
//        manager.saveSession(userId, sessionData);
//
//        String retrievedSession = manager.getSession(userId);
//        System.out.println("Retrieved session data: " + retrievedSession);
//
//        manager.close();
//    }
}