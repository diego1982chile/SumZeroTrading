/**
 * MIT License
 *
 * Copyright (c) 2015  Rob Terpilowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.sumzerotrading.ib.example.trading;

import com.sumzerotrading.broker.order.TradeDirection;
import com.sumzerotrading.broker.order.TradeOrder;
import com.sumzerotrading.data.CurrencyTicker;
import com.sumzerotrading.data.Exchange;
import com.sumzerotrading.data.FuturesTicker;
import com.sumzerotrading.data.StockTicker;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingExample {

    protected InteractiveBrokersClientInterface ibClient;
    protected Logger logger = LoggerFactory.getLogger(TradingExample.class);

    static final int IB_PORT = 4002;
    static final int TWS = 7497;

    public void start() {
        //Connect to the Interactive Brokers TWS Client
        logger.debug("Connecting to IB Client");
        ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, 1);
        ibClient.connect();
        logger.debug("IB client connected" );
    }

    public void placeFuturesOrder() {
        InteractiveBrokersClientInterface ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, 1);
        ibClient.connect();

        //Create a crude oil futures ticker
        FuturesTicker futuresTicker = new FuturesTicker();
        futuresTicker.setSymbol("CL");
        futuresTicker.setExpiryMonth(4);
        futuresTicker.setExpiryYear(2016);
        futuresTicker.setExchange(Exchange.NYMEX);

        String orderId = ibClient.getNextOrderId();
        int contracts = 5;

        //Create the order and send to Interactive Brokers
        TradeOrder order = new TradeOrder(orderId, futuresTicker, contracts, TradeDirection.BUY);
        order.setType(TradeOrder.Type.LIMIT);
        order.setLimitPrice(32.50);
        ibClient.placeOrder(order);
    }
 
    public void placeEquityOrder() {
        InteractiveBrokersClientInterface ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, 1);
        ibClient.connect();

        StockTicker amazonTicker = new StockTicker("QQQ");
        String orderId = ibClient.getNextOrderId();
        int shares = 500;

        TradeOrder order = new TradeOrder(orderId, amazonTicker, shares, TradeDirection.SELL);

        //ibClient.placeOrder(order);
        ibClient.getOpenPositions();
    }

    public void placeCurrencyOrder() {
        InteractiveBrokersClientInterface ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, 1);
        ibClient.connect();
        CurrencyTicker eurTicker = new CurrencyTicker();
        eurTicker.setSymbol("EUR");
        eurTicker.setCurrency("USD");
        eurTicker.setExchange(Exchange.IDEALPRO);

        String orderId = ibClient.getNextOrderId();
        int amount = 50000;

        TradeOrder order = new TradeOrder(orderId, eurTicker, amount, TradeDirection.BUY);

        ibClient.placeOrder(order);
    }

    public static void main(String[] args) throws Exception {
        TradingExample example = new TradingExample();
        example.start();
        example.placeEquityOrder();
    }
}
