package com.example.learning.customizebean;

import com.example.learning.common.ConsoleColor;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DestroyBeanNew {

    public DestroyBeanNew() {
        ConsoleColor.info("DestroyBeanNew created");
    }

    @PreDestroy
    public void preDestroy() {
        ConsoleColor.warn("DestroyBeanNew destroyed");
    }

}
