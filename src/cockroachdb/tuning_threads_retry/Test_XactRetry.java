
import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class Test_XactRetry {

	public SqlExecutor executor;

	public Test_XactRetry(Connection conn) {
		this.executor = new SqlExecutor("Test_XactRetry", conn);
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

			Test_XactRetry testRetryXactHandler = new Test_XactRetry(conn);
			ResultSet res = testRetryXactHandler.executor.execute("SELECT * from district limit 10");
			while (res.next()) {
				int d_w_id = res.getInt("d_w_id");
				int d_id = res.getInt("d_id");
				System.out.printf("%d\t%d\n", d_w_id, d_id);
			}

		} catch (Exception e) {
			System.err.println(e);
		}


	}

}
