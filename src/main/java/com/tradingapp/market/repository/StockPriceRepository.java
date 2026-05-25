package com.tradingapp.market.repository;

import com.tradingapp.market.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockPriceRepository extends JpaRepository<StockPrice, String> {

    @Query("SELECT sp.symbol FROM StockPrice sp")
    List<String> findAllSymbols();
}
