package com.tradingapp.websocket.broadcaster;

import com.tradingapp.trading.dto.TradeResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TradeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public TradeBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(Long userId, TradeResponse response) {
        messagingTemplate.convertAndSend("/topic/trades/" + userId, response);
    }
}
