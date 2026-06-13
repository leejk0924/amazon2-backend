package com.jk.amazon2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Amazon2Application {

    public static void main(String[] args) {
        SpringApplication.run(Amazon2Application.class, args);
    }

}
