
import java.lang.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.concurrent.*;
import com.zaxxer.hikari.*;

public class mainDriver {

	public static void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		if (args.length < 1) {
			System.out.println("param1: the path of input file");
			System.exit(1);
		}

		boolean analyze = true;

		try {

			// set up connection
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:postgresql://0.0.0.0:26257/wholesaledata");
			config.setUsername("root");
			config.addDataSourceProperty("ssl", "false");
			config.addDataSourceProperty("sslmode", "disable");
			config.addDataSourceProperty("reWriteBatchedInserts", "true");
			config.setAutoCommit(false);
			config.setMaximumPoolSize(240);     // server has 24 cores
			config.setKeepaliveTime(150000);

			HikariDataSource ds = new HikariDataSource(config);
			Connection conn = ds.getConnection();

			// set up file reader
			File file = new File(args[0]);
			Scanner reader = new Scanner(file);

			while (reader.hasNextLine()) {
				String str = reader.nextLine();
				if (str.length() == 0) {
					continue;
				}
				String[] values = str.split(",");

				switch (values[0].charAt(0)) {
					case 'N': {
						long startTime = System.currentTimeMillis();

						int C_ID = Integer.parseInt(values[1]);
						int W_ID = Integer.parseInt(values[2]);
						int D_ID = Integer.parseInt(values[3]);
						int NUM_ITEMS = Integer.parseInt(values[4]);
						
						int[] OL_I_IDs = new int[NUM_ITEMS];
						int[] OL_SUPPLY_W_IDs = new int[NUM_ITEMS];
						int[] OL_QUANTITYs = new int[NUM_ITEMS];

						for (int i = 0; i < NUM_ITEMS; ++i) {
							str = reader.nextLine();
							values = str.split(",");

							OL_I_IDs[i] = Integer.parseInt(values[0]);
							OL_SUPPLY_W_IDs[i] = Integer.parseInt(values[1]);
							OL_QUANTITYs[i] = Integer.parseInt(values[2]);
						}
						XactHandler newOrderXactHandler = new NewOrderXactHandler(
							conn, W_ID, D_ID, C_ID, NUM_ITEMS, 
							OL_I_IDs, OL_SUPPLY_W_IDs, OL_QUANTITYs);

						newOrderXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[New Order Transaction]", timeInMillis);
						break;
					}
					case 'P': {
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);
						double PAYMENT = Double.parseDouble(values[4]);

						XactHandler paymentXactHandler = new PaymentXactHandler(
							conn, C_W_ID, C_D_ID, C_ID, PAYMENT);
						paymentXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Payment Transaction]", timeInMillis);
						break;
					}
					case 'D': {
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int CARRIER_ID = Integer.parseInt(values[2]);

						XactHandler deliveryXactHandler = new DeliveryXactHandler(
							conn, W_ID, CARRIER_ID);
						deliveryXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Delivery Transaction]", timeInMillis);

						break;
					}
					case 'O': {
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);

						XactHandler orderStatusXactHandler = new OrderStatusXactHandler(
							conn, C_W_ID, C_D_ID, C_ID);
						orderStatusXactHandler.execute();
						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Delivery Transaction]", timeInMillis);

						break;
					}
					case 'S': {
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int D_ID = Integer.parseInt(values[2]);
						int T = Integer.parseInt(values[3]);
						int L = Integer.parseInt(values[4]);

						XactHandler stockLevelXactHandler = new StockLevelXactHandler(
							conn, W_ID, D_ID, T, L);
						stockLevelXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Stock Level Transaction]", timeInMillis);

						break;
					}
					case 'I': {
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int D_ID = Integer.parseInt(values[2]);
						int L = Integer.parseInt(values[3]);

						XactHandler popularItemXactHandler = new PopularItemXactHandler(
							conn, W_ID, D_ID, L);
						popularItemXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Popular Item Transaction]", timeInMillis);

						break;
					}
					case 'T': {
						long startTime = System.currentTimeMillis();

						XactHandler topBalanceXactHandler = new TopBalanceXactHandler(conn);
						topBalanceXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Top Balance Transaction]", timeInMillis);
						
						break;
					}
					case 'R': {
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);

						XactHandler relatedCustomerXactHandler = new RelatedCustomerXactHandler(
							conn, C_W_ID, C_D_ID, C_ID);
						relatedCustomerXactHandler.execute();

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Related Customer Transaction]", timeInMillis);

						break;
					}
					default: {
					}

				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}


	}

}
