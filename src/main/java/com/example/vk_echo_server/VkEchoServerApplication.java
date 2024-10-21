package com.example.vk_echo_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class VkEchoServerApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(VkEchoServerApplication.class, args);
        LongPollingService service = context.getBean(LongPollingService.class);
        service.startPolling();
    }

}
