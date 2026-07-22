package com.bubbletea.chat;

import com.bubbletea.chat.infrastructure.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
@EnableJpaAuditing
public class ChatServiceApplication {

  static void main(String[] args) {
    SpringApplication.run(ChatServiceApplication.class, args);
  }

}
