import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class StockLevelXactHandler extends XactHandler {

	// inputs
	private int W_ID;
	private int D_ID;
	private int T;
	private int L;

	// debug
	private boolean debug;
	private boolean analyze;

	public StockLevelXactHandler (Connection conn, int wid, int did, int t, int l) {
		super("StockLevelXact", conn);
		this.W_ID = wid;
		this.D_ID = did;
		this.T = t;
		this.L = l;

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Stock Level Transaction]==========\n");
		String sql_get_next_o_id = String.format(
			"select d_next_o_id from district2 where d_w_id = %d and d_id = %d\n",
			this.W_ID, this.D_ID);
		ResultSet res_next_o_id = conn.createStatement().executeQuery(sql_get_next_o_id);
		int d_next_o_id = -1;
		if (res_next_o_id.next()) {
			d_next_o_id = res_next_o_id.getInt("d_next_o_id");
		}
		if (d_next_o_id == -1) {
			throw new SQLException("[StockLevel Transaction] sql_get_next_o_id failed, d_next_o_id not found");
		}

		String sql_get_items = String.format(
			"select ol_i_id from order_line \n" +
			"where ol_w_id = %d and ol_d_id = %d and ol_o_id >= %d and ol_o_id < %d\n",
			this.W_ID, this.D_ID, d_next_o_id - L, d_next_o_id);
		ResultSet res_items = conn.createStatement().executeQuery(sql_get_items);

		int total_number = 0;
		while (res_items.next()) {
			int ol_i_id = res_items.getInt("ol_i_id");

			String sql_get_s_quantity = String.format(
				"select s_quantity from stock1 where s_w_id = %d and s_i_id = %d\n",
				this.W_ID, ol_i_id);
			ResultSet res_s_quantity = conn.createStatement().executeQuery(sql_get_s_quantity);
			int s_quantity = -1;
			if (res_s_quantity.next()) {
				s_quantity = res_s_quantity.getInt("s_quantity");
			}
			if (s_quantity == -1) {
				throw new SQLException("[StockLevel Transaction] sql_get_s_quantity failed, s_quantity not found");
			}

			if (s_quantity < this.T) {
				total_number += 1;
			}
		}

		System.out.printf("TOTAL_NUMBER\n%d\n", total_number);
		System.out.println("========================================\n");

	}
}
