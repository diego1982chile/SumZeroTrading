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

import com.sumzerotrading.data.*;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;
import com.sumzerotrading.marketdata.ILevel1Quote;
import com.sumzerotrading.marketdata.QuoteType;
import com.sumzerotrading.realtime.bar.RealtimeBarListener;
import com.sumzerotrading.realtime.bar.RealtimeBarRequest;

import java.util.concurrent.ThreadLocalRandom;


public class MarketDataCurrencyExample {

    static final int IB_PORT = 4002;
    static final int TWS_PORT = 7497;
    static int CLIENT_ID;
    
    public void start() {
        CLIENT_ID = ThreadLocalRandom.current().nextInt(1, 1000000 + 1);

        InteractiveBrokersClientInterface ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, CLIENT_ID);
        ibClient.disconnect();
        ibClient.connect();
        CurrencyTicker eurTicker = new CurrencyTicker();
        eurTicker.setSymbol("EUR");
        eurTicker.setCurrency("USD");
        eurTicker.setExchange(Exchange.IDEALPRO);
        //eurTicker.setPrimaryExchange(Exchange.INTERACTIVE_BROKERS_SMART);
        //eurTicker.setContractMultiplier(null);

        RealtimeBarRequest request = new RealtimeBarRequest(1, eurTicker, 1, BarData.LengthUnit.MINUTE);

        ibClient.subscribeRealtimeBar(request, (int requestId, Ticker ticker, BarData bar) -> {
            System.out.println(bar.toString());
        });

        
    }
    
    public static void main(String[] args) {
        new MarketDataCurrencyExample().start();
    }
}
