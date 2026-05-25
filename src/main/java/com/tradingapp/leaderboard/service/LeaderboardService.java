package com.tradingapp.leaderboard.service;

import com.tradingapp.leaderboard.dto.LeaderboardEntry;

import java.util.List;

public interface LeaderboardService {
    void updateRoi(Long userId);
    List<LeaderboardEntry> getTopN(int limit);
}
