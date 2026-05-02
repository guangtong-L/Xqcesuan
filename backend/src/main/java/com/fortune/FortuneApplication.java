package com.fortune;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 应用启动类
 * <p>
 * 强制将 JVM 默认时区设为 Asia/Shanghai，避免云托管容器跨天计算异常
 */
@SpringBootApplication
public class FortuneApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(FortuneApplication.class, args);
    }
}
