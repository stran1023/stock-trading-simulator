package com.tradingapp.leaderboard.service.impl;

import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.leaderboard.dto.LeaderboardEntry;
import com.tradingapp.leaderboard.service.LeaderboardService;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.portfolio.entity.Holding;
import com.tradingapp.portfolio.repository.HoldingRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final String LEADERBOARD_KEY = "leaderboard:roi";
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000000");

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final MarketService marketService;
    private final RedisTemplate<String, String> redisTemplate;

    public LeaderboardServiceImpl(UserRepository userRepository,
                                  HoldingRepository holdingRepository,
                                  MarketService marketService,
                                  RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.marketService = marketService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void updateRoi(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            BigDecimal holdingsValue = holdingRepository.findByUserId(userId).stream()
                    .map(h -> marketService.getPriceValue(h.getSymbol())
                            .multiply(BigDecimal.valueOf(h.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalValue = user.getBalance().add(holdingsValue);
            BigDecimal roi = totalValue.subtract(STARTING_BALANCE)
                    .divide(STARTING_BALANCE, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId.toString(), roi.doubleValue());
        });
    }

    @Override
    public List<LeaderboardEntry> getTopN(int limit) {
        Set<ZSetOperations.TypedTuple<String>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);

        if (entries == null || entries.isEmpty()) return List.of();

        List<LeaderboardEntry> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : entries) {
            Long userId = Long.parseLong(tuple.getValue());
            double score = tuple.getScore() != null ? tuple.getScore() : 0.0;

            String username = userRepository.findById(userId)
                    .map(u -> u.getDisplayUsername())
                    .orElse("unknown");

            BigDecimal roi = BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalValue = STARTING_BALANCE
                    .multiply(BigDecimal.ONE.add(roi.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)))
                    .setScale(4, RoundingMode.HALF_UP);

            result.add(new LeaderboardEntry(rank++, username, roi, totalValue));
        }
        return result;
    }
}
