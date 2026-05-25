package com.tradingapp.trading.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TradeRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
