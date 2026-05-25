package com.tradingapp.portfolio.service;

import com.tradingapp.auth.entity.User;
import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.common.exception.ResourceNotFoundException;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.portfolio.dto.PortfolioResponse;
import com.tradingapp.portfolio.dto.TransactionDto;
import com.tradingapp.portfolio.entity.Holding;
import com.tradingapp.portfolio.entity.Transaction;
import com.tradingapp.portfolio.repository.HoldingRepository;
import com.tradingapp.portfolio.repository.TransactionRepository;
import com.tradingapp.portfolio.service.impl.PortfolioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock HoldingRepository holdingRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock MarketService marketService;

    @InjectMocks PortfolioServiceImpl portfolioService;

    static final Long USER_ID = 1L;

    // ── getPortfolio ───────────────────────────────────────────────

    @Test
    void getPortfolio_emptyHoldings_returnsCashOnly() {
        User user = user(USER_ID, new BigDecimal("1000000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserId(USER_ID)).thenReturn(List.of());

        PortfolioResponse res = portfolioService.getPortfolio(USER_ID);

        assertThat(res.cash()).isEqualByComparingTo("1000000.0000");
        assertThat(res.totalValue()).isEqualByComparingTo("1000000.0000");
        assertThat(res.totalPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(res.holdings()).isEmpty();
        verify(marketService, never()).getPriceValue(any());
    }

    @Test
    void getPortfolio_withHoldings_computesPnlCorrectly() {
        User user = user(USER_ID, new BigDecimal("850000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Holding h = holding(USER_ID, "AAPL", 10, new BigDecimal("100.00"));
        when(holdingRepository.findByUserId(USER_ID)).thenReturn(List.of(h));
        when(marketService.getPriceValue("AAPL")).thenReturn(new BigDecimal("150.00"));

        PortfolioResponse res = portfolioService.getPortfolio(USER_ID);

        // holdingsValue = 10 * 150 = 1500
        // pnl = (150 - 100) * 10 = 500
        // totalValue = 850000 + 1500 = 851500
        assertThat(res.cash()).isEqualByComparingTo("850000.0000");
        assertThat(res.totalValue()).isEqualByComparingTo("851500.0000");
        assertThat(res.totalPnl()).isEqualByComparingTo("500.00");
        assertThat(res.holdings()).hasSize(1);
        assertThat(res.holdings().get(0).symbol()).isEqualTo("AAPL");
        assertThat(res.holdings().get(0).pnl()).isEqualByComparingTo("500.00");
    }

    @Test
    void getPortfolio_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPortfolio(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getHistory ─────────────────────────────────────────────────

    @Test
    void getHistory_returnsPagedTransactions() {
        Transaction tx = transaction(USER_ID, "AAPL", "BUY", 5, new BigDecimal("150.00"));
        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findByUserIdOrderByTimestampDesc(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionDto> page = portfolioService.getHistory(USER_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        TransactionDto dto = page.getContent().get(0);
        assertThat(dto.symbol()).isEqualTo("AAPL");
        assertThat(dto.type()).isEqualTo("BUY");
        assertThat(dto.quantity()).isEqualTo(5);
        assertThat(dto.price()).isEqualByComparingTo("150.00");
    }

    @Test
    void getHistory_empty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findByUserIdOrderByTimestampDesc(eq(USER_ID), any()))
                .thenReturn(Page.empty());

        Page<TransactionDto> page = portfolioService.getHistory(USER_ID, pageable);

        assertThat(page.isEmpty()).isTrue();
    }

    // ── helpers ────────────────────────────────────────────────────

    private User user(Long id, BigDecimal balance) {
        User u = new User();
        u.setId(id);
        u.setUsername("alice");
        u.setEmail("alice@example.com");
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

    private Transaction transaction(Long userId, String symbol, String type, int quantity, BigDecimal price) {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setUserId(userId);
        t.setSymbol(symbol);
        t.setType(type);
        t.setQuantity(quantity);
        t.setPrice(price);
        t.setTimestamp(OffsetDateTime.now());
        return t;
    }
}
