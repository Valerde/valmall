package com.sovava.cart.interceptor;

import com.sovava.cart.vo.UserInfoTo;
import com.sovava.common.constant.AuthServerConstant;
import com.sovava.common.constant.CartConstant;
import com.sovava.common.vo.MemberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法之前 ， 判断用户有没有登录并封装传递给controller
 */
@Slf4j
@Component
public class CartInterceptor implements HandlerInterceptor {
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<UserInfoTo>();

    /**
     * 业务执行之前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();
        MemberRespVo memberRespVo = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (memberRespVo != null) {
            log.debug("用户登陆了");
            userInfoTo.setUserId(memberRespVo.getId());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length != 0) {
            log.debug("cookie不为空");
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if (name.equals(CartConstant.TEMP_USER_COOKIE_KEY)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }

        //设置临时用户：如果没有临时用户一定要分配一个临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String userKey = UUID.randomUUID().toString();
            userInfoTo.setUserKey(userKey);
        }

        threadLocal.set(userInfoTo);
        return true;
    }


    /**
     * 业务执行之后 , 分配临时用户 ， 让浏览器保存
     *
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();

        //如果没有临时用户 ， 一定保存临时用户
        if (!userInfoTo.isTempUser()) {
            log.debug("是临时用户，给cookie");

            //持续延长用户的过期时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_KEY, userInfoTo.getUserKey());
            cookie.setDomain("valmall.com");
            cookie.setMaxAge(CartConstant.COOKIE_MAX_AGE);
            response.addCookie(cookie);
        }

    }
}
