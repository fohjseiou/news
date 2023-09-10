package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerQuickStart {
    public static void main(String[] args) {
        //1.kafka链接配置信息
        Properties prop = new Properties();
        //kafka链接地址
        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        //key和value的序列化
        prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

        //ack配置  消息确认机制
        prop.put(ProducerConfig.ACKS_CONFIG,"all");
        //消息发送失败 重试次数
        prop.put(ProducerConfig.RETRIES_CONFIG,10);
        //数据压缩方式
        prop.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");

        //2.创建kafka生产者对象
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(prop);

        //3.发送消息
        /**
         * 第一个消息：topic
         * 第二个消息：消息的key
         * 第三个消息：消息的value
         */
        ProducerRecord<String,String> kvProducerRecord = new ProducerRecord<String,String>("topic-first","key-001","hello kafka");
        producer.send(kvProducerRecord);

        //4.关闭消息通道 ,必须要关闭
        producer.close();
    }
}
