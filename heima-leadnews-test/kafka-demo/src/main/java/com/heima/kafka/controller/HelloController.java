package com.heima.kafka.controller;

import com.alibaba.fastjson.JSON;
import com.heima.kafka.pojo.User;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class HelloController {
    @Resource
    private KafkaTemplate<String,String> kafkaTemplate;


    @GetMapping("/hello")
     public String hello(){
      //kafkaTemplate.send("itcast-topic","黑马程序员");
        User user = new User();
        user.setUsername("小王");
        user.setAge(18);
        kafkaTemplate.send("user-topic", JSON.toJSONString(user));
      return "ok";
    }

}
