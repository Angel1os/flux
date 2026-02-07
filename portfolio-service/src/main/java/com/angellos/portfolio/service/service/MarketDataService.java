package com.angellos.portfolio.service.service;

import java.math.BigDecimal;

/**
 * Service to fetch current prices from Market Data Service.
 * This is a client service that calls Market Data Service REST API.
 */
public interface MarketDataService {

    BigDecimal getCurrentPrice(String symbol);

}
