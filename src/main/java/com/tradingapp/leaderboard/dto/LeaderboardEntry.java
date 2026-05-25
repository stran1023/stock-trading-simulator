package com.tradingapp.leaderboard.dto;

import java.math.BigDecimal;

public record LeaderboardEntry(int rank, String username, BigDecimal roi, BigDecimal totalValue) {}
