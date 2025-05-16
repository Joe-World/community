package org.wgd.community.service;


import org.springframework.security.core.GrantedAuthority;
import org.wgd.community.model.pojo.LoginTicket;
import org.wgd.community.model.pojo.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface UserService {
    User getUser(Integer userId);

    User getUserByName(String name);

    Map<String, Object> register(User user);

    int activation(int userId, String code);

    public Map<String, Object> login(String username, String password, int expiredSeconds);

    public void logout(String ticket);

    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword, String confirmPassword);

    public LoginTicket findLoginTicket(String ticket);

    public int updateHeaderById(int userId, String headerUrl);

    public Collection<? extends GrantedAuthority> getAuthorities(int userId);
}
