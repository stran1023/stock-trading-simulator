package com.tradingapp.trading.service;

import com.tradingapp.auth.entity.User;
import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.common.exception.InsufficientBalanceException;
import com.tradingapp.common.exception.InsufficientHoldingException;
import com.tradingapp.common.exception.ResourceNotFoundException;
import com.tradingapp.leaderboard.service.LeaderboardService;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.portfolio.entity.Holding;
import com.tradingapp.portfolio.repository.HoldingRepository;
import com.tradingapp.portfolio.repository.TransactionRepository;
import com.tradingapp.trading.dto.TradeRequest;
import com.tradingapp.trading.dto.TradeResponse;
import com.tradingapp.trading.service.impl.TradeServiceImpl;
import com.tradingapp.websocket.broadcaster.PortfolioBroadcaster;
import com.tradingapp.websocket.broadcaster.TradeBroadcaster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock UserRepository userRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock MarketService marketService;
    @Mock LeaderboardService leaderboardService;
    @Mock PortfolioBroadcaster portfolioBroadcaster;
    @Mock TradeBroadcaster tradeBroadcaster;

    @InjectMocks TradeServiceImpl tradeService;

    static final BigDecimal PRICE = new BigDecimal("150.00");
    static final Long USER_ID = 1L;

    // ── buy ────────────────────────────────────────────────────────

    @Test
    void buy_firstBuy_createsNewHolding() {
        User user = user(USER_ID, new BigDecimal("1000000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(marketService.getPriceValue("AAPL")).thenReturn(PRICE);
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeResponse res = tradeService.buy(USER_ID, tradeRequest("AAPL", 5));

        assertThat(res.symbol()).isEqualTo("AAPL");
        assertThat(res.quantity()).isEqualTo(5);
        assertThat(res.price()).isEqualByComparingTo(PRICE);
        assertThat(res.total()).isEqualByComparingTo("750.00");

        ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
        assertThat(captor.getValue().getAveragePrice()).isEqualByComparingTo(PRICE);
    }

    @Test
    void buy_existingHolding_updatesWeightedAverage() {
        User user = user(USER_ID, new BigDecimal("1000000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(marketService.getPriceValue("AAPL")).thenReturn(new BigDecimal("200.00"));

        Holding existing = holding(USER_ID, "AAPL", 5, new BigDecimal("100.00"));
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenReturn(user);
        when(holdingRepository.save(any())).thenReturn(existing);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tradeService.buy(USER_ID, tradeRequest("AAPL", 5));

        // new avg = (5*100 + 5*200) / 10 = 150
        assertThat(existing.getQuantity()).isEqualTo(10);
        assertThat(existing.getAveragePrice()).isEqualByComparingTo("150.0000");
    }

    @Test
    void buy_insufficientBalance_throws() {
        User user = user(USER_ID, new BigDecimal("100.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(marketService.getPriceValue("AAPL")).thenReturn(PRICE);

        assertThatThrownBy(() -> tradeService.buy(USER_ID, tradeRequest("AAPL", 10)))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    // ── sell ───────────────────────────────────────────────────────

    @Test
    void sell_partialSell_reducesQuantity() {
        User user = user(USER_ID, new BigDecimal("0.00"));
        Holding holding = holding(USER_ID, "AAPL", 10, PRICE);
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.of(holding));
        when(marketService.getPriceValue("AAPL")).thenReturn(PRICE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(holdingRepository.save(any())).thenReturn(holding);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeResponse res = tradeService.sell(USER_ID, tradeRequest("AAPL", 3));

        assertThat(res.quantity()).isEqualTo(3);
        assertThat(holding.getQuantity()).isEqualTo(7);
        verify(holdingRepository, never()).delete(any());
    }

    @Test
    void sell_allShares_deletesHolding() {
        User user = user(USER_ID, new BigDecimal("0.00"));
        Holding holding = holding(USER_ID, "AAPL", 5, PRICE);
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.of(holding));
        when(marketService.getPriceValue("AAPL")).thenReturn(PRICE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tradeService.sell(USER_ID, tradeRequest("AAPL", 5));

        verify(holdingRepository).delete(holding);
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void sell_insufficientQuantity_throws() {
        Holding holding = holding(USER_ID, "AAPL", 2, PRICE);
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> tradeService.sell(USER_ID, tradeRequest("AAPL", 5)))
                .isInstanceOf(InsufficientHoldingException.class);
    }

    @Test
    void sell_noHolding_throws() {
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.sell(USER_ID, tradeRequest("AAPL", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buy_broadcastsAfterTrade() {
        User user = user(USER_ID, new BigDecimal("1000000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(marketService.getPriceValue("AAPL")).thenReturn(PRICE);
        when(holdingRepository.findByUserIdAndSymbol(USER_ID, "AAPL")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tradeService.buy(USER_ID, tradeRequest("AAPL", 1));

        verify(portfolioBroadcaster).broadcast(USER_ID);
        verify(tradeBroadcaster).broadcast(eq(USER_ID), any(TradeResponse.class));
        verify(leaderboardService).updateRoi(USER_ID);
    }

    // ── helpers ────────────────────────────────────────────────────

    private TradeRequest tradeRequest(String symbol, int quantity) {
        TradeRequest r = new TradeRequest();
        r.setSymbol(symbol);
        r.setQuantity(quantity);
        return r;
    }

    private User user(Long id, BigDecimal balance) {
        User u = new User();
        u.setId(id);
        u.setUsername("trader");
        u.setEmail("trader@example.com");
        u.setPasswordHash("hashed");
        u.setBalance(balance);
        return u;
    }

    private Holding holding(Long userId, String symbol, int quantity, BigDecimal avgPrice) {
        Holding h = new Holding();
        h.setUserId(userId);
        h.setSymbol(symbol);
        h.setQuantity(quantity);
        h.setAveragePrice(avgPrice);
        return h;
    }
}
