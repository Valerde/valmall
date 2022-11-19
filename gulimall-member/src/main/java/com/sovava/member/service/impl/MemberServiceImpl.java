package com.sovava.member.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.common.utils.HttpUtil;
import com.sovava.member.dao.MemberLevelDao;
import com.sovava.member.entity.MemberLevelEntity;
import com.sovava.member.exception.PhoneExistException;
import com.sovava.member.exception.UsernameExistException;
import com.sovava.member.vo.SocialUser;
import com.sovava.member.vo.UserLoginVo;
import com.sovava.member.vo.UserRegistVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.member.dao.MemberDao;
import com.sovava.member.entity.MemberEntity;
import com.sovava.member.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(UserRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setLevelId(1L);
        //查询默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        //检查手机号是否唯一
        //利用异常机制让controller感知
        checkPhoneUnique(vo.getPhone());
        memberEntity.setMobile(vo.getPhone());
        //检查用户名是否唯一
        checkUsernameUnique(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());
        //密码进行加密存储
        //spring自带的密码加密器
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodePassword = encoder.encode(vo.getPassword());
        memberEntity.setPassword(encodePassword);
//        其他的默认信息
        this.baseMapper.insert(memberEntity);
    }

    public void checkUsernameUnique(String userName) throws UsernameExistException {
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(MemberEntity::getUsername, userName);

        MemberEntity one = this.getOne(lqw);
        if (one != null) {
            throw new UsernameExistException();
        }

    }

    public void checkPhoneUnique(String phone) throws PhoneExistException {
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(MemberEntity::getMobile, phone);

        MemberEntity one = this.getOne(lqw);
        if (one != null) {
            throw new PhoneExistException();
        }
    }

    @Override
    public MemberEntity login(UserLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        LambdaQueryWrapper<MemberEntity> lqw = new LambdaQueryWrapper<>();

        lqw.eq(MemberEntity::getMobile, loginacct).or().eq(MemberEntity::getUsername, loginacct);
        MemberEntity one = this.getOne(lqw);
        if (null != one) {
            //有这个人
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean matches = encoder.matches(password, one.getPassword());
            if (matches) {
                //匹配成功
                return one;
            } else {
                return null;
            }
        } else {
            //没这个账户
            return null;
        }

    }

    @Override
    public MemberEntity login(SocialUser socialUser) {
        //具有登陆和注册合并逻辑
        String uid = socialUser.getUid();
        //判断当前用户是否已经登陆过系统
        MemberEntity memberEntity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, uid));
        if (memberEntity != null) {
            //已经注册过
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setExpiresIn(socialUser.getExpires_in());
            update.setAccessToken(socialUser.getAccess_token());
            this.baseMapper.updateById(update);
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        } else {
            //没有查到社交用户的账号 ， 注册一个
            MemberEntity register = querySocialUserInfo(socialUser);
            //查询当前社交用户的昵称用户信息等
            register.setSocialUid(socialUser.getUid());
            register.setAccessToken(socialUser.getAccess_token());
            register.setExpiresIn(socialUser.getExpires_in());
            int insert = this.baseMapper.insert(register);
            return insert > 0 ? register : new MemberEntity();
        }

    }

    public MemberEntity querySocialUserInfo(SocialUser socialUser) {
        MemberEntity memberEntity = new MemberEntity();
        try {
            HttpUtil.ReqData req = HttpUtil.createReq();
            req.addReqParameter("access_token", socialUser.getAccess_token());
            req.addReqParameter("uid", socialUser.getUid());
            req.setMethod("GET");
            req.setContentType("application/json");
            req.setUrl("https://api.weibo.com/2/users/show.json");
            HttpUtil.RespData respData = HttpUtil.reqConnection(req);

            if (respData.getCode() == 200) {
                log.debug("查询到的信息：{}", JSON.toJSONString(respData.getData()));
                String json = JSON.toJSONString(respData.getData());
                JSONObject jsonObject = JSONObject.parseObject(json);
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                memberEntity.setNickname(name);
                memberEntity.setGender("m".equals(gender) ? 1 : 0);
            } else {
                log.debug("查询微博用户信息失败");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return memberEntity;
    }

}