//package com.sovava.thirdparty;
//
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//public class SMS {
//
//    @Test
//    public void send(){
//        DefaultProfile profile = DefaultProfile.getProfile("cn-beijing", "<your-access-key-id>", "<your-access-key-secret>");
//        /** use STS Token
//         DefaultProfile profile = DefaultProfile.getProfile(
//         "<your-region-id>",           // The region ID
//         "<your-access-key-id>",       // The AccessKey ID of the RAM account
//         "<your-access-key-secret>",   // The AccessKey Secret of the RAM account
//         "<your-sts-token>");          // STS Token
//         **/
//        IAcsClient client = new DefaultAcsClient(profile);
//
//        SendSmsRequest request = new SendSmsRequest();
//        request.setPhoneNumbers("1368846****");//接收短信的手机号码
//        request.setSignName("阿里云");//短信签名名称
//        request.setTemplateCode("SMS_20933****");//短信模板CODE
//        request.setTemplateParam("张三");//短信模板变量对应的实际值
//
//        try {
//            SendSmsResponse response = client.getAcsResponse(request);
//            System.out.println(new Gson().toJson(response));
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            System.out.println("ErrCode:" + e.getErrCode());
//            System.out.println("ErrMsg:" + e.getErrMsg());
//            System.out.println("RequestId:" + e.getRequestId());
//        }
//    }
//}
