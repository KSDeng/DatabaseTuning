
import java.lang.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.*;
import java.util.concurrent.*;

public class Demo {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		try {

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:postgresql://0.0.0.0:26257/wholesaledata");
			config.setUsername("root");
			config.addDataSourceProperty("ssl", "false");
			config.addDataSourceProperty("sslmode", "disable");
			config.addDataSourceProperty("reWriteBatchedInserts", "true");
			config.setAutoCommit(false);
			config.setMaximumPoolSize(240);		// server has 24 cores
			config.setKeepaliveTime(150000);

			HikariDataSource ds = new HikariDataSource(config);

			Connection conn = ds.getConnection();

			// Task with return value (ResultSet)
			FutureTask<ResultSet> futureTask = new FutureTask<>(()-> {
				Statement stmt = conn.createStatement();
				stmt.execute("select * from district limit 10");
				return stmt.getResultSet();
			});

			FutureTask<ResultSet> futureTask2 = new FutureTask<>(()-> {
				Statement stmt = conn.createStatement();
				stmt.execute("select * from order_ limit 10");
				return stmt.getResultSet();
			});

			// Task without return value
			// pass a runnable instance to Thread()

			// run t1 and t2 in parallel
			Thread t1 = new Thread(futureTask);
			t1.start();

			Thread t2 = new Thread(futureTask2);
			t2.start();

			// if t2 has to wait for t1, use t1.join()

			// the printing process must be sequential
			// get result of t1 
			ResultSet res = futureTask.get();
			ResultSetMetaData rsmd = res.getMetaData();
			int columnNumber = rsmd.getColumnCount();
			for (int i = 1; i <= columnNumber; ++i) {
				if (i > 1) System.out.print("\t");
				System.out.print(rsmd.getColumnName(i));
			}
			System.out.println("");
			while (res.next()) {
				for (int i = 1; i <= columnNumber; ++i) {
					if (i > 1) System.out.print("\t");
					System.out.print(res.getString(i));
				}
				System.out.println("");
			}	
			System.out.println("");

			// get result of t2
			ResultSet res2 = futureTask2.get();
			ResultSetMetaData rsmd2 = res2.getMetaData();
			columnNumber = rsmd2.getColumnCount();
			for (int i = 1; i <= columnNumber; ++i) {
				if (i > 1) System.out.print("\t");
				System.out.print(rsmd2.getColumnName(i));
			}
			System.out.println("");
			while (res2.next()) {
				for (int i = 1; i <= columnNumber; ++i) {
					if (i > 1) System.out.print("\t");
					System.out.print(res2.getString(i));
				}
				System.out.println("");
			}	
			System.out.println("");

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}

}
