
import java.lang.*;
import java.util.Random;
import java.sql.*;
import com.zaxxer.hikari.*;

public abstract class XactHandler {

	private static final int MAX_RETRY_COUNT = 3;
	private static final String RETRY_SQL_STATE = "40001";
	private final Random rand = new Random();

	protected Connection conn;
	protected String xactName;

	abstract void process() throws SQLException;		// code to process the xact
	
	public XactHandler(String name, Connection c) {
		this.xactName = name;
		this.conn = c;
	}

	protected void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	public final boolean execute() {
		try {
			this.conn.setAutoCommit(false);
			int retryCount = 0;
			while (retryCount <= this.MAX_RETRY_COUNT) {
				if (retryCount == this.MAX_RETRY_COUNT) {
					System.err.printf("[%s] Hit max of %d retries, aborting\n",this.xactName, retryCount);
					break;
				}
				if (retryCount > 0) {
					System.err.printf("[%s] Retry %d...\n", this.xactName, retryCount);
				}

				try {
					process();

					this.conn.commit();
					return true;		// execution succeeds

				} catch (SQLException e) {
					if (RETRY_SQL_STATE.equals(e.getSQLState())) {
						// Retry 40001 error
						// https://www.cockroachlabs.com/docs/stable/error-handling-and-troubleshooting.html
						this.conn.rollback();

						retryCount++;
						int sleepMillis = (int)(Math.pow(2, retryCount) * 100) + rand.nextInt(100);
						System.err.printf("[%s] Hit 40001 transaction retry error, sleeping %d milliseconds\n", this.xactName, sleepMillis);
						try {
							Thread.sleep(sleepMillis);
						} catch (InterruptedException ignored) {
					
						}
					} else {
						throw e;
					}
				}
			}

		} catch (SQLException exp) {
			System.err.println(exp);
		}
		return false;
	}

}
