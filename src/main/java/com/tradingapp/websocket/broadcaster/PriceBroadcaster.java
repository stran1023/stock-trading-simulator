package com.tradingapp.websocket.broadcaster;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class PriceBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public PriceBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(String symbol, BigDecimal price) {
        messagingTemplate.convertAndSend("/topic/prices", Map.of(
                "symbol", symbol,
                "price", price,
                "updatedAt", OffsetDateTime.now().toString()
        ));
    }
}
