package com.jk.amazon2.posting.config;

import com.jk.amazon2.posting.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfiguration {

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(2000); // 2초에 1개 요청
    }

    @Bean
    public NaverBlogScraper naverBlogScraper() {
        return new NaverBlogScraper();
    }
}
