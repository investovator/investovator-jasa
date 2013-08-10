/*
 * investovator, Stock Market Gaming Framework
 *     Copyright (C) 2013  investovator
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.jasa.agent.strategy;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import net.sourceforge.jasa.agent.TradingAgent;
import net.sourceforge.jasa.market.Market;
import net.sourceforge.jasa.market.Order;
import net.sourceforge.jasa.report.EquilibriumReportVariables;
import net.sourceforge.jasa.report.HistoricalDataReport;

import java.util.ArrayList;

/**
 * @author rajith
 * @version $Revision$
 */
public class MovingAverageCDStrategy extends AbstractTradingStrategy {

    public static final int QUICK_PERIODS_AVERAGE = 12;
    public static final int SLOW_PERIODS_AVERAGE = 26;
    public static final int SIGNAL_PERIOD_AVERAGE = 9;


    public enum PlaceOrder {SELL, BUY, NOTHING}
    private EquilibriumReportVariables equilibriumReportVariables;

    private int quantity;
    private boolean isBuy;

    public MovingAverageCDStrategy (){
        super();
        quantity = 1;
    }

    @Override
    public int determineQuantity(Market auction) {
        return quantity;  //TODO check
    }

    public boolean modifyShout(Order shout) {
        if(equilibriumReportVariables.getMatchedShouts() != null){
            PlaceOrder placeOrder = inMarketSignal();
            if(placeOrder == PlaceOrder.NOTHING){
                return false;
            }
            else {
                determineStockQuantity(placeOrder);
                shout.setAgent(getAgent());
                shout.setQuantity(quantity);
                shout.setPrice(getAgent().getValuation(auction));
                if(placeOrder == PlaceOrder.BUY ) {
                    shout.setIsBid(true);
                }
                else {
                    shout.setIsBid(false);
                }
                return true;
            }
        } else
            return false;
    }

    private void determineStockQuantity(PlaceOrder signal){
        if(signal == PlaceOrder.BUY){
            quantity = (int) ((getAgent().getAccount().getFunds() / 2) /
                    getAgent().getValuation(auction));
        } else if (signal == PlaceOrder.SELL){
            quantity = Math.min(getAgent().getStock(), 5);
        } else
            quantity = 1;
    }

    private PlaceOrder inMarketSignal(){
        ArrayList<Order> matchedShouts =
                new ArrayList<Order>(equilibriumReportVariables.getMatchedShouts());
        double[] closePrice = new double[matchedShouts.size()];

        double[] macd= new double[matchedShouts.size()];
        double[] macdSignal= new double[matchedShouts.size()];
        double[] macdHist= new double[matchedShouts.size()];

        MInteger signalSMABegin = new MInteger();
        MInteger signalSMALength = new MInteger();

        for (int i = 0; i < matchedShouts.size(); i++) {
            closePrice[i] = matchedShouts.get(i).getPrice();
        }

        Core core = new Core();
        RetCode retCode = core.macd(0, closePrice.length - 1,
                closePrice, QUICK_PERIODS_AVERAGE, SLOW_PERIODS_AVERAGE,
                SIGNAL_PERIOD_AVERAGE, signalSMABegin, signalSMALength,
                macd, macdSignal, macdHist);

        if (retCode == RetCode.Success){
            double lastMatchedPrice = macdHist[closePrice.length - signalSMABegin.value - 1];
            double previousMatchedPrice = macdHist[closePrice.length - signalSMALength.value - 2 ];
            if (lastMatchedPrice > 0 && previousMatchedPrice <= 0){
                isBuy = true;
                return PlaceOrder.BUY;
            } else if (lastMatchedPrice < 0 && previousMatchedPrice >= 0 && getAgent().getStock() > 0){
                isBuy = false;
                return PlaceOrder.SELL;
            } else
                return PlaceOrder.NOTHING;
        } else
            return PlaceOrder.NOTHING;
    }

    public boolean isBuy(Market market) {
        return isBuy;
    }

    public void setEquilibriumReportVariables(EquilibriumReportVariables variables){
        this.equilibriumReportVariables = variables;
    }
}