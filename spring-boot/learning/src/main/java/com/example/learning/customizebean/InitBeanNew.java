package com.example.learning.customizebean;

import com.example.learning.common.ConsoleColor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitBeanNew {

    public InitBeanNew() {
        ConsoleColor.info("InitBeanNew created");
    }

    @PostConstruct
    public void postConstruct() {
        ConsoleColor.success("InitBeanNew postConstruct");
    }

}
