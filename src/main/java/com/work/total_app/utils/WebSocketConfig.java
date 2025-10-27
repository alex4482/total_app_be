package com.work.total_app.utils;

import org.springframework.lang.NonNull;
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
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:5173","*").withSockJS();
    }
}
