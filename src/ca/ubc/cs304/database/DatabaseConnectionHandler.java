package ca.ubc.cs304.database;
import ca.ubc.cs304.model.*;
import ca.ubc.cs304.util.PrintablePreparedStatement;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * This class handles all database related transactions
 */
public class DatabaseConnectionHandler {
	// Use this version of the ORACLE_URL if you are running the code off of the server
//	private static final String ORACLE_URL = "jdbc:oracle:thin:@dbhost.students.cs.ubc.ca:1522:stu";
	// Use this version of the ORACLE_URL if you are tunneling into the undergrad servers
	private static final String ORACLE_URL = "jdbc:oracle:thin:@localhost:1522:stu";
	private static final String EXCEPTION_TAG = "[EXCEPTION]";
	private static final String WARNING_TAG = "[WARNING]";

	private Connection connection = null;

	public DatabaseConnectionHandler() {
		try {
			// Load the Oracle JDBC driver
			// Note that the path could change for new drivers
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
		}
	}

	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
		}
	}


	public void insertReservation(ReservationModel model) {
		try {
			String query = "INSERT INTO Reservation VALUES (?,TO_DATE(?,'YYYY-MM-DD'),TO_DATE(?,'YYYY-MM-DD'),TO_DATE(?,'YYYY-MM-DD'),?,?,?,?,?,?)";
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ps.setInt(1, model.getReservationID());
			ps.setString(2, model.getReservationDate());
			ps.setString(3, model.getCheckInDate());
			ps.setString(4, model.getCheckOutDate());
			if (model.getRoomNo() == 0) {
				ps.setNull(5, java.sql.Types.INTEGER);
			} else {
				ps.setInt(5, model.getRoomNo());
			}
			ps.setInt(6, model.getCustomerID());
			ps.setInt(7, model.getHotelID());
			ps.setInt(8, model.getInvoiceNumber());
			if (model.getEventID() == 0) {
				ps.setNull(9, java.sql.Types.INTEGER);
			} else {
				ps.setInt(9, model.getEventID());
			}
			if (model.getFacilityID() == 0) {
				ps.setNull(10, java.sql.Types.INTEGER);
			} else {
				ps.setInt(10, model.getFacilityID());
			}

			ps.executeUpdate();
			connection.commit();

			ps.close();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
			rollbackConnection();
		}
	}

	public void deleteReservation(int reservationID) {
		try {
			String query = "DELETE FROM RESERVATION WHERE RESERVATIONID = ?";
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ps.setInt(1, reservationID);

			int rowCount = ps.executeUpdate();
			if (rowCount == 0) {
				System.out.println(WARNING_TAG + " Reservation with ID " + reservationID + " does not exist!");
			}
			connection.commit();

			ps.close();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
			rollbackConnection();
		}
	}

	public DefaultTableModel aggregateMostExpensiveInvoice() {
		String query = "SELECT * FROM INVOICE WHERE INVOICEAMOUNT = (SELECT  MAX(INVOICEAMOUNT) FROM  INVOICE)";
		return getDefaultTableModel(query, "");
	}

	// maximum average invoice comes from a particular customer
	public DefaultTableModel nestedAggregateInvoice() {
		String query = "SELECT MAX(avg) " +
				"FROM " +
				"(SELECT AVG(INVOICEAMOUNT) AS avg FROM INVOICE GROUP BY CUSTOMERID)";
		return getDefaultTableModel(query, "");
	}


	public DefaultTableModel divisionCustomersUsingAllServices() {
		String query = "SELECT c.CUSTOMERID FROM CUSTOMER c WHERE NOT EXISTS((SELECT s.serviceID FROM HOTELSERVICES s) MINUS (SELECT a.serviceID FROM INVOICE a WHERE a.CUSTOMERID = c.CUSTOMERID and a.SERVICEID IS NOT NULL))";
		return getDefaultTableModel(query, "");
	}


	public DefaultTableModel joinMailsofCustomersMoreThanOneWeek() {
		String query = "SELECT DISTINCT s.CUSTOMEREMAIL  " +
				"FROM  CUSTOMER s, RESERVATION r " +
				"WHERE (r.CHECKOUTDATE-r.CHECKINDATE) > 7  	" +
				"AND s.CUSTOMERID =r.CUSTOMERID";
		return getDefaultTableModel(query, "");
	}

	public DefaultTableModel selectionInvoice(int value, String operator, String columnName) {
		// operator: (bigger, less, equal)
		String query;
		if (operator.equals("less")) {
			query = "SELECT * FROM INVOICE WHERE " + columnName + " < " + value;
		} else if (operator.equals("bigger")) {
			query = "SELECT * FROM INVOICE WHERE " + columnName + " > " + value;
		} else
			query = "SELECT * FROM INVOICE WHERE " + columnName + " = " + value;
		return getDefaultTableModel(query, "");
	}

	public DefaultTableModel projectionReservation(String columnName) {
		String query = "SELECT " + columnName + " FROM RESERVATION";
		return getDefaultTableModel(query, "");
	}



	public void updateHotel(int id, String hotelType, String hotelName) {
		try {
			String query = "UPDATE HOTEL SET HOTELTYPE = \'" + hotelType + "\',HOTELNAME = \'" + hotelName + "\' WHERE HOTELID = " + id;
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
			rollbackConnection();
		}
	}

	public ArrayList<String> getIDListFrom(String tableName) {
		ArrayList<String> result = new ArrayList<String>();
		try {
			String query = "SELECT * FROM " + tableName;
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String idColumnName = tableName + "ID";
				if(tableName.equalsIgnoreCase("rooms")){
					idColumnName = "ROOMNO";
				}
				if(tableName.equalsIgnoreCase("invoice")){
					idColumnName = tableName + "NUMBER";
				}
				if(tableName.equalsIgnoreCase("events")){
					idColumnName = "EVENTID";
				}
				result.add(rs.getString(idColumnName));
			}
			ps.close();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
			rollbackConnection();
		}
		return result;
	}



	public ArrayList<String> getColumnList(String columnName) {
		ArrayList<String> columnNames = new ArrayList<>();
		try {
			String query = "SELECT * FROM " + columnName;
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			for (int i = 1; i <= columnsNumber; i++) {
				columnNames.add(rsmd.getColumnName(i));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return columnNames;
	}


	public boolean login(String username, String password) {
		try {
			if (connection != null) {
				connection.close();
			}

			connection = DriverManager.getConnection(ORACLE_URL, username, password);
			connection.setAutoCommit(false);

			System.out.println("\nConnected to Oracle!");
			return true;
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
			return false;
		}
	}

	private void rollbackConnection() {
		try {
			connection.rollback();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
		}
	}

	public DefaultTableModel getDefaultTableModel(String query, String tableName) {
		DefaultTableModel model = new DefaultTableModel();
		try {


			String querytoExecute = query + " " + tableName;
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(querytoExecute), querytoExecute, false);

			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();

			int columnsNumber = rsmd.getColumnCount();


			Vector<String> columnNames = new Vector<String>();
			for (int i = 1; i <= columnsNumber; i++) {
				columnNames.add(rsmd.getColumnName(i));
			}

			Vector<Vector<Object>> data = new Vector<Vector<Object>>();
			while (rs.next()) {
				Vector<Object> row = new Vector<Object>();

				for (int i = 1; i <= columnsNumber; i++) {
					row.add(rs.getObject(i));
				}

				data.add(row);
			}
			model.setDataVector(data, columnNames);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return model;
	}

	private void executeHelper(String query) throws SQLException {
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();
	}

	public void reservationSetUp() throws SQLException {
		String query = "CREATE TABLE Reservation (reservationID INTEGER NOT NULL, reservationDate DATE NOT NULL, checkInDate DATE NOT NULL, checkOutDate DATE NOT NULL, roomNo INTEGER, customerID INTEGER NOT NULL, hotelID INTEGER NOT NULL, invoiceNumber INTEGER NOT NULL, eventID INTEGER, facilityID INTEGER, PRIMARY KEY(reservationID),UNIQUE (customerID, hotelID, invoiceNumber), FOREIGN KEY (customerID) REFERENCES Customer, FOREIGN KEY (hotelID) REFERENCES Hotel, FOREIGN KEY (invoiceNumber) REFERENCES Invoice, FOREIGN KEY (eventID) REFERENCES Events, FOREIGN KEY (facilityID) REFERENCES Facility,FOREIGN KEY (roomNo) REFERENCES Rooms)";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Reservation VALUES (111111111, TO_DATE('2020-10-20','YYYY-MM-DD'), TO_DATE('2020-11-19','YYYY-MM-DD'), TO_DATE('2020-11-20','YYYY-MM-DD'),300,73648,13,92847563, null,null)");
		executeHelper("INSERT INTO Reservation VALUES (222222222, TO_DATE('2020-02-10','YYYY-MM-DD'), TO_DATE('2020-03-11','YYYY-MM-DD'), TO_DATE('2020-03-14','YYYY-MM-DD'),100,92837,13,10293846, 23453,50)");
		executeHelper("INSERT INTO Reservation VALUES (333333333, TO_DATE('2020-03-15','YYYY-MM-DD'), TO_DATE('2020-03-16','YYYY-MM-DD'), TO_DATE('2020-03-25','YYYY-MM-DD'),123,46352,24,38273957,null, null)");
		executeHelper("INSERT INTO Reservation VALUES (444444444, TO_DATE('2020-01-20','YYYY-MM-DD'), TO_DATE('2020-02-21','YYYY-MM-DD'), TO_DATE('2020-03-01','YYYY-MM-DD'),200,85769,12,38264061,null,null )");
		executeHelper("INSERT INTO Reservation VALUES (555555555, TO_DATE('2020-08-01','YYYY-MM-DD'), TO_DATE('2020-09-02','YYYY-MM-DD'), TO_DATE('2020-09-02','YYYY-MM-DD'),150,36252,22,27364951,null,null )");

	}

	public void invoiceSetUp() throws SQLException {
		String query = "CREATE TABLE Invoice (invoiceNumber INT NOT NULL, invoiceDate DATE, customerID INT NOT NULL, hotelID INT NOT NULL, invoiceAmount DEC NOT NULL, serviceID INT, facilityID INT, paymentNumber INT NOT NULL, PRIMARY KEY (invoiceNumber), FOREIGN KEY (customerID) REFERENCES Customer, FOREIGN KEY (hotelID) REFERENCES Hotel, FOREIGN KEY (serviceID) REFERENCES HotelServices, FOREIGN KEY (facilityID) REFERENCES Facility, FOREIGN KEY (paymentNumber) REFERENCES Payment)";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Invoice VALUES (92847563,TO_DATE('2020-10-21','YYYY-MM-DD'),73648,13,3049.00,2938,null,192837)");
		executeHelper("INSERT INTO Invoice VALUES (10293846,TO_DATE('2020-02-11','YYYY-MM-DD'),92837,13,39473.00,3948,null,394856)");
		executeHelper("INSERT INTO Invoice VALUES (38273957,TO_DATE('2020-03-16','YYYY-MM-DD'),46352,24,3748.00,null,50,485739)");
		executeHelper("INSERT INTO Invoice VALUES (38264061,TO_DATE('2020-01-21','YYYY-MM-DD'),85769,12,2937.00,null,null,384759)");
		executeHelper("INSERT INTO Invoice VALUES (27364951,TO_DATE('2020-08-02','YYYY-MM-DD'),36252,22,3847.00,null,null,183754)");
		executeHelper("INSERT INTO Invoice VALUES (87384958,TO_DATE('2020-08-03','YYYY-MM-DD'),73648,22,4000.00,3948,null,488758)");
		executeHelper("INSERT INTO Invoice VALUES (40543821,TO_DATE('2020-08-21','YYYY-MM-DD'),92837,13,473.00,2938,null,224145)");

	}

	public void customerSetUp() throws SQLException {
		String query = "CREATE TABLE Customer (customerID INT NOT NULL,customerName CHAR(20) NOT NULL,customerPhone INT NOT NULL,customerEmail CHAR(20) NOT NULL,PRIMARY KEY (customerID),UNIQUE (customerEmail) )";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Customer VALUES (73648,'Shania Twain',9283756504,'shania@gmail.com')");
		executeHelper("INSERT INTO Customer VALUES (92837,'Justin Bieber',8372915485,'jb@gmail.com')");
		executeHelper("INSERT INTO Customer VALUES (46352,'Prince Philips',2735485960,'pp@gmail.com')");
		executeHelper("INSERT INTO Customer VALUES (85769,'Elisa Liu',1246584910,'el@gmail.com')");
		executeHelper("INSERT INTO Customer VALUES (36252,'Brad Pitt',4193520673,'bp@gmail.com')");
	}

	public void hotelServicesSetUp() throws SQLException {
		String query = "CREATE TABLE HotelServices (serviceID INT  NOT NULL,serviceName CHAR(20),serviceType CHAR(20),servicePrice DEC,PRIMARY KEY (serviceID))";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO HotelServices VALUES (2938, 'Room Service', 'Room', 100.00)");
		executeHelper("INSERT INTO HotelServices VALUES (3948, 'Massage', 'Spa', 10.00)");
	}

	public void roomsSetUp() throws SQLException {
		String query = "CREATE TABLE Rooms (roomNo INT NOT NULL, floorNo INT, roomType CHAR(20), numberOfBeds INT, hotelID INT NOT NULL, typeOfView   CHAR(20), roomPrice    INT NOT NULL, typeOfBed    CHAR(20), PRIMARY KEY (roomNo), FOREIGN KEY (hotelID) REFERENCES Hotel)";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Rooms VALUES (300,3, 'Suite',null,13,'Ocean',20.00,null)");
		executeHelper("INSERT INTO Rooms VALUES (100,1, 'Multiple',3,13,null,1020.00,null )");
		executeHelper("INSERT INTO Rooms VALUES (123,1, 'Single',null,24,null,100.00,'Queen')");
		executeHelper("INSERT INTO Rooms VALUES (200,2, 'Suite',null,12,'Garden',1.00,null)");
		executeHelper("INSERT INTO Rooms VALUES (150,1, 'Suite',null,22,'Ocean',150.00,null  )");


	}

	public void eventSetUp() throws SQLException {
		String query = "CREATE TABLE Events (eventID INT NOT NULL, numberofGuests INT NOT NULL, eventType CHAR(20), eventQuota DEC, dateAvailable DATE, PRIMARY KEY (eventID))";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Events VALUES (23453,300,'Wedding', 5010.00, TO_DATE('2020-03-11','YYYY-MM-DD'))");
	}

	public void paymentSetUp() throws SQLException {
		String query = "CREATE TABLE Payment (paymentNumber INT NOT NULL, paymentAmount DEC NOT NULL, paymentMethod CHAR(20) NOT NULL, paymentDate DATE, PRIMARY KEY (paymentNumber))";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Payment VALUES (192837,3049.00, 'Credit', TO_DATE('2020-10-21','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (394856,39473.00, 'Debit', TO_DATE('2020-02-11','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (485739,3748.00, 'Debit', TO_DATE('2020-03-16','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (384759,2937.00, 'Credit', TO_DATE('2020-01-21','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (183754,3847.00, 'Credit', TO_DATE('2020-08-02','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (488758,4000.00, 'Debit', TO_DATE('2020-08-03','YYYY-MM-DD'))");
		executeHelper("INSERT INTO Payment VALUES (224145,473.00, 'Debit', TO_DATE('2020-08-21','YYYY-MM-DD'))");

	}

	public void hotelSetUp() throws SQLException {
		String query = "CREATE TABLE Hotel ( hotelID INT NOT NULL, hotelType CHAR(20), hotelName CHAR(20) NOT NULL, PRIMARY KEY (hotelID))";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();


		executeHelper("INSERT INTO Hotel VALUES (13,'Resort','Resorteros')");
		executeHelper("INSERT INTO Hotel VALUES (24,'Airport','Airporteros')");
		executeHelper("INSERT INTO Hotel VALUES (12,'Motel','Moteleros')");
		executeHelper("INSERT INTO Hotel VALUES (22,'Chain','Chaineros')");
	}

	public void facilitySetUp() throws SQLException {
		String query = "CREATE TABLE Facility(facilityID INT NOT NULL, facilityType CHAR(20), facilityAmenities CHAR(20), facilityCapacity INT NOT NULL, dateAvailable DATE, PRIMARY KEY (facilityID))";
		PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
		ps.executeUpdate();
		ps.close();

		executeHelper("INSERT INTO Facility VALUES (50,'Aquatic Centre', 'Pool',500, TO_DATE('2020-03-11','YYYY-MM-DD'))");

	}

	private void dropBranchTableIfExists() {
		try {
			String query = "BEGIN EXECUTE IMMEDIATE 'DROP TABLE Reservation'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Invoice'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Customer'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE HotelServices'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Rooms'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Events'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Payment'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Hotel'; " +
					"EXECUTE IMMEDIATE 'DROP TABLE Facility'; " +
					"EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN " +
					"RAISE; " +
					"END IF; " +
					"END;";
			PrintablePreparedStatement ps = new PrintablePreparedStatement(connection.prepareStatement(query), query, false);
			ps.executeQuery();
			ps.close();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
		}
	}

	public void databaseSetup() {
		dropBranchTableIfExists();
		try {
			//Path file = Paths.get("src/ca/ubc/cs304","sql/scripts/databaseSetup.sql");
			//String query = Files.readString(file);
			facilitySetUp();
			hotelSetUp();
			paymentSetUp();
			eventSetUp();
			roomsSetUp();
			hotelServicesSetUp();
			customerSetUp();
			invoiceSetUp();
			reservationSetUp();
		} catch (SQLException e) {
			System.out.println(EXCEPTION_TAG + " " + e.getMessage());
		}
	}
}








