package com.sovava.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.sovava.common.exception.BizCodeEnum;
import com.sovava.member.exception.PhoneExistException;
import com.sovava.member.exception.UsernameExistException;
import com.sovava.member.vo.SocialUser;
import com.sovava.member.vo.UserLoginVo;
import com.sovava.member.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sovava.member.entity.MemberEntity;
import com.sovava.member.service.MemberService;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.R;


/**
 * 会员
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:39:50
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @PostMapping("/oauth2/login")
    public R socialLogin(@RequestBody SocialUser socialUser) {
        MemberEntity memberEntity = memberService.login(socialUser);
        if (memberEntity != null) {
            //登陆成功
            return R.ok().setData(memberEntity);
        } else {
            //登陆失败
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMessage());
        }
    }

    /**
     * 注册功能 ， 在验证码合法的情况下向数据库插入数据原， 需要检查是否非重复注册
     *
     * @param vo
     * @return
     */
    @PostMapping("/regist")
    public R register(@RequestBody UserRegistVo vo) {
        try {
            memberService.regist(vo);
        } catch (UsernameExistException uex) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMessage());
        } catch (PhoneExistException pex) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMessage());
        }

        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody UserLoginVo vo) {
        MemberEntity memberEntity = memberService.login(vo);
        if (memberEntity != null) {
            //登陆成功
            return R.ok().setData(memberEntity);
        } else {
            //登陆失败
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMessage());
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }
}
