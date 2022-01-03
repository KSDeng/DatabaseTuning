
import java.lang.*;
import java.util.Random;
import java.sql.*;
import com.zaxxer.hikari.*;

public class SqlExecutor {
	
	private static final int MAX_RETRY_COUNT = 5;
	private static final String RETRY_SQL_STATE = "40001";
	private final Random rand = new Random();

	private Connection conn;
	private String xactName;

	public boolean suc;

	public SqlExecutor(String name, Connection c) {
		this.xactName = name;
		this.conn = c;

		this.suc = false;
	}

	// sqlCommand: command to execute
	// query: true means query, false means update
	public ResultSet execute(String sqlCommand) {
		int retryCount = 0;
		try {
			this.conn.setAutoCommit(false);
			this.conn.createStatement().execute("BEGIN");
			while (retryCount <= this.MAX_RETRY_COUNT) {
				if (retryCount == this.MAX_RETRY_COUNT) {
					System.err.printf("[%s] Hit max of %d retries, aborting\n",this.xactName, retryCount);
					break;
				}
				if (retryCount > 0) {
					System.out.printf("[%s] Retry %d...\n", this.xactName, retryCount);
				}

				try  {
					Statement stmt = this.conn.createStatement();
					stmt.execute(sqlCommand);
					//this.conn.commit();			// execution succeeded
					this.conn.createStatement().execute("COMMIT");
					this.suc = true;

					return stmt.getResultSet();
				} catch (SQLException e) {
					//if (RETRY_SQL_STATE.equals(e.getSQLState())) {
					// Retry 40001 error
					// https://www.cockroachlabs.com/docs/stable/error-handling-and-troubleshooting.html
					this.conn.rollback();
					retryCount++;
					int sleepMillis = (int)(Math.pow(2, retryCount) * 100) + rand.nextInt(100);
					System.out.printf("[%s] Hit %d error, SQL state: %s, message: %s, sleeping %d milliseconds\n", this.xactName, e.getErrorCode(), e.getSQLState(), e.getMessage(), sleepMillis);
					try {
						Thread.sleep(sleepMillis);
					} catch (InterruptedException ignored) {}
				}
			}


		} catch (SQLException e) {
			System.out.println("[SqlExecutor]" + e);
		}

		this.suc = false;	// execution failed
		return null;
	}

}
