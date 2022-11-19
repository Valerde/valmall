package com.sovava.member;

import com.sovava.member.service.MemberService;
import com.sovava.member.service.impl.MemberServiceImpl;
import com.sovava.member.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@SpringBootTest
public class OtherTest {
    @Test
    public void testMD5() {
        String s = DigestUtils.md5Hex("veuiyigeyjviveyzuiyigeyjviykn12345");
        s = Md5Crypt.md5Crypt("123456".getBytes(), "$1$1234");
        log.debug(s);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        //$2a$10$tJ8WSi60sRS0AqBebE.S0.KZrlHlaKUi8JQfeIqqCrGQNcmUevjpW
//        $2a$10$d4lh.CMeEh08yEkqsH6.g.JknD.nyDi2YTDvsiswOxBUFSE.3pHde
        String encode = encoder.encode("123456");
        //两次不一样
        log.debug(encode);
        boolean matches = encoder.matches("123456", "$2a$10$d4lh.CMeEh08yEkqsH6.g.JknD.nyDi2YTDvsiswOxBUFSE.3pHde");
        log.debug("{}",matches);
    }

    @Autowired
    private MemberServiceImpl memberService;
    @Test
    public void testGetWeiBoSocialInfo(){
        SocialUser socialUser = new SocialUser();
        socialUser.setUid("7802249662");
        socialUser.setAccess_token("2.00UY8BWIc5q12E4dbdd16fb9ImFORE");
        memberService.querySocialUserInfo(socialUser);
    }
}
