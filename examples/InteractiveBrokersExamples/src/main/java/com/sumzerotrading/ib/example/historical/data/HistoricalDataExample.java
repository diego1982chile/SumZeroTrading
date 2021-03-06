/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sumzerotrading.ib.example.historical.data;

import com.sumzerotrading.data.BarData;
import com.sumzerotrading.data.BarData.LengthUnit;
import com.sumzerotrading.data.CurrencyTicker;
import com.sumzerotrading.data.Exchange;
import com.sumzerotrading.data.StockTicker;
import com.sumzerotrading.historicaldata.IHistoricalDataProvider.ShowProperty;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author RobTerpilowski
 */
public class HistoricalDataExample {

    static final int IB_PORT = 4002;
    static final int TWS = 7497;
    
    
    public void requestHistoricalData() {
        InteractiveBrokersClientInterface client = InteractiveBrokersClient.getInstance("localhost", IB_PORT, 1);
        client.connect();
        
        //StockTicker ticker = new StockTicker("AMZN");
        CurrencyTicker ticker = new CurrencyTicker();
        ticker.setExchange(Exchange.IDEALPRO);
        ticker.setSymbol("EUR");
        ticker.setCurrency("USD");
        ticker.setContractMultiplier(BigDecimal.ONE);
        int duration = 1;
        LengthUnit durationUnit = LengthUnit.DAY;
        int barSize = 1;
        LengthUnit barSizeUnit = LengthUnit.MINUTE;
        ShowProperty dataToRequest = ShowProperty.MIDPOINT;

        List<BarData> historicalData = client.requestHistoricalData(ticker, duration, durationUnit, barSize, barSizeUnit, dataToRequest);
        
        System.out.println("Retrieved " + historicalData.size() + " bars");
        historicalData.stream().forEach((bar) -> {
            System.out.println("Retrieved Bar: " + bar);
        });
        
        client.disconnect();
    }
    
    
    public static void main(String[] args) {
        new HistoricalDataExample().requestHistoricalData();
    }
}
