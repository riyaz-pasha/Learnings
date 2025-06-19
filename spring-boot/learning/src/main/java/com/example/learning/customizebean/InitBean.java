package com.example.learning.customizebean;

import com.example.learning.common.ConsoleColor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitBean implements InitializingBean {

    public InitBean() {
        ConsoleColor.info("InitBean created");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConsoleColor.success("InitBean afterPropertiesSet");
    }

}
