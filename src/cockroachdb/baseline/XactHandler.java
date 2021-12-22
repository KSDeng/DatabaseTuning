
import java.lang.*;
import java.util.Random;
import java.sql.*;
import com.zaxxer.hikari.*;

public abstract class XactHandler {

	private static final int MAX_RETRY_COUNT = 3;
	private static final String RETRY_SQL_STATE = "40001";
	private final Random rand = new Random();

	protected Connection conn;

	abstract void process();		// code to process the xact

	
	public XactHandler(Connection c) {
		this.conn = c;
	}

	public final boolean execute() {
		try {
			this.conn.setAutoCommit(false);
			int retryCount = 0;

			try {
				while (retryCount <= this.MAX_RETRY_COUNT) {
					if (retryCount == this.MAX_RETRY_COUNT) {
						break;
					}

					process();

					this.conn.commit();
					return true;		// execution succeeds
				}
			} catch (SQLException e) {
				if (RETRY_SQL_STATE.equals(e.getSQLState())) {
					this.conn.rollback();

					retryCount++;
					int sleepMillis = (int)(Math.pow(2, retryCount) * 100) + rand.nextInt(100);
					try {
						Thread.sleep(sleepMillis);
					} catch (InterruptedException ignored) {
				
					}
				} else {
					throw e;
				}
			}
		} catch (SQLException exp) {
			System.out.println(exp);
		}
		return false;
	}

}
