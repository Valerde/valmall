package com.sovava.deckill.interceptor;

import com.sovava.common.constant.AuthServerConstant;
import com.sovava.common.vo.MemberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        boolean match = new AntPathMatcher().match("/kill", request.getRequestURI());
        if (match) {
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
            if (attribute != null) {
                //记录此时的登录用户
                threadLocal.set(attribute);
                return true;
            } else {
                request.getSession().setAttribute("msg", "请先进行登录");
                response.sendRedirect("http://auth.valmall.com/loginPage");
                log.debug("未登录去结算，重定向到登录页");
                return false;
            }
        }
        return true;
    }
}
