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

package com.sumzerotrading.data;

import java.io.Serializable;

/**
 *
 * @author Rob Terpilowski
 */
public class Exchange implements Serializable {

    public static final long serialVersionUID = 1L;
    
    

    public static final Exchange NYMEX = new Exchange("NYMEX");
    public static final Exchange ECBOT = new Exchange("ECBOT");
    public static final Exchange NYBOT = new Exchange("NYBOT");
    public static final Exchange CFE = new Exchange("CFE");
    public static final Exchange NYSE_LIFFE = new Exchange("NYSELIFFE");
    public static final Exchange IDEALPRO = new Exchange("IDEALPRO");
    public static final Exchange INTERACTIVE_BROKERS_SMART = new Exchange("SMART");
    public static final Exchange TSEJ = new Exchange("TSEJ");
    public static final Exchange SEHKNTL = new Exchange("SEHKNTL");
    public static final Exchange SEHK = new Exchange("SEHK");
    public static final Exchange OSE = new Exchange("OSE.JPN");
    public static final Exchange SGX = new Exchange("SGX");
    public static final Exchange BOX = new Exchange("BOX");
    public static final Exchange ACE = new Exchange("ACE");
    public static final Exchange AEB = new Exchange("AEB");
    public static final Exchange AMEX = new Exchange("AMEX");
    public static final Exchange ARCA = new Exchange("ARCA");
    public static final Exchange ASX = new Exchange("ASX");
    public static final Exchange BELFOX = new Exchange("BELFOX");
    public static final Exchange BRUT = new Exchange("BRUT");
    public static final Exchange BTRADE = new Exchange("BTRADE");
    public static final Exchange BVME = new Exchange("BVME");
    public static final Exchange CBOE = new Exchange("CBOE");
    public static final Exchange DTB = new Exchange("DTB");
    public static final Exchange EOE = new Exchange("EOE");
    public static final Exchange GLOBEX = new Exchange("GLOBEX");
    public static final Exchange HKFE = new Exchange("HKFE");
    public static final Exchange IBIS = new Exchange("IBIS");
    public static final Exchange IDEAL = new Exchange("IDEAL");
    public static final Exchange IDEM = new Exchange("IDEM");
    public static final Exchange INSTINET = new Exchange("INSTINET");
    public static final Exchange ISE = new Exchange("ISE");
    public static final Exchange ISLAND = new Exchange("ISLAND");
    public static final Exchange LIFFE = new Exchange("LIFFE");
    public static final Exchange LSE = new Exchange("LSE");
    public static final Exchange MATIF = new Exchange("MATIF");
    public static final Exchange MEFFRV = new Exchange("MEFFRV");
    public static final Exchange MONEP = new Exchange("MONEP");
    public static final Exchange MXT = new Exchange("MXT");
    public static final Exchange NASDAQ = new Exchange("NASDAQ");
    public static final Exchange NYSE = new Exchange("NYSE");
    public static final Exchange OMLX = new Exchange("OMLX");
    public static final Exchange OMS = new Exchange("OMS");
    public static final Exchange PHLX = new Exchange("PHLX");
    public static final Exchange PSE = new Exchange("PSE");
    public static final Exchange RDBK = new Exchange("RDBK");
    public static final Exchange SFB = new Exchange("SFB");
    public static final Exchange SNFE = new Exchange("SNFE");
    public static final Exchange SOFFEX = new Exchange("SOFFEX");
    public static final Exchange VIRTX = new Exchange("VIRTX");
    public static final Exchange VWAP = new Exchange("VWAP");
    public static final Exchange ZSE = new Exchange("ZSE");


    public static final Exchange[] ALL_EXCHANGES = { ARCA, GLOBEX, NYMEX, CBOE, ECBOT, NYBOT, CFE,
        NYSE_LIFFE, IDEALPRO, PSE, INTERACTIVE_BROKERS_SMART, NASDAQ, TSEJ, SEHKNTL, SEHK, HKFE, OSE, SGX, BOX,
        ACE, AEB, AMEX, ARCA, ASX, BELFOX, BRUT, BTRADE, BVME, CBOE, DTB, EOE, GLOBEX, HKFE, IBIS, IDEAL, IDEM,
        INSTINET, ISE, ISLAND, LIFFE, LSE, MATIF, MEFFRV, MONEP, MXT, NASDAQ, NYSE, OMLX, OMS, PHLX, PSE, RDBK, SFB,
        SNFE, SOFFEX, VIRTX, VWAP, ZSE
    };

    protected String exchangeName;
    
    
    protected Exchange( String exchangeName ) {
        this.exchangeName = exchangeName;
    }

    public String getExchangeName() {
        return exchangeName;
    }
    
    public static Exchange getExchangeFromString(String exchangeString) {
        for( Exchange exchange : ALL_EXCHANGES ) {
            if( exchangeString.equals(exchange.getExchangeName())) {
                return exchange;
            }
        }
        
        throw new SumZeroException("Unknown exchange " + exchangeString );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.exchangeName != null ? this.exchangeName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Exchange other = (Exchange) obj;
        if ((this.exchangeName == null) ? (other.exchangeName != null) : !this.exchangeName.equals(other.exchangeName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Exchange{" + "exchangeName=" + exchangeName + '}';
    }
            
    
    
    
            
            
            
}
