package com.shoplite.reviews;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A WebClient.Builder whose requests go through Spring Cloud LoadBalancer: the host in the URL
 * (http://catalog-service) is a service id, resolved to an instance on every request.
 */
@Configuration(proxyBeanMethods = false)
class WebClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

}
