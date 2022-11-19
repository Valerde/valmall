package com.sovava.authserver.web;

import com.alibaba.fastjson2.TypeReference;
import com.sovava.authserver.feign.MemberFeignService;
import com.sovava.authserver.feign.ThirdPartyFeignService;
import com.sovava.authserver.vo.UserLoginVo;
import com.sovava.authserver.vo.UserRegistVo;
import com.sovava.common.constant.AuthServerConstant;
import com.sovava.common.exception.BizCodeEnum;
import com.sovava.common.utils.R;
import com.sovava.common.vo.MemberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class LoginController {


    @Resource
    private ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {

        //TODO //接口防刷

        String code = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(code)) {
            String s = code.split("_")[1];
            long l = Long.parseLong(s);
            if (System.currentTimeMillis() - l < 60 * 1000) {
                log.debug("一分钟内请求发送多次验证码");
                //60秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }

        //验证码的再次校验。redis存key-phone，value-code ,,sms:code:1823442**** -> 1234
        code = "1234";
        //redis缓存验证码 ， 防止同一个手机号在60秒内重复发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code + "_" + System.currentTimeMillis(), 60 * 3, TimeUnit.SECONDS);
        thirdPartyFeignService.sendCode(phone, code);
        return R.ok();
    }

    /**
     * TODO 重定向携带数据 ， 使用session存放数据，只要跳到下一个页面取出数据session里面的数据就会删掉
     * TODO 分布式下的session问题
     *
     * @param vo
     * @param result
     * @param attributes 模拟重定向携带数据
     * @return
     */
    @PostMapping("/register")
    public String register(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes attributes) {
        log.debug("register 参数为{}", vo.toString());
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //校验出错，转发到注册页
            attributes.addFlashAttribute("errors", errors);

            log.debug("校验出错，重定向到注册页");
            //重定向使用session的方式
            return "redirect:http://auth.valmall.com/reg";
        }

        //注册调用远程服务
        //1. 验证校验码：

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            if (vo.getCode().equals(redisCode.split("_")[0])) {
                log.debug("验证码校验成功");
                //删除验证码,令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证通过 ， 调用远程服务
                R registerR = memberFeignService.register(vo);
                if (registerR.getCode() == 0) {
                    //   注册成功回到登陆页
                    return "redirect:http://auth.valmall.com/loginPage";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    String errorMsg = registerR.getData("msg", new TypeReference<String>() {
                    });
                    log.debug("调用远程member服务信息出错，错误信息为{}", errorMsg);
                    errors.put("msg", errorMsg);
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.valmall.com/reg";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.valmall.com/reg";
            }
        } else {
            log.debug("没有验证码，重定向到注册页");
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.valmall.com/reg";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {
        log.debug(vo.toString());
        R loginR = memberFeignService.login(vo);
        if (loginR.getCode() == 0) {
            //登陆成功 TO DO 登陆成功后的处理
            MemberRespVo memberRespVo = loginR.getData(new TypeReference<MemberRespVo>() {
            });
            log.debug("账号登录的memberRespVo:{}",memberRespVo.toString());
            //将成功的用户放到session中
            session.setAttribute(AuthServerConstant.LOGIN_USER, memberRespVo);
            Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
            log.debug("账号登录的session为：{}",attribute.toString());
            return "redirect:http://valmall.com";
        } else {
            String msg = loginR.getData("msg", new TypeReference<String>() {
            });
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", msg);
            return "redirect:http://auth.valmall.com/loginPage";
        }


    }

    @GetMapping({"/login.html", "/loginPage"})
    public String loginPage(HttpSession session) {
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            //没登录
            return "login";
        } else {
            //登陆了
            return "redirect:http://valmall.com";
        }

    }

    @GetMapping({"indexreg.html"})
    public String indexreg() {
        return "indexreg";
    }

    @GetMapping({"loginindex.html"})
    public String loginindex() {
        return "loginindex";
    }

    @GetMapping({"reg.html"})
    public String reg() {
        return "reg";
    }
}
