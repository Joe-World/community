package org.wgd.community.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.common.enums.ActivationEnum;
import org.wgd.community.common.enums.YesOrNo;
import org.wgd.community.model.pojo.LoginTicket;
import org.wgd.community.model.pojo.User;
import org.wgd.community.mapper.LoginTicketMapper;
import org.wgd.community.mapper.UserMapper;
import org.wgd.community.service.UserService;
import org.wgd.community.util.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends BaseInfoProperties implements UserService {
    @Value("${community.path}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClientUtil mailClientUtil;

    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public User getUser(Integer userId) {
        User user = getCache(userId);
        if (user == null) {
            user = initCache(userId);
        }
        return user;
    }

    @Override
    public User getUserByName(String name) {
        return userMapper.selectByName(name);
    }

    @Override
    public Map<String, Object> register(User user) {
        // 存放提示信息，没有说明，注册成功
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号：是否重复
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱：是否重复
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        // 盐值:随机五位字符串
        user.setSalt(UUIDUtils.simpleUUID().substring(0, 5));
        // 密码：md5加密-【用户设置的密码+盐值】
        user.setPassword(MD5Utils.MD5Lower(user.getPassword() + user.getSalt()));
        // 普通用户
        user.setType(0);
        // 未激活
        user.setStatus(1);
        user.setActivationCode(UUIDUtils.simpleUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insert(user);

        // 激活邮件，组装模板
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // 用户点击连接（激活接口）：http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        // 发送邮件
        mailClientUtil.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }


    @Override
    public int activation(int userId, String code) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user.getStatus() == 1) {
            return ActivationEnum.REPEAT.getType();
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatusById(userId, 1);
            clearCache(userId);
            return ActivationEnum.SUCCESS.getType();
        } else {
            return ActivationEnum.FAILURE.getType();
        }
    }

    @Override
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = MD5Utils.MD5Lower(password, user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(UUIDUtils.simpleUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));

        String redisKey = RedisKeyUtils.getTicketKey(loginTicket.getTicket());
        redisUtil.set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    @Override
    public void logout(String ticket) {
        String redisKey = RedisKeyUtils.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisUtil.get(redisKey);
        loginTicket.setStatus(1);
        redisUtil.set(redisKey, loginTicket);
    }

    //修改密码
    @Override
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword, String confirmPassword){
        Map<String, Object> map = new HashMap<>();
        if(StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword) || StringUtils.isBlank(confirmPassword)){
            map.put("errorPsd", "密码不能为空!");
            return map;
        }
        if(!newPassword.equals(confirmPassword)){
            map.put("errorPsd","两次密码输入不一致！");
            return map;
        }

        User user = userMapper.selectByPrimaryKey(userId);
        oldPassword = MD5Utils.MD5Lower(oldPassword + user.getSalt());
        if(!user.getPassword().equals(oldPassword)){
            map.put("errorPsd","原密码输入错误！");
            return map;
        }

        // 修改盐
        user.setSalt(UUIDUtils.simpleUUID().substring(0,5));
        confirmPassword = MD5Utils.MD5Lower(confirmPassword + user.getSalt());
        userMapper.updatePassword(userId, confirmPassword, user.getSalt());

        map.put("successPsd","修改成功");

        return map;

    }

    @Override
    public LoginTicket findLoginTicket(String ticket) {
        //        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtils.getTicketKey(ticket);
        return (LoginTicket) redisUtil.get(redisKey);
    }

    @Override
    public int updateHeaderById(int userId, String headerUrl) {
        int result = userMapper.updateHeaderById(userId, headerUrl);
        clearCache(userId);
        return result;
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtils.getUserInfoKey(userId);
        return (User) redisUtil.get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        String redisKey = RedisKeyUtils.getUserInfoKey(userId);
        // 缓存存在一个小时
        redisUtil.set(redisKey, user, 3600);
        return user;
    }

    // 3.数据变更时清除缓存数据
    /**
     * 为这个user添加权限组
     * @param userId
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        // 获取user
        User user = userMapper.selectByPrimaryKey(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(() -> {
            switch (user.getType()) {
                case 0:
                    return AUTHORIZATION_USER;
                case 1:
                    return AUTHORIZATION_ADMIN;
                default:
                    return AUTHORIZATION_MODERATOR;
            }
        });

        return list;
    }

    private void clearCache(int userId) {
        String redisKey = RedisKeyUtils.getUserInfoKey(userId);
        redisUtil.del(redisKey);
    }
}
