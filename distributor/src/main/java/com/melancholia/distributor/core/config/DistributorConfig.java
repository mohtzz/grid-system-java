package com.melancholia.distributor.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class DistributorConfig {

    @Bean
    public String callbackAddress(@Value("${server.port}") int port) throws UnknownHostException {
        return String.format("http://%s:%d/result", InetAddress.getLocalHost().getHostAddress(), port);
    }


}
