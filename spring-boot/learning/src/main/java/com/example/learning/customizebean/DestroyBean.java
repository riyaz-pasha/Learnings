package com.example.learning.customizebean;

import com.example.learning.common.ConsoleColor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DestroyBean implements DisposableBean {

    public DestroyBean() {
        ConsoleColor.info("DestroyBean created");
    }

    @Override
    public void destroy() throws Exception {
        ConsoleColor.warn("DestroyBean destroyed");
    }

}
