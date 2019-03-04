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

package com.sumzerotrading.broker.bitmex;

import com.sumzerotrading.broker.order.OrderEvent;
import java.util.concurrent.BlockingQueue;
//import org.apache.log4j.Logger;

/**
 *
 * @author Rob Terpilowski
 */
public class BitmexOrderEventProcessor implements Runnable  {

   // protected Logger logger = Logger.getLogger( IBOrderEventProcessor.class );
    protected BlockingQueue<OrderEvent> orderEventQueue;
    protected volatile boolean shouldRun = false;
    protected BitmexBroker broker;
    
    public BitmexOrderEventProcessor( BlockingQueue<OrderEvent> orderEventQueue, BitmexBroker broker ) {
        this.orderEventQueue = orderEventQueue;
        this.broker = broker;
    }
    
    
    public void startProcessor() {
        shouldRun = true;
        Thread thread = new Thread( this );
        thread.start();
    }
    
    
    public void stopProcessor() {
        shouldRun = false;
    }
    
    
    public void run() {
        while( shouldRun ) {
            try {
                broker.fireOrderEvent( orderEventQueue.take() );
            } catch( Exception ex ) {
                ex.printStackTrace();
             //   logger.error(ex, ex);
            }
        }
    }
    
    
    
}
