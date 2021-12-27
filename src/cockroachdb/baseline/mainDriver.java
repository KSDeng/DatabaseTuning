
import java.lang.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.concurrent.*;
import com.zaxxer.hikari.*;

public class mainDriver {

	public static void printTimeInfo(String name, double timeInNanos) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInNanos);
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
						long startTime = System.nanoTime();

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
						XactHandler handler = new NewOrderXactHandler(
							conn, W_ID, D_ID, C_ID, NUM_ITEMS, 
							OL_I_IDs, OL_SUPPLY_W_IDs, OL_QUANTITYs);

						handler.execute();

						long timeInNanos = System.nanoTime() - startTime;
						if (analyze) printTimeInfo("[New Order Transaction]", timeInNanos);
						break;
					}
					case 'P': {
						System.out.println("Payment Xact");
						break;
					}
					case 'D': {
						System.out.println("Delivery Xact");
						break;

					}
					case 'O': {
						System.out.println("OrderStatus Xact");
						break;
					}
					case 'S': {
						System.out.println("StockLevel Xact");
						break;
					}
					case 'I': {
						System.out.println("PopularItem Xact");
						break;
					}
					case 'T': {
						System.out.println("TopBalance Xact");
						break;
					}
					case 'R': {
						System.out.println("RelatedCustomer Xact");
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
