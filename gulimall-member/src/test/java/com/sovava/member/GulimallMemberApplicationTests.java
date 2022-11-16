package com.sovava.member;

import com.sovava.member.dao.MemberLevelDao;
import com.sovava.member.entity.MemberLevelEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    MemberLevelDao memberLevelDao;

    @Test
    void testGetDefaultLevel() {
        MemberLevelEntity defaultLevel = memberLevelDao.getDefaultLevel();
        log.debug(defaultLevel.toString());
    }



}
