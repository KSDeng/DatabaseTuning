
import java.lang.*;
import java.sql.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.*;

public class NewOrderXactHandler {

	private Connection conn;

	// inputs
	private int W_ID;
	private int D_ID;
	private int C_ID;
	private int NUM_ITEMS;
	private int[] ITEM_NUMBER;
	private int[] SUPPLIER_WAREHOUSE;
	private int[] QUANTITY;

	// debug
	private boolean debug;
	private boolean analyze;

	public NewOrderXactHandler(Connection conn, int wid, int did, int cid, int num_items,
		int[] item_number, int[] supplier_warehouse, int[] quantity) {
		this.conn = conn;

		this.W_ID = wid;
		this.D_ID = did;
		this.C_ID = cid;
		this.NUM_ITEMS = num_items;
		this.ITEM_NUMBER = item_number;
		this.SUPPLIER_WAREHOUSE = supplier_warehouse;
		this.QUANTITY = quantity;
		
		this.debug = true;
		this.analyze = true;
	}

	private void getTimeMillis() {
		System.out.printf("TIMEINMILLIS: %d\n", System.currentTimeMillis());
	}

	public boolean process() {
	
		try {
			if (this.analyze) getTimeMillis();	// analyze
			
			Statement stat = conn.createStatement();
			String sql1 = String.format(
				"select d_next_o_id from district2 where d_w_id = %d and d_id = %d",
				this.W_ID, this.D_ID);
			if (this.debug) System.out.println(sql1 + "\n");	// debug
			
			stat.execute(sql1);
			ResultSet res1 = stat.getResultSet();

			if (this.analyze) getTimeMillis();	// analyze

			
			
			String sql2 = String.format(
				"update district2 set d_next_o_id = d_next_o_id + 1 \n" +
				"where d_w_id = %d and d_id = %d\n", this.W_ID, this.D_ID);
			if (this.debug) System.out.println(sql2 + "\n");	// debug
			stat.execute(sql2);
		} catch (SQLException e) {
			System.out.println(e);
		}	

		return true;		// true means succeed
	}

	public static void main(String[] args) {
		try {

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:postgresql://0.0.0.0:26257/wholesaledata");
			config.setUsername("root");
			config.addDataSourceProperty("ssl", "false");
			config.addDataSourceProperty("sslmode", "disable");
			config.addDataSourceProperty("reWriteBatchedInserts", "true");
			config.setAutoCommit(false);
			config.setMaximumPoolSize(240);
			config.setKeepaliveTime(150000);

			HikariDataSource ds = new HikariDataSource(config);

			Connection conn = ds.getConnection();


		} catch (Exception e) {
			System.out.println(e);
		}

	}

}
