package com.sovava.member.dao;

import com.sovava.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:39:50
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
