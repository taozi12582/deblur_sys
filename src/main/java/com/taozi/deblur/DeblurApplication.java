package com.taozi.deblur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAutoConfiguration
@SpringBootApplication
public class DeblurApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeblurApplication.class, args);
    }
}
