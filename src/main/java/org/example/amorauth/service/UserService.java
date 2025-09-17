package org.example.amorauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.entity.User;
import org.example.amorauth.mapper.UserMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public User processOAuth2User(OAuth2User oauth2User) {
        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        String locale = oauth2User.getAttribute("locale");

        log.info("Processing OAuth2 user: googleId={}, email={}", googleId, email);

        // 首先尝试通过Google ID查找用户
        User existingUser = userMapper.findByGoogleId(googleId);

        if (existingUser != null) {
            // 更新用户信息
            existingUser.setName(name);
            existingUser.setPicture(picture);
            existingUser.setLocale(locale);
            existingUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateUser(existingUser);

            // 缓存用户信息到Redis
            cacheUser(existingUser);

            log.info("Updated existing user: {}", existingUser.getId());
            return existingUser;
        } else {
            // 创建新用户
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPicture(picture);
            newUser.setLocale(locale);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            userMapper.insertUser(newUser);

            // 缓存用户信息到Redis
            cacheUser(newUser);

            log.info("Created new user: {}", newUser.getId());
            return newUser;
        }
    }

    public User createOrUpdateUser(String googleId, String email, String name, String picture, String locale) {
        log.info("Creating or updating user: googleId={}, email={}", googleId, email);

        User existingUser = userMapper.findByGoogleId(googleId);

        if (existingUser != null) {
            // 更新现有用户
            existingUser.setName(name);
            existingUser.setPicture(picture);
            existingUser.setLocale(locale);
            existingUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateUser(existingUser);

            // 更新缓存
            cacheUser(existingUser);

            return existingUser;
        } else {
            // 创建新用户
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPicture(picture);
            newUser.setLocale(locale);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            userMapper.insertUser(newUser);

            // 缓存新用户
            cacheUser(newUser);

            return newUser;
        }
    }

    public User findByGoogleId(String googleId) {
        // 先从Redis缓存中查找
        String cacheKey = "user:google:" + googleId;
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            log.debug("Found user in cache: {}", googleId);
            return cachedUser;
        }

        // 缓存中没有，从数据库查找
        User user = userMapper.findByGoogleId(googleId);
        if (user != null) {
            cacheUser(user);
        }

        return user;
    }

    public User findByEmail(String email) {
        // 先从Redis缓存中查找
        String cacheKey = "user:email:" + email;
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            log.debug("Found user in cache by email: {}", email);
            return cachedUser;
        }

        // 缓存中没有，从数据库查找
        User user = userMapper.findByEmail(email);
        if (user != null) {
            cacheUser(user);
        }

        return user;
    }

    public User findById(Long id) {
        // 先从Redis缓存中查找
        String cacheKey = "user:id:" + id;
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            log.debug("Found user in cache by id: {}", id);
            return cachedUser;
        }

        // 缓存中没有，从数据库查找
        User user = userMapper.findById(id);
        if (user != null) {
            cacheUser(user);
        }

        return user;
    }

    private void cacheUser(User user) {
        if (user == null) return;

        // 设置缓存时间为1小时
        Duration cacheDuration = Duration.ofHours(1);

        // 按不同的key缓存用户信息
        redisTemplate.opsForValue().set("user:id:" + user.getId(), user, cacheDuration);
        redisTemplate.opsForValue().set("user:google:" + user.getGoogleId(), user, cacheDuration);
        redisTemplate.opsForValue().set("user:email:" + user.getEmail(), user, cacheDuration);

        log.debug("Cached user: {}", user.getId());
    }

    public void clearUserCache(User user) {
        if (user == null) return;

        redisTemplate.delete("user:id:" + user.getId());
        redisTemplate.delete("user:google:" + user.getGoogleId());
        redisTemplate.delete("user:email:" + user.getEmail());

        log.debug("Cleared cache for user: {}", user.getId());
    }
}
