/**
 * MIT License

Copyright (c) 2015  Rob Terpilowski

Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING 
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.sumzerotrading.ib.example.market.data;

import com.sumzerotrading.broker.IBroker;
import com.sumzerotrading.broker.ib.InteractiveBrokersBroker;
import com.sumzerotrading.data.*;
import com.sumzerotrading.historicaldata.IHistoricalDataProvider;
import com.sumzerotrading.ib.IBConnectionUtil;
import com.sumzerotrading.ib.IBSocket;
import com.sumzerotrading.ib.historical.IBHistoricalDataProvider;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;
import com.sumzerotrading.marketdata.ILevel1Quote;
import com.sumzerotrading.marketdata.QuoteEngine;
import com.sumzerotrading.marketdata.QuoteType;
import com.sumzerotrading.marketdata.ib.IBQuoteEngine;
import com.sumzerotrading.realtime.bar.IRealtimeBarEngine;
import com.sumzerotrading.realtime.bar.RealtimeBarListener;
import com.sumzerotrading.realtime.bar.RealtimeBarRequest;
import com.sumzerotrading.realtime.bar.ib.IBRealTimeBarEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class MarketDataCurrencyExample {

    static final int IB_PORT = 4002;
    static final int TWS_PORT = 7497;
    static int CLIENT_ID;

    protected IBSocket ibSocket;
    protected QuoteEngine quoteEngine;
    protected IBroker broker;
    IRealtimeBarEngine realtimeBarProvider;
    protected IHistoricalDataProvider historicalDataProvider;
    
    public void start() {
        CLIENT_ID = ThreadLocalRandom.current().nextInt(1, 1000000 + 1);

        InteractiveBrokersClientInterface ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, CLIENT_ID);
        ibClient.connect();

        CurrencyTicker ticker = new CurrencyTicker();
        ticker.setExchange(Exchange.IDEALPRO);
        ticker.setSymbol("EUR");
        ticker.setCurrency("USD");
        ticker.setContractMultiplier(BigDecimal.ONE);
        int duration = 1;
        BarData.LengthUnit durationUnit = BarData.LengthUnit.DAY;
        int barSize = 1;
        BarData.LengthUnit barSizeUnit = BarData.LengthUnit.MINUTE;
        IHistoricalDataProvider.ShowProperty dataToRequest = IHistoricalDataProvider.ShowProperty.MIDPOINT;

        List<BarData> historicalData = ibClient.requestHistoricalData(ticker, duration, durationUnit, barSize, barSizeUnit, dataToRequest);

        System.out.println("Retrieved " + historicalData.size() + " bars");
        historicalData.stream().forEach((bar) -> {
            System.out.println("Retrieved Bar: " + bar);
        });

        IBConnectionUtil util = new IBConnectionUtil("localhost", IB_PORT, ibClient.getClientId());

        ibSocket = util.getIBSocket();
        quoteEngine = new IBQuoteEngine(ibSocket);
        broker = new InteractiveBrokersBroker(ibSocket);

        historicalDataProvider = new IBHistoricalDataProvider(ibSocket);
        historicalDataProvider.connect();
        realtimeBarProvider = new IBRealTimeBarEngine(quoteEngine, historicalDataProvider);

        RealtimeBarRequest request = new RealtimeBarRequest(ibClient.getClientId(), ticker, barSize, barSizeUnit, dataToRequest);

        realtimeBarProvider.subscribeRealtimeBars(request, (int requestId, Ticker _ticker, BarData bar) -> {
            System.out.println(bar.toString());
        });

        RealtimeBarListener listener = (int requestId, Ticker _ticker, BarData bar) -> {
            System.out.println(bar.toString());
        };

        ibClient.subscribeRealtimeBar(request, listener);

        ibClient.disconnect();
        
    }
    
    public static void main(String[] args) {
        new MarketDataCurrencyExample().start();
    }
}
