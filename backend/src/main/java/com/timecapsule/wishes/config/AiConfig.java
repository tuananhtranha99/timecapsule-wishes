package com.timecapsule.wishes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Configuration
public class AiConfig {

    @Value("${app.ai.gemini.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${app.ai.gemini.read-timeout-seconds:20}")
    private int readTimeoutSeconds;

    @Bean
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("application/octet-stream"),
                MediaType.TEXT_PLAIN,
                MediaType.ALL
        ));

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(List.of(
                MediaType.TEXT_PLAIN,
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("application/octet-stream"),
                MediaType.ALL
        ));

        return RestClient.builder()
                .requestFactory(factory)
                .messageConverters(converters -> {
                    converters.add(0, stringConverter);
                    converters.add(1, jacksonConverter);
                })
                .build();
    }
}
