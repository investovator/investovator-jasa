/*
 * investovator, Stock Market Gaming framework
 * Copyright (C) 2013  investovator
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

package org.investovator.agentsimulation.api;

import net.sourceforge.jabm.event.EventListener;
import net.sourceforge.jabm.report.Report;
import net.sourceforge.jasa.market.Account;
import net.sourceforge.jasa.market.MarketSimulation;
import net.sourceforge.jasa.market.Order;
import org.investovator.agentsimulation.api.utils.HollowTradingAgent;
import org.investovator.agentsimulation.api.utils.HumanAgent;
import org.investovator.agentsimulation.multiasset.simulation.HeadlessMultiAssetSimulationManager;
import org.investovator.core.commons.simulationengine.MarketOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author rajith
 * @version $Revision$
 */
public class JASAFacade implements MarketFacade {

    private static volatile JASAFacade jasaFacade;

    private HeadlessMultiAssetSimulationManager manager;
    private HashMap<String, HumanAgent> humanPlayers;

    public static JASAFacade getMarketFacade() {
        if(jasaFacade == null){
            synchronized(JASAFacade.class){
                if(jasaFacade == null)
                    jasaFacade = new JASAFacade();
            }
        }
        return jasaFacade;
    }

    private JASAFacade() {
        manager = new HeadlessMultiAssetSimulationManager();
        humanPlayers = new HashMap<String, HumanAgent>();
    }

    @Override
    public boolean startSimulation() {
        new Thread(manager).start();
        return true;
    }

    @Override
    public boolean pauseSimulation() {
        manager.pause();
        return true;
    }

    @Override
    public boolean terminateSimulation() {
        manager.terminate();
        return true;
    }

    @Override
    public boolean resumeSimulation() {
        manager.resume();
        return true;
    }

    /**
     * @param username username
     * @param initFunds initial account balance
     */
    @Override
    public void AddUserAgent(String username, double initFunds) {
        humanPlayers.put(username,  new HumanAgent(username, initFunds));
    }

    public HashMap<String, ArrayList<Report>> getReports() {
        return manager.getReports();
    }

    @Override
    public void addListener(String stockID, EventListener eventListener) {
        manager.getController(stockID).addListener(eventListener);
    }

    /**
     * @param username  corresponding username
     * @param stockId  security id
     * @param quantity stock quantity
     * @param isBuy    buy = true, sell=false;
     * @param price    single stock price
     * @return adding order successful
     */
    @Override
    public boolean putLimitOrder(String username, String stockId, int quantity,
                                 double price, boolean isBuy) {

        HumanAgent humanAgent = humanPlayers.get(username);

        if(!humanAgent.isHollowAgentAvailable(stockId)){
            humanAgent.addHollowAgentToStock(stockId);
        }

        if(isBuy && buyOrderApproved(humanAgent.getAccount(), quantity, price)){
            //create the order
            putOrder(stockId, quantity, price, isBuy, humanAgent);
            return true;
        } else if(!isBuy && sellOrderApproved(humanAgent.getHollowTradingAgent(stockId), quantity)){
            putOrder(stockId, quantity, price, isBuy, humanAgent);
            return true;
        } else
            return false;
    }

    /**
     * @param username  corresponding username
     * @param stockId  security id
     * @param quantity stock quantity
     * @param isBuy    buy = true, sell=false;
     * @return adding order successful
     */
    @Override
    public boolean putMarketOrder(String username, String stockId, int quantity,
                                  boolean isBuy) {
        return false;  //TODO adding a Market order
    }

    @Override
    public HashMap<String, Integer> getUserAgentAssets(String username) {
        return humanPlayers.get(username).getAssets();
    }

    @Override
    public double getUserAgentFunds(String username) {
        return humanPlayers.get(username).getAccount().getFunds();
    }

    @Override
    public HashMap<String, ArrayList<MarketOrder>> getUserUnmatchedOrders(String username) {
        HashMap<String, MarketSimulation> stocks = (manager.getExchange()).getStocks();

        HashMap<String, ArrayList<MarketOrder>> unmatchedOrders = new HashMap<>();
        for(String stockId : stocks.keySet()){
            ArrayList<MarketOrder> ordersForId = new ArrayList<>();

            List<Order> unmatchedAsksList = ((stocks.get(stockId)).getAuctioneer()).getUnmatchedAsks();
            getUserOrdersFromList(username, ordersForId, unmatchedAsksList);

            List<Order> unmatchedBidsList = ((stocks.get(stockId)).getAuctioneer()).getUnmatchedBids();
            getUserOrdersFromList(username, ordersForId, unmatchedBidsList);

            unmatchedOrders.put(stockId, ordersForId);
        }

        return unmatchedOrders;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void getUserOrdersFromList(String username, ArrayList<MarketOrder> orders,
                                                         List<Order> unmatchedList) {
        for (Order order : unmatchedList){
            if((order.getAgent()) instanceof HollowTradingAgent){
                HollowTradingAgent agent = (HollowTradingAgent) order.getAgent();
                if(agent.getUserName().equals(username)){
                    orders.add(order);
                }

            }
        }
    }

    private void putOrder(String stockId, int quantity, double price,
                          boolean isBuy, HumanAgent humanAgent) {
        Order order = new Order(humanAgent.getHollowTradingAgent(stockId), quantity,
                price, isBuy);
        manager.getExchange().placeOrder(stockId, order);
    }

    private boolean buyOrderApproved(Account account, int quantity, double price){
        return account.getFunds() >= (price * quantity);
    }

    private boolean sellOrderApproved(HollowTradingAgent agent, int quantity){
        return agent.getStock() >= quantity;
    }

    public long getSimulationTime(String stockID){
        return  manager.getController(stockID).getSimulationTime().getTicks();
    }
}
