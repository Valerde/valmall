package com.sovava.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.member.dao.MemberLevelDao;
import com.sovava.member.entity.MemberLevelEntity;
import com.sovava.member.exception.PhoneExistException;
import com.sovava.member.exception.UsernameExistException;
import com.sovava.member.vo.UserLoginVo;
import com.sovava.member.vo.UserRegistVo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

}