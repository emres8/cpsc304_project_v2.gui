package ca.ubc.cs304.controller;

import ca.ubc.cs304.database.DatabaseConnectionHandler;
import ca.ubc.cs304.delegates.LoginWindowDelegate;
import ca.ubc.cs304.delegates.ReservationDelegate;

import ca.ubc.cs304.model.InvoiceModel;
import ca.ubc.cs304.model.ReservationModel;
import ca.ubc.cs304.ui.LoginWindow;


import ca.ubc.cs304.ui.UI;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;


public class HotelManagement  implements LoginWindowDelegate, ReservationDelegate {

    private DatabaseConnectionHandler dbHandler = null;
    private LoginWindow loginWindow = null;

    public HotelManagement() {
        dbHandler = new DatabaseConnectionHandler();
    }

    private void start() {
        loginWindow = new LoginWindow();
        loginWindow.showFrame(this);
    }


    @Override
    public void login(String username, String password) {
        boolean didConnect = dbHandler.login(username, password);

        if (didConnect) {
            // Once connected, remove login window and start text transaction flow
            loginWindow.dispose();

            //TerminalTransactions transaction = new TerminalTransactions();
           UI mainUI = new UI(this);


            //transaction.showMainMenu(this);
        } else {
            loginWindow.handleLoginFailed();

            if (loginWindow.hasReachedMaxLoginAttempts()) {
                loginWindow.dispose();
                System.out.println("You have exceeded your number of allowed attempts");
                System.exit(-1);
            }
        }
    }

    @Override
    public void reservationFinished() {
        dbHandler.close();
        dbHandler = null;
        System.exit(0);
    }

    @Override
    public void updateHotel(int id, String type, String name) {
        dbHandler.updateHotel(id, type, name);
    }

    @Override
    public DefaultTableModel joinMailsofCustomersMoreThanOneWeek() {
        return dbHandler.joinMailsofCustomersMoreThanOneWeek();
    }

    @Override
    public DefaultTableModel aggregateMostExpensiveInvoice() {
        return dbHandler.aggregateMostExpensiveInvoice();
    }


    @Override
    public DefaultTableModel nestedAggregationInvoice() {return dbHandler.nestedAggregateInvoice();}

    @Override
    public void deleteReservation(int reservationID) {
        dbHandler.deleteReservation(reservationID);
    }

    @Override
    public void insertReservation(ReservationModel reservationModel) {
        dbHandler.insertReservation(reservationModel);
    }

    @Override
    public ArrayList<String> getColumnList(String columnName) {
        return dbHandler.getColumnList(columnName);
    }

    @Override
    public ArrayList<String> getIDListFrom(String tableName) {
        return dbHandler.getIDListFrom(tableName);
    }

    @Override
    public DefaultTableModel selectionInvoice(int value, String operator, String columnName) {
        return dbHandler.selectionInvoice(value, operator, columnName);
    }

    @Override
    public void databaseSetup() {
        dbHandler.databaseSetup();
    }

    @Override
    public DefaultTableModel projectionReservation(String columnName) {
        return dbHandler.projectionReservation(columnName);
    }

    @Override
    public DefaultTableModel divisionCustomersUsingAllServices() {
        return dbHandler.divisionCustomersUsingAllServices();
    }

    @Override
    public DefaultTableModel getDefaultTable(String query, String tableName){
        return dbHandler.getDefaultTableModel(query,tableName);
    }


    public static void main(String[] args) {
        HotelManagement hotelManagement = new HotelManagement();
        hotelManagement.start();
    }
}
