package ca.ubc.cs304.delegates;

import ca.ubc.cs304.model.ReservationModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;


public interface ReservationDelegate {


    public DefaultTableModel joinMailsofCustomersMoreThanOneWeek();
    public DefaultTableModel nestedAggregationInvoice();
    public DefaultTableModel aggregateMostExpensiveInvoice();
    public DefaultTableModel divisionCustomersUsingAllServices();


    public void deleteReservation(int reservationID);
    public DefaultTableModel projectionReservation(String columnName);
    public DefaultTableModel getDefaultTable(String query, String tableName);

    public void insertReservation(ReservationModel reservationModel);

    public ArrayList<String> getColumnList(String columnName);
    public ArrayList<String> getIDListFrom(String tableName);

    public void updateHotel(int id, String type, String name);
    public DefaultTableModel selectionInvoice(int value, String operator, String columnName);

    public void databaseSetup();

    public void reservationFinished();
}

