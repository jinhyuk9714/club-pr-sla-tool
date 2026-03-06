package com.club.sla.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class RestClientConfiguration {

  @Bean
  RestClientCustomizer restClientCustomizer() {
    return builder -> builder.requestFactory(new JdkClientHttpRequestFactory());
  }
}
