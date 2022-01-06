
import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class Test_XactRetry extends XactHandler {

	public Test_XactRetry(Connection conn) {
		super("Test_XactRetry", conn);
	}

	@Override
	void process() throws SQLException {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {

		}
		throw new SQLException("Test_retry SQLException occurs", "40001");

	}

	public static void main(String[] args) {

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

			XactHandler testRetryXactHandler = new Test_XactRetry(conn);
			//for (int i = 0; i < 10; ++i) {
				//System.out.printf("[Test_Retry XactHandler] retry %d...\n", i + 1);
				testRetryXactHandler.execute();
			//}


		} catch (Exception e) {
			System.err.println(e);
		}


	}

}
