
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
			
			// get d_next_o_id
			String sql_get_d_next_o_id = String.format(
				"select d_next_o_id from district2 where d_w_id = %d and d_id = %d\n",
				this.W_ID, this.D_ID);
			if (this.debug) System.out.println(sql_get_d_next_o_id);	// debug
			Statement stat_get_d_next_o_id = conn.createStatement();
			stat_get_d_next_o_id.execute(sql_get_d_next_o_id);
			ResultSet res_d_next_o_id = stat_get_d_next_o_id.getResultSet();
			int d_next_o_id = res_d_next_o_id.getInt("d_next_o_id");
			if (this.analyze) getTimeMillis();	// analyze

			// update d_next_o_id
			String sql_update_d_next_o_id = String.format(
				"update district2 set d_next_o_id = %d\n", d_next_o_id + 1);
			conn.createStatement().execute(sql_update_d_next_o_id);

			int all_local = 1;
			for (int i = 0; i < this.NUM_ITEMS; ++i) {
				if (this.W_ID != this.SUPPLIER_WAREHOUSE[i]) {
					all_local = 0;
					break;
				}
			}

			String sql_create_order = String.format(
				"insert into order_ values \n" +
				"(%d, %d, %d, %d, current_timestamp, null, %d, %d)\n",
				d_next_o_id, this.D_ID, this.W_ID, this.C_ID, 
				this.NUM_ITEMS, all_local)
			conn.createStatement().exeucte(sql_create_order);

			
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
