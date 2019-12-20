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

import cl.dsoto.trading.clients.ServiceLocator;
import cl.dsoto.trading.components.PeriodManager;
import cl.dsoto.trading.model.*;
import com.sumzerotrading.broker.Position;
import com.sumzerotrading.broker.order.OrderStatus;
import com.sumzerotrading.broker.order.TradeDirection;
import com.sumzerotrading.broker.order.TradeOrder;
import com.sumzerotrading.data.*;
import com.sumzerotrading.historicaldata.IHistoricalDataProvider;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClient;
import com.sumzerotrading.interactive.brokers.client.InteractiveBrokersClientInterface;
import com.sumzerotrading.realtime.bar.RealtimeBarRequest;
import org.ta4j.core.*;
import org.ta4j.core.Strategy;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import ta4jexamples.loaders.CsvTicksLoader;
import ta4jexamples.research.MultipleStrategy;
import ta4jexamples.strategies.*;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.*;

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
    static int CLIENT_ID = 0;
    static CurrencyTicker TICKER;
    static InteractiveBrokersClientInterface IB_CLIENT;
    static int AMOUNT = 20000;
    static int MAX_BAR_COUNT = 240;

    static final int START_EUROPEAN_SESSION = 4;
    static final int END_EUROPEAN_SESSION = 12;

    static final int START_AMERICAN_SESSION = 9;
    static final int END_AMERICAN_SESSION = 17;

    static PeriodManager periodManager = (PeriodManager) ServiceLocator.getInstance().getService(PeriodManager.class);

    static final int DURATION = 1;
    static final BarData.LengthUnit DURATION_UNIT = BarData.LengthUnit.MONTH;
    static final int BAR_SIZE = 1;
    static final BarData.LengthUnit BAR_SIZE_UNIT = BarData.LengthUnit.HOUR;
    static final IHistoricalDataProvider.ShowProperty DATA_TO_REQUEST =  IHistoricalDataProvider.ShowProperty.MIDPOINT;
    static final Date END_DATE =  Date                        // Terrible old legacy class, avoid using. Represents a moment in UTC.
            .from(                                // New conversion method added to old classes for converting between legacy classes and modern classes.
                    LocalDate                         // Represents a date-only value, without time-of-day and without time zone.
                            .of( 2019 , 11 , 30 )              // Specify year-month-day. Notice sane counting, unlike legacy classes: 2014 means year 2014, 1-12 for Jan-Dec.
                            .atStartOfDay(                    // Let java.time determine first moment of the day. May *not* start at 00:00:00 because of anomalies such as Daylight Saving Time (DST).
                                    ZoneId.systemDefault()   // Specify time zone as `Continent/Region`, never the 3-4 letter pseudo-zones like `PST`, `EST`, or `IST`.
                            )                                 // Returns a `ZonedDateTime`.
                            .toInstant()                      // Adjust from zone to UTC. Returns a `Instant` object, always in UTC by definition.
            );

    static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd HH:mm:ss")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy.MM.dd")
            .toFormatter();

    static private final Logger logger = Logger.getLogger(LiveTestingTimeSeries.class.getName());
    static private FileHandler fh;

    /**
     * Builds a moving time series (i.e. keeping only the maxBarCount last bars)
     */
    private static void start() {

        try {
            //CLIENT_ID = ThreadLocalRandom.current().nextInt(1, 1000000 + 1);

            // Si el timeframe es de 1M se usara una serie movil de 1 SEMANA = 60 x 24 x 5 = 7200 (bars)
            if(BAR_SIZE_UNIT == BarData.LengthUnit.MINUTE) {
                MAX_BAR_COUNT = 7200;
            }
            // Si el timeframe es de 1H se usara una serie movil de 1 MES = 24 x 5 x 4 = 480 (bars)
            if(BAR_SIZE_UNIT == BarData.LengthUnit.HOUR) {
                MAX_BAR_COUNT = 480;
            }
            // Si el timeframe es de 1D se usara una serie movil de 1 AÑO = 20 x 12 = 240 (bars)
            if(BAR_SIZE_UNIT == BarData.LengthUnit.DAY) {
                MAX_BAR_COUNT = 240;
            }

            logger.log(Level.INFO, "Parametros globales: {IB_PORT = '" + IB_PORT + "', TWS_PORT = " + TWS_PORT +
                    ", CLIENT_ID = " + CLIENT_ID + ", AMOUNT = " + AMOUNT + ", MAX_BAR_COUNT = " + MAX_BAR_COUNT +
                    ", START_EUROPEAN_SESSION = " + START_EUROPEAN_SESSION + ", END_EUROPEAN_SESSION = " + END_EUROPEAN_SESSION +
                    ", START_AMERICAN_SESSION = " + START_AMERICAN_SESSION + ", END_AMERICAN_SESSION = " + END_AMERICAN_SESSION +
                    ", DURATION = " + DURATION + ", DURATION_UNIT = '" + DURATION_UNIT + "', BAR_SIZE = " + BAR_SIZE +
                    ", BAR_SIZE_UNIT = '" + BAR_SIZE_UNIT + "', DATA_TO_REQUEST = '" + DATA_TO_REQUEST + "', END_DATE = '" + END_DATE + "'}");

            logger.log(Level.INFO, "Conectando con Client ID: " + CLIENT_ID + " por puerto: " +  IB_PORT);

            IB_CLIENT = InteractiveBrokersClient.getInstance("localhost", IB_PORT, CLIENT_ID);
            //IB_CLIENT = InteractiveBrokersClient.getInstance("localhost", TWS_PORT, CLIENT_ID);
            IB_CLIENT.connect();

            logger.log(Level.INFO, "Conexion exitosa");

            logger.log(Level.INFO, "Inicializando Ticker con parametros: {Exchange = '" + Exchange.IDEALPRO + "', Symbol = 'EUR', Currency = 'USD', ContractMultiplier = " + BigDecimal.ONE + "}");

            TICKER = new CurrencyTicker();
            TICKER.setExchange(Exchange.IDEALPRO);
            //ticker.setExchange(Exchange.IDEAL);
            TICKER.setSymbol("EUR");
            TICKER.setCurrency("USD");
            TICKER.setContractMultiplier(BigDecimal.ONE);

            AMOUNT = 20000; //2mLote = 2*100*100 = 200(EUR/USD)*LVG

            /*
            ARCA, GLOBEX, NYMEX, CBOE, ECBOT, NYBOT, CFE, NYSE_LIFFE, IDEALPRO, PSE, INTERACTIVE_BROKERS_SMART, NASDAQ,
            TSEJ, SEHKNTL, SEHK, HKFE, OSE, SGX, BOX, ACE, AEB, AMEX, ARCA, ASX, BELFOX, BRUT, BTRADE, BVME, CBOE, DTB,
            EOE, GLOBEX, HKFE, IBIS, IDEAL, IDEM, INSTINET, ISE, ISLAND, LIFFE, LSE, MATIF, MEFFRV, MONEP, MXT, NASDAQ,
            NYSE, OMLX, OMS, PHLX, PSE, RDBK, SFB, SNFE, SOFFEX, VIRTX, VWAP, ZSE
            */
        }
        catch(Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * Builds a moving time series (i.e. keeping only the maxBarCount last bars)
     * @param maxBarCount the number of bars to keep in the time series (at maximum)
     * @return a moving time series
     */
    private static TimeSeries initMovingTimeSeries(int maxBarCount) throws Exception {

        logger.log(Level.INFO, "Inicializando Serie de Tiempo Movil");

        List<BarData> historicalData = new ArrayList<>();

        live = new BaseTimeSeries();

        //List<BarData> historicalData = ibClient.requestHistoricalData(ticker, DURATION, DURATION_UNIT, BAR_SIZE, BAR_SIZE_UNIT, DATA_TO_REQUEST);
        logger.log(Level.INFO, "Obteniendo data historica con parametros: {END_DATE = '" + END_DATE + "'" +
                               ", DURATION = " + DURATION + ", DURATION_UNIT = '" + DURATION_UNIT + "', BAR_SIZE = " + BAR_SIZE +
                               ", BAR_SIZE_UNIT = '" + BAR_SIZE_UNIT +"', DATA_TO_REQUEST = '" + DATA_TO_REQUEST +"'}");

        try {
            historicalData = IB_CLIENT.requestHistoricalData(TICKER, END_DATE, DURATION, DURATION_UNIT, BAR_SIZE, BAR_SIZE_UNIT, DATA_TO_REQUEST, false);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        logger.log(Level.INFO, "Registros recuperados: " + historicalData.size());

        for (BarData barData : historicalData) {

            try {
                live.addBar(toBar(barData));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("2019_NOV_H.csv"))) {

            writer.write("DATE;TIME;OPEN;HIGH;LOW;CLOSE;VOLUME");
            writer.flush();

            for (Bar bar : live.getBarData()) {

                writer.newLine();

                String open = String.format("%.5f", bar.getOpenPrice().doubleValue());

                String max = String.format("%.5f", bar.getMaxPrice().doubleValue());

                String min = String.format("%.5f", bar.getMinPrice().doubleValue());

                String close = String.format("%.5f", bar.getClosePrice().doubleValue());

                String volume = String.format("%.5f", bar.getVolume().doubleValue());

                String dateTime = bar.getEndTime().format(DATE_TIME_FORMATTER);
                String[] tokens = dateTime.split(" ");
                String date = tokens[0];
                String time = tokens[1];

                writer.write(date + ";" + time + ";" + open + ";" + max + ";" + min + ";" + close + ";" + volume);
                //writer.write(date + ";" + open + ";" + max + ";" + min + ";" + close + ";" + volume);
                writer.flush();
            }
            writer.close();


            //periodManager.generateOptimizations(live);

            //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
            //history = CsvTicksLoader.load("EURUSD_Daily_201701020000_201712290000.csv");

            System.out.print("Initial bar count: " + live.getBarCount());
            // Limitating the number of bars to maxBarCount
            LAST_BAR_CLOSE_PRICE = live.getBar(live.getEndIndex()).getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + LAST_BAR_CLOSE_PRICE);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        logger.log(Level.INFO, "Truncando Serie de Tiempo Movil MAX_BAR_COUNT: " + MAX_BAR_COUNT);

        live.setMaximumBarCount(maxBarCount);

        return live;
    }

    public static void generateLogs(TimeSeries series, TradingRecord tradingRecord) {

        String fileName = "resultados_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            // Analysis
            writer.write("Number of trades for our strategy: " + tradingRecord.getTradeCount());
            writer.newLine();
            writer.newLine();

            // Getting the profitable trades ratio
            AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
            writer.write("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
            writer.newLine();
            writer.newLine();

            // Getting the reward-risk ratio
            AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
            writer.write("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));
            writer.newLine();
            writer.newLine();

            // Total profit of our strategy
            // vs total profit of a buy-and-hold strategy
            AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
            writer.write("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
            writer.newLine();
            writer.newLine();

            for (int i = 0; i < tradingRecord.getTrades().size(); ++i) {
                writer.write("Trade["+ i +"]: " + tradingRecord.getTrades().get(i).toString());
                writer.newLine();
            }

            writer.newLine();

            // Getting the cash flow of the resulting trades
            CashFlow cashFlow = new CashFlow(series, tradingRecord);

            for (int i = 0; i < cashFlow.getSize(); ++i) {
                writer.write("CashFlow["+ i +"]: " + cashFlow.getValue(i));
                writer.newLine();
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws InterruptedException {


        System.out.println("********************** Initialization **********************");
        ZoneId z = ZoneId.of( "America/Santiago" );
        LocalDateTime today = LocalDateTime.now( z );
        addFileHandler(String.valueOf(today.getDayOfMonth()) + "-" + String.valueOf(today.getMonth()) + "-" + String.valueOf(today.getYear()) + ".txt");
        start();
        TimeSeries series;
        // Getting the time series
        try {
            series = initMovingTimeSeries(MAX_BAR_COUNT);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            return;
        }

        // Initializing the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");

        logger.log(Level.INFO, "Inicializando Solicitud de Barras en Tiempo Real con parametros: {BAR_SIZE = " + BAR_SIZE +
                               ", BAR_SIZE_UNIT: '" + BAR_SIZE_UNIT + "', DATA_TO_REQUEST = " + DATA_TO_REQUEST + "}");

        RealtimeBarRequest request = new RealtimeBarRequest(IB_CLIENT.getClientId(), TICKER, BAR_SIZE, BAR_SIZE_UNIT, DATA_TO_REQUEST);

        List<Object> barCounter = new ArrayList<>();
        List<Object> flag = new ArrayList<>();
        // Building the trading strategy
        List<Strategy> strategyBuffer = new ArrayList<>();

        logger.log(Level.INFO, "Suscribiendo solicitud...");

        IB_CLIENT.subscribeRealtimeBar(request, (int requestId, Ticker _ticker, BarData bar) -> {

            logger.log(Level.INFO, "Recuperando barra: " + bar.toString());
            LAST_BAR_CLOSE_PRICE = Decimal.valueOf(bar.getClose());
            Bar newBar = toBar(bar);

            // Agregar nuevo bar a series
            try {
                series.addBar(newBar);
                //System.out.println("\nBar "+barCounter.size()+" added, close price = " + newBar.getClosePrice().doubleValue());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //Incrementar el contador de bars
            barCounter.add(true);

            //Si han transcurrido MAX_BAR_COUNT bars, generar log con resultados periódicos
            if(barCounter.size() == MAX_BAR_COUNT) {
                logger.log(Level.INFO, "Se han recuperado: " + MAX_BAR_COUNT + " barras. Generando estadisticas para la unidad de duracion " + DURATION_UNIT + " mas reciente");
                barCounter.clear();
                generateLogs(series, tradingRecord);
            }

            //TODO: Actualizar periodicamente la estrategia. Ej: Todos los viernes
            if(today.getDayOfWeek().equals(DayOfWeek.FRIDAY) && flag.isEmpty()) {
                logger.log(Level.INFO, "Generando nuevas optimizaciones para estrategia de inversion...");
                flag.add(true);
                // Genera un nuevo period (optimizaciones)
                periodManager.generateOptimizations(series);
            }

            if(!today.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {

                Period period;
                List<org.ta4j.core.Strategy> strategies;
                MultipleStrategy multipleStrategy = null;

                //TODO: Recuperar la estrategia actualizada. Ej: Todos los lunes
                if(today.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
                    flag.clear();
                    strategyBuffer.clear();
                }

                if(strategyBuffer.isEmpty()) {
                    // Se recupera el ultimo period con las optimizaciones
                    period = periodManager.getLast(1).get(0);
                    logger.log(Level.INFO, "Recuperando ultima estrategia de inversion: " + period.toString());
                    // Se extraen las estrategias del period
                    strategies = period.extractStrategy(period);
                    multipleStrategy = new MultipleStrategy(strategies);
                    strategyBuffer.add(multipleStrategy.buildStrategy(series));
                }

            }

            if(strategyBuffer.size() > 1) {
                logger.log(Level.SEVERE, "El buffer de estrategias solo puede contener 1 estrategia!!. Contacte al administrador");
                return;
            }

            logger.log(Level.INFO, "today.getHour() = " + String.valueOf(today.getHour()));

            // TODO: Solo operar en las sesiones de mercado adecuadas: EUROPEA / AMERICANA
            if(today.getHour() >= START_EUROPEAN_SESSION && today.getHour() <= END_EUROPEAN_SESSION) {
                logger.log(Level.INFO, "Operando en el mercado...");

                int endIndex = series.getEndIndex();

                if (strategyBuffer.get(0).shouldEnter(endIndex)) {
                    // Our strategy should enter
                    //System.out.println("Strategy should ENTER on " + endIndex);
                    logger.log(Level.INFO, "La estrategia deberia entrar en " + endIndex);
                    boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), Decimal.TEN);
                    if (entered) {
                        Order entry = tradingRecord.getLastEntry();
                        // Entrar en compra en IB
                        String orderId = IB_CLIENT.getNextOrderId();
                        TradeOrder order = new TradeOrder(orderId, TICKER, AMOUNT, TradeDirection.BUY);
                        order.setType(TradeOrder.Type.MARKET);
                        logger.log(Level.INFO, "La estrategia ENTRARA en COMPRA con parametros: {ORDER_ID = " + orderId +
                                ", PRICE = " + entry.getPrice().doubleValue() + ", TYPE = " + TradeOrder.Type.MARKET + ", AMOUNT = " + AMOUNT);
                        IB_CLIENT.placeOrder(order);
                        //////////////////////////
                        //System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().doubleValue() + ", amount=" + entry.getAmount().doubleValue() + ")");
                    }
                } else if (strategyBuffer.get(0).shouldExit(endIndex)) {
                    // Our strategy should exit
                    //System.out.println("Strategy should EXIT on " + endIndex);
                    logger.log(Level.INFO, "La estrategia deberia salir en " + endIndex);
                    boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), Decimal.TEN);
                    if (exited) {
                        Order exit = tradingRecord.getLastExit();
                        // Entrar en venta en IB
                        String orderId = IB_CLIENT.getNextOrderId();
                        TradeOrder order = new TradeOrder(orderId, TICKER, AMOUNT, TradeDirection.SELL);
                        order.setType(TradeOrder.Type.MARKET);
                        logger.log(Level.INFO, "La estrategia SALDRA en VENTA con parametros: {ORDER_ID = " + orderId +
                                ", PRICE = " + exit.getPrice().doubleValue() + ", TYPE = " + TradeOrder.Type.MARKET + ", AMOUNT = " + AMOUNT);
                        IB_CLIENT.placeOrder(order);
                        //////////////////////////
                        //System.out.println("Exited on " + exit.getIndex() + " (PRICE=" + exit.getPrice().doubleValue() + ", amount=" + exit.getAmount().doubleValue() + ")");
                    }
                }

            }

        });

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

    private static void addFileHandler(String name) {

        try {

            // This block configure the logger with handler and formatter
            fh = new FileHandler(name);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);


        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
