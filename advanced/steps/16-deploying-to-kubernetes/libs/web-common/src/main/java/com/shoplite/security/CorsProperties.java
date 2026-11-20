package com.shoplite.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Browser origins (scheme + host + port) allowed to call the API from JavaScript (shoplite.cors.*). */
@ConfigurationProperties("shoplite.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
