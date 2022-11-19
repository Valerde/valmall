package com.sovava.authserver.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.sovava.authserver.feign.MemberFeignService;
import com.sovava.common.constant.AuthServerConstant;
import com.sovava.common.vo.MemberRespVo;
import com.sovava.authserver.vo.SocialUser;
import com.sovava.common.utils.HttpUtil;
import com.sovava.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * 处理社交登陆请求
 */
@Controller
@Slf4j
public class Oauth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam String code, HttpSession session) {
        //根据code换取access_token
        HttpUtil.ReqData reqData = HttpUtil.createReq();
        //请求方式
        reqData.setMethod("POST");
        //数据请求类型
        reqData.setContentType("application/json");
        //参数
        reqData.addReqParameter("client_id", "3979802358");
        reqData.addReqParameter("client_secret", "e7a1e09c4246bcd8d673319fce4f4802");
        reqData.addReqParameter("grant_type", "authorization_code");
        reqData.addReqParameter("redirect_uri", "http://auth.valmall.com/oauth2.0/weibo/success");
        reqData.addReqParameter("code", code);
        //请求地址
        reqData.setUrl("https://api.weibo.com/oauth2/access_token");
        //调用接口
        HttpUtil.RespData respData = HttpUtil.reqConnection(reqData);
        //获得返回的json字符串
        String result = JSON.toJSONString(respData);
        log.debug("获得的json:{}", result);

        if (respData.getCode() == 200) {
            //获取成功
            String bodyString = JSON.toJSONString(respData.getData());
            SocialUser socialUser = JSON.parseObject(bodyString, SocialUser.class);
            log.debug("用户信息为:{}", socialUser.toString());
            log.debug("获取的access_token为：{}", socialUser.getAccess_token());
            //知道是哪个社交用户
            // 当前用户如果是第一次进入这个网站 。 那么就注册进来（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应哪个会员）
            //登陆或注册
            R r = memberFeignService.socialLogin(socialUser);
            if (r.getCode() == 0) {
                MemberRespVo memberRespVo = r.getData(new TypeReference<MemberRespVo>() {
                });
                log.debug("登陆成功");
                log.debug("成员信息为:{}", memberRespVo.getAccessToken());
                log.debug("用户昵称为：{}", memberRespVo.getNickname());

                //TO DO： 作用域：默认发放的令牌 作用于为当前域（解决子域共享问题）
                //TO DO： 使用json的序列化方式来向redis保存数据
                session.setAttribute(AuthServerConstant.LOGIN_USER, memberRespVo);
                return "redirect:http://valmall.com/";
            } else {
                log.debug("远程调用memberFeignService失败失败原因：{}", r.get("msg"));
                return "redirect:http://auth.valmall.com/loginPage";
            }
        } else {
            log.debug("获取微博token失败");
            return "redirect:http://auth.valmall.com/loginPage";
            //重定向不可以使用@Responsebody （@RestController内置了@RB）
        }
    }
}
