package com.futbol.majo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Value("${football.api.url:https://api.football-data.org/v4}")
  private String baseUrl;

  @Value("${football.api.key:}")
  private String apiKey;

  @Bean
  public RestClient footballRestClient() {
    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("X-Auth-Token", apiKey)
        .build();
  }
}