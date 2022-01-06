
import java.lang.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.concurrent.*;
import com.zaxxer.hikari.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class mainDriver {

	public static void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		if (args.length < 1) {
			System.out.println("param1: the path of input file");
			System.exit(1);
		}
		
		boolean debug = false;
		boolean analyze = false;

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

			// set up statistics
			int totalXactExecuted = 0, totalXactSucceeded = 0;
			int count_no = 0, count_pay = 0, count_de = 0, count_os = 0, 
				count_sl = 0, count_pi = 0, count_tb = 0, count_rc = 0;
			int count_suc_no = 0, count_suc_pay = 0, count_suc_de = 0, count_suc_os = 0,
				count_suc_sl = 0, count_suc_pi = 0, count_suc_tb = 0, count_suc_rc = 0;
			long allStartTime = System.currentTimeMillis();
			DescriptiveStatistics stat_all = new DescriptiveStatistics();
			DescriptiveStatistics stat_no = new DescriptiveStatistics();
			DescriptiveStatistics stat_pay = new DescriptiveStatistics();
			DescriptiveStatistics stat_de = new DescriptiveStatistics();
			DescriptiveStatistics stat_os = new DescriptiveStatistics();
			DescriptiveStatistics stat_sl = new DescriptiveStatistics();
			DescriptiveStatistics stat_pi = new DescriptiveStatistics();
			DescriptiveStatistics stat_tb = new DescriptiveStatistics();
			DescriptiveStatistics stat_rc = new DescriptiveStatistics();

			// set up file reader
			File file = new File(args[0]);
			Scanner reader = new Scanner(file);

			while (reader.hasNextLine()) {
				if (debug && totalXactExecuted >= 1000) break;

				String str = reader.nextLine();
				if (str.length() == 0) {
					continue;
				}
				String[] values = str.split(",");

				switch (values[0].charAt(0)) {
					case 'N': {
						totalXactExecuted += 1;
						count_no += 1;
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

						if (newOrderXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_no += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[New Order Transaction]", timeInMillis);
						stat_no.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'P': {
						totalXactExecuted += 1;
						count_pay += 1;
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);
						double PAYMENT = Double.parseDouble(values[4]);

						XactHandler paymentXactHandler = new PaymentXactHandler(
							conn, C_W_ID, C_D_ID, C_ID, PAYMENT);
						if (paymentXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_pay += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Payment Transaction]", timeInMillis);
						stat_pay.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'D': {
						totalXactExecuted += 1;
						count_de += 1;
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int CARRIER_ID = Integer.parseInt(values[2]);

						XactHandler deliveryXactHandler = new DeliveryXactHandler(
							conn, W_ID, CARRIER_ID);
						if (deliveryXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_de += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Delivery Transaction]", timeInMillis);
						stat_de.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'O': {
						totalXactExecuted += 1;
						count_os += 1;
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);

						XactHandler orderStatusXactHandler = new OrderStatusXactHandler(
							conn, C_W_ID, C_D_ID, C_ID);
						if (orderStatusXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_os += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Order Status Transaction]", timeInMillis);
						stat_os.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'S': {
						totalXactExecuted += 1;
						count_sl += 1;
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int D_ID = Integer.parseInt(values[2]);
						int T = Integer.parseInt(values[3]);
						int L = Integer.parseInt(values[4]);

						XactHandler stockLevelXactHandler = new StockLevelXactHandler(
							conn, W_ID, D_ID, T, L);
						if (stockLevelXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_sl += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Stock Level Transaction]", timeInMillis);
						stat_sl.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'I': {
						totalXactExecuted += 1;
						count_pi += 1;
						long startTime = System.currentTimeMillis();

						int W_ID = Integer.parseInt(values[1]);
						int D_ID = Integer.parseInt(values[2]);
						int L = Integer.parseInt(values[3]);

						XactHandler popularItemXactHandler = new PopularItemXactHandler(
							conn, W_ID, D_ID, L);
						if (popularItemXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_pi += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Popular Item Transaction]", timeInMillis);
						stat_pi.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'T': {
						totalXactExecuted += 1;
						count_tb += 1;
						long startTime = System.currentTimeMillis();

						XactHandler topBalanceXactHandler = new TopBalanceXactHandler(conn);
						if (topBalanceXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_tb += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Top Balance Transaction]", timeInMillis);
						stat_tb.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					case 'R': {
						totalXactExecuted += 1;
						count_rc += 1;
						long startTime = System.currentTimeMillis();

						int C_W_ID = Integer.parseInt(values[1]);
						int C_D_ID = Integer.parseInt(values[2]);
						int C_ID = Integer.parseInt(values[3]);

						XactHandler relatedCustomerXactHandler = new RelatedCustomerXactHandler(
							conn, C_W_ID, C_D_ID, C_ID);
						if (relatedCustomerXactHandler.execute()) {
							totalXactSucceeded += 1;
							count_suc_rc += 1;
						}

						long timeInMillis = System.currentTimeMillis() - startTime;
						if (analyze) printTimeInfo("[Related Customer Transaction]", timeInMillis);
						stat_rc.addValue(timeInMillis);
						stat_all.addValue(timeInMillis);
						break;
					}
					default: {
					}

				}

				if (totalXactExecuted % 500 == 250) {
					// refresh connection
					conn.close();
					conn = ds.getConnection();
				}

				if (totalXactExecuted > 0 && totalXactExecuted % 500 == 0) {
					System.err.printf("TotalXactExecuted: %d\n", totalXactExecuted);
				}
			}

			long allEndTime = System.currentTimeMillis();
			double totalTime = (allEndTime - allStartTime) / 1000.0;
			double throughput = totalXactExecuted / totalTime;

			// output statistics
			System.err.printf("Total number of transactions succeeded/executed: %d/%d\n", totalXactSucceeded, totalXactExecuted);
			System.err.printf("Total execution time: %.2f s\n", totalTime);
			System.err.printf("Transaction throughput: %.2f xact/s\n", throughput);

			System.err.printf("Average transaction latency: %.2f ms\n", stat_all.getMean());
			System.err.printf("Median transaction latency: %.2f ms\n", stat_all.getPercentile(50));
			System.err.printf("95th percentile transaction latency: %.2f ms\n", stat_all.getPercentile(95));
			System.err.printf("99th percentile transaction latency: %.2f ms\n", stat_all.getPercentile(99));
			System.err.println("");
			System.err.printf("[New Order Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_no, count_no, stat_no.getMin(), stat_no.getMean(), stat_no.getPercentile(50), stat_no.getMax());
			System.err.printf("[Payment Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_pay, count_pay, stat_pay.getMin(), stat_pay.getMean(), stat_pay.getPercentile(50), stat_pay.getMax());
			System.err.printf("[Delivery Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_de, count_de, stat_de.getMin(), stat_de.getMean(), stat_de.getPercentile(50), stat_de.getMax());
			System.err.printf("[Order Status Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_os, count_os, stat_os.getMin(), stat_os.getMean(), stat_os.getPercentile(50), stat_os.getMax());
			System.err.printf("[Stock Level Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_sl, count_sl, stat_sl.getMin(), stat_sl.getMean(), stat_sl.getPercentile(50), stat_sl.getMax());
			System.err.printf("[Popular Item Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_pi, count_pi, stat_pi.getMin(), stat_pi.getMean(), stat_pi.getPercentile(50), stat_pi.getMax());
			System.err.printf("[Top Balance Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_tb, count_tb, stat_tb.getMin(), stat_tb.getMean(), stat_tb.getPercentile(50), stat_tb.getMax());
			System.err.printf("[Related Customer Transaction] succeeded/executed: %d/%d, min: %.2f ms, mean: %.2f ms, median: %.2f ms, max: %.2f ms\n",
				count_suc_rc, count_rc, stat_rc.getMin(), stat_rc.getMean(), stat_rc.getPercentile(50), stat_rc.getMax());

		} catch (Exception e) {
			System.err.println(e);
		}


	}

}
