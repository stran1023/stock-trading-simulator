package com.tradingapp.watchlist.repository;

import com.tradingapp.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserId(Long userId);
    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    @Modifying
    @Query("DELETE FROM Watchlist w WHERE w.userId = :userId AND w.symbol = :symbol")
    void deleteByUserIdAndSymbol(Long userId, String symbol);

    @Query("SELECT DISTINCT w.symbol FROM Watchlist w")
    List<String> findAllDistinctSymbols();
}
