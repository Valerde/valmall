package com.sovava.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.member.entity.MemberEntity;
import com.sovava.member.exception.PhoneExistException;
import com.sovava.member.exception.UsernameExistException;
import com.sovava.member.vo.UserLoginVo;
import com.sovava.member.vo.UserRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:39:50
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(UserRegistVo vo);

    void checkUsernameUnique(String userName) throws UsernameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(UserLoginVo vo);
}

