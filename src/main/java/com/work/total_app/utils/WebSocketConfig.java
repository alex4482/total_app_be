package com.work.total_app.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//@Configuration
//@EnableWebSocketMessageBroker
//@Log4j2
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /*
     * @Override
     * public void configureMessageBroker(MessageBrokerRegistry config) {
     * config.enableSimpleBroker("/topic/");
     * config.setApplicationDestinationPrefixes("/app");
     * }
     */

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:5173","*").withSockJS();
    }
}
