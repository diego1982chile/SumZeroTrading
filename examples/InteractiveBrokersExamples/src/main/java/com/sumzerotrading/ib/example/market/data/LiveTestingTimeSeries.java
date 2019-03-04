/*
  The MIT License (MIT)

  Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
  the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.sumzerotrading.ib.example.market.data;

import com.sumzerotrading.data.BarData;
import com.sumzerotrading.data.CurrencyTicker;
import com.sumzerotrading.data.Exchange;
import com.sumzerotrading.data.Ticker;
import com.sumzerotrading.historicaldata.IHistoricalDataProvider;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;
import com.sumzerotrading.realtime.bar.RealtimeBarRequest;
import org.ta4j.core.*;
import org.ta4j.core.analysis.CashFlow;
import ta4jexamples.loaders.CsvTicksLoader;
import ta4jexamples.research.MultipleStrategy;
import ta4jexamples.strategies.*;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is an example of a dummy trading bot using ta4j.
 * <p/>
 */
public class LiveTestingTimeSeries {

    /** Close price of the last bar */
    private static Decimal LAST_BAR_CLOSE_PRICE;

    private static TimeSeries live;

    static final int IB_PORT = 4002;
    static final int TWS_PORT = 7497;
    static int CLIENT_ID;
    static CurrencyTicker ticker;
    static InteractiveBrokersClientInterface ibClient;

    static final int DURATION = 1;
    static final BarData.LengthUnit DURATION_UNIT = BarData.LengthUnit.YEAR;
    static final int BAR_SIZE = 1;
    static final BarData.LengthUnit BAR_SIZE_UNIT = BarData.LengthUnit.DAY;
    static final IHistoricalDataProvider.ShowProperty DATA_TO_REQUEST =  IHistoricalDataProvider.ShowProperty.MIDPOINT;

    static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd HH:mm:ss")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd")
            .toFormatter();

    /**
     * Builds a moving time series (i.e. keeping only the maxBarCount last bars)
     */
    private static void start() {
        CLIENT_ID = ThreadLocalRandom.current().nextInt(1, 1000000 + 1);
        ibClient = InteractiveBrokersClient.getInstance("localhost", IB_PORT, CLIENT_ID);
        ibClient.connect();

        ticker = new CurrencyTicker();
        ticker.setExchange(Exchange.IDEALPRO);
        ticker.setSymbol("EUR");
        ticker.setCurrency("USD");
        ticker.setContractMultiplier(BigDecimal.ONE);

        live = new BaseTimeSeries();
    }

    /**
     * Builds a moving time series (i.e. keeping only the maxBarCount last bars)
     * @param maxBarCount the number of bars to keep in the time series (at maximum)
     * @return a moving time series
     */
    private static TimeSeries initMovingTimeSeries(int maxBarCount) {

        List<BarData> historicalData = ibClient.requestHistoricalData(ticker, DURATION, DURATION_UNIT, BAR_SIZE, BAR_SIZE_UNIT, DATA_TO_REQUEST);

        for (BarData barData : historicalData) {
            live.addBar(toBar(barData));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("test.csv"))) {

            for (Bar bar : live.getBarData()) {
                writer.newLine();
                writer.write(bar.getBeginTime().format(DATE_FORMATTER) + ";" + bar.getOpenPrice() + ";" + bar.getMaxPrice() + ";" + bar.getMinPrice() + ";" + bar.getClosePrice() + ";" + bar.getVolume());
                writer.flush();
            }
            writer.close();

            //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
            //history = CsvTicksLoader.load("EURUSD_Daily_201701020000_201712290000.csv");

            System.out.print("Initial bar count: " + live.getBarCount());
            // Limitating the number of bars to maxBarCount
            live.setMaximumBarCount(maxBarCount);
            LAST_BAR_CLOSE_PRICE = live.getBar(live.getEndIndex()).getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + LAST_BAR_CLOSE_PRICE);

        } catch (IOException e) {
                e.printStackTrace();
        }

        return live;
    }

        /**
     * @param series a time series
     * @return a dummy strategy
     */
    private static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        List<Strategy> strategies = new ArrayList<>();

        //strategies.add(CCICorrectionStrategy.buildStrategy(series));
        strategies.add(GlobalExtremaStrategy.buildStrategy(series));
        //strategies.add(MovingMomentumStrategy.buildStrategy(series));
        //strategies.add(RSI2Strategy.buildStrategy(series));
        strategies.add(MACDStrategy.buildStrategy(series));
        strategies.add(StochasticStrategy.buildStrategy(series));
        //strategies.add(ParabolicSARStrategy.buildStrategy(series));
        strategies.add(MovingAveragesStrategy.buildStrategy(series));
        strategies.add(BagovinoStrategy.buildStrategy(series));
        //strategies.add(FXBootCampStrategy.buildStrategy(series));

        MultipleStrategy multipleStrategy = new MultipleStrategy(strategies);

        return multipleStrategy.buildStrategy(series);
    }

    public static void main(String[] args) throws InterruptedException {


        System.out.println("********************** Initialization **********************");
        start();
        // Getting the time series
        TimeSeries series = initMovingTimeSeries(240);

        // Building the trading strategy
        Strategy strategy = buildStrategy(series);

        // Initializing the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");

        RealtimeBarRequest request = new RealtimeBarRequest(ibClient.getClientId(), ticker, BAR_SIZE, BAR_SIZE_UNIT, DATA_TO_REQUEST);

        int cont = 1;

        ibClient.subscribeRealtimeBar(request, (int requestId, Ticker _ticker, BarData bar) -> {
            System.out.println(bar.toString());
            LAST_BAR_CLOSE_PRICE = Decimal.valueOf(bar.getClose());
            Bar newBar = toBar(bar);
            System.out.println("------------------------------------------------------\n"
                    + "Bar "+cont+" added, close price = " + newBar.getClosePrice().doubleValue());
            series.addBar(newBar);

            int endIndex = series.getEndIndex();
            if (strategy.shouldEnter(endIndex)) {
                // Our strategy should enter
                System.out.println("Strategy should ENTER on " + endIndex);
                boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), Decimal.TEN);
                if (entered) {
                    Order entry = tradingRecord.getLastEntry();
                    System.out.println("Entered on " + entry.getIndex()
                            + " (price=" + entry.getPrice().doubleValue()
                            + ", amount=" + entry.getAmount().doubleValue() + ")");
                }
            } else if (strategy.shouldExit(endIndex)) {
                // Our strategy should exit
                System.out.println("Strategy should EXIT on " + endIndex);
                boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), Decimal.TEN);
                if (exited) {
                    Order exit = tradingRecord.getLastExit();
                    System.out.println("Exited on " + exit.getIndex()
                            + " (price=" + exit.getPrice().doubleValue()
                            + ", amount=" + exit.getAmount().doubleValue() + ")");
                }
            }
            //cont++;
        });


        // Getting the cash flow of the resulting trades
        CashFlow cashFlow = new CashFlow(series, tradingRecord);

        for (int i = 0; i < 459; ++i) {
            try {
                System.out.println("CashFlow["+ i +"]: " + cashFlow.getValue(i));
            }
            catch (IndexOutOfBoundsException e) {
                return;
            }
        }
    }

    private static Bar toBar(BarData barData) {
        LocalDateTime time = barData.getDateTime();
        double open = barData.getOpen().doubleValue();
        double high = barData.getHigh().doubleValue();
        double low = barData.getLow().doubleValue();
        double close = barData.getClose().doubleValue();
        double volume = barData.getVolume().doubleValue();
        return new BaseBar(time.atZone(ZoneId.of("America/Santiago")), open, high, low, close, volume);
    }
}
