package com.sovava.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
class GulimallThirdpartyApplicationTests {

    @Test
    void contextLoads() {
    }


    @Resource
    private OSSClient ossClient;

    /**
     * 阿里云OSS 对象存储
     * 1. 引入oss-starter
     * 2. yml配置key endpoint 等相关信息
     * 3. 使用OSSClient
     *
     * @throws Exception
     */
    @Test
    public void testOSS() throws Exception {
//        AliyunUploadFiles aliyunUploadFiles = new AliyunUploadFiles();
//        aliyunUploadFiles.upload();


        // 填写Bucket名称，例如examplebucket。
        String bucketName = "gulimall-valverde";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "igyjqq4.jpg";
        String filePath = "/root/IdeaProjects/valmall/igyjqq.jpg";
        InputStream inputStream = Files.newInputStream(Paths.get(filePath));
        ossClient.putObject(bucketName, objectName, inputStream);
        System.out.println("上传完成");
    }

}
