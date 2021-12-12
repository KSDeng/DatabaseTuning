
import java.lang.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.*;

public class mainDriver {
	
	public static void main(String[] args) {
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

			// testing...
			Statement stat = conn.createStatement();
			stat.execute("select * from district");
			ResultSet res = stat.getResultSet();
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

		} catch (Exception e) {
			System.err.println(e);
		}

	}

}
