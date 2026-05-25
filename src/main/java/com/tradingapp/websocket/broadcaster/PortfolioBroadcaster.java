package com.tradingapp.websocket.broadcaster;

import com.tradingapp.portfolio.service.PortfolioService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PortfolioBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioService portfolioService;

    public PortfolioBroadcaster(SimpMessagingTemplate messagingTemplate,
                                PortfolioService portfolioService) {
        this.messagingTemplate = messagingTemplate;
        this.portfolioService = portfolioService;
    }

    public void broadcast(Long userId) {
        messagingTemplate.convertAndSend(
                "/topic/portfolio/" + userId,
                portfolioService.getPortfolio(userId)
        );
    }
}
