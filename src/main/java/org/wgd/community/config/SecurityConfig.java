package org.wgd.community.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.ResponseStatusEnum;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.service.UserService;
import org.wgd.community.util.JacksonUtil;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends BaseInfoProperties {

    @Autowired
    private UserService userService;

    /**
     * 让security忽略resources下的文件，不需要管理
     *
     * @return
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers("/resources/**");
    }



    /**
     * 授权：默认对任何请求做管理
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 给角色授权接口
        http.authorizeRequests()
                .antMatchers(
                        // 哪些接口需要权限才能访问
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        // 那些角色拥有访问上面接口的权限
                        AUTHORIZATION_USER,
                        AUTHORIZATION_ADMIN,
                        AUTHORIZATION_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORIZATION_MODERATOR,
                        AUTHORIZATION_ADMIN
                )
                .antMatchers(
//                        "/discuss/delete",
                        "/data/**"
                )
                .hasAnyAuthority(
                        AUTHORIZATION_ADMIN
                )
                // 除此以外的接口，不做约束
                .anyRequest().permitAll()
                .and().csrf().disable();

        // 权限不够的处理
        http.exceptionHandling()
                // 没有登录的处理
                .authenticationEntryPoint((request, response, authException) -> {
                    // 先确定请求是异步还是同步
                    String xRequestedWith = request.getHeader("x-requested-with");
                    // 异步
                    if ("XMLHttpRequest".equals(xRequestedWith)) {
                        // 设置response的contentType，指定为普通字符串
                        response.setContentType("application/plain;charset=utf-8");
                        // 通过response的输出流，将json返回给前端
                        PrintWriter writer = response.getWriter();
                        writer.write(GraceJSONResult.errorCustomJson(ResponseStatusEnum.UN_LOGIN));
                    } else {
                        // 同步
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
                // 权限不足的处理
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // 和上面差不多一个逻辑，简单修改即可
                    // 先确定请求是异步还是同步
                    String xRequestedWith = request.getHeader("x-requested-with");
                    // 异步
                    if ("XMLHttpRequest".equals(xRequestedWith)) {
                        // 设置response的contentType，指定为普通字符串
                        response.setContentType("application/plain;charset=utf-8");
                        // 通过response的输出流，将json返回给前端
                        PrintWriter writer = response.getWriter();
                        writer.write(JacksonUtil.getBeanToJson(ResponseStatusEnum.NO_AUTH));
                    } else {
                        // 同步
                        response.sendRedirect(request.getContextPath() + "/denied");
                    }
                });

        // security默认拦截/logout接口，进行退出处理，所以要修改一下他拦截的接口
        // 避免影响咱们的/logout接口
        http.logout().logoutUrl("aaaaaa");

        return http.build();
    }
}