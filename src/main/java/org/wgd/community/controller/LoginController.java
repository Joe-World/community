package org.wgd.community.controller;


import com.google.code.kaptcha.Producer;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.wgd.community.common.enums.ExpiredSecondsEnum;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.UserService;
import org.wgd.community.util.JWTUtils;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;
import org.wgd.community.util.UUIDUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisUtil redisUtils;

    /**
     * 进入登录页面
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 生成验证码
     * @param response
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 验证码的归属
        String kaptchaOwner = UUIDUtils.simpleUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入Redis
        String redisKey = RedisKeyUtils.getCodeKey(kaptchaOwner);
        // 存储验证码60秒
        redisUtils.set(redisKey, text, 60);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            log.error("响应验证码失败:" + e.getMessage());
        }
    }


    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, /*HttpSession session, */HttpServletResponse response,
                        @CookieValue(value = "kaptchaOwner",defaultValue = "") String kaptchaOwner) {
        // 万能验证码用于自动化测试
        if (!"0000".equals(code)) {
            // 验证redis中的验证码
            String kaptcha = null;
            if (StringUtils.isNotBlank(kaptchaOwner)) {
                String redisKey = RedisKeyUtils.getCodeKey(kaptchaOwner);
                kaptcha = (String) redisUtils.get(redisKey);
            }
            if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
                model.addAttribute("codeMsg", "验证码不正确!");
                return "/site/login";
            }

        }

        // 检查账号,密码
        int expiredSeconds = rememberme ? ExpiredSecondsEnum.REMEMBER.getSeconds() :ExpiredSecondsEnum.DEFAULT.getSeconds();
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        // 若有ticket说明登录成功
        if (map.containsKey("ticket")) {
            // 将ticket放到cookie作为用户凭证
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);

        // 清空权限凭证
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

}
