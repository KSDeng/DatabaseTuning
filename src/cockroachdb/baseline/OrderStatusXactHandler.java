import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class OrderStatusXactHandler extends XactHandler {
	// inputs
	private int C_W_ID;
	private int C_D_ID;
	private int C_ID;

	// debug
	private boolean debug;
	private boolean analyze;

	public OrderStatusXactHandler(Connection conn, int cwid, int cdid, int cid) {
		super("OrderStatusXact", conn);
		this.C_W_ID = cwid;
		this.C_D_ID = cdid;
		this.C_ID = cid;

		this.debug = true;
		this.analyze = true;
	}

	private void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	@Override
	void process() {
		System.out.printf("==========[Order Status Transaction]==========\n");
		try {
			String sql_get_c_info = String.format(
				"select c_first, c_middle, c_last, c_balance from customer\n" +
				"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.C_W_ID, this.C_D_ID, this.C_ID);
			ResultSet res_c_info = conn.createStatement().executeQuery(sql_get_c_info);
			String c_first = "", c_middle = "", c_last = "";
			double c_balance = -1;
			while (res_c_info.next()) {
				c_first = res_c_info.getString("c_first");
				c_middle = res_c_info.getString("c_middle");
				c_last = res_c_info.getString("c_last");
				c_balance = res_c_info.getDouble("c_balance");
			}

			System.out.printf("C_FIRST\tC_MIDDLE\tC_LAST\tC_BALANCE\n"
				+ "%s\t%s\t%s\t%f", c_first, c_middle, c_last, c_balance);

			String sql_get_last_order = String.format(
				"select o_w_id, o_d_id, o_c_id, max(o_entry_d) as last_o_entry_d\n" +
				"from order_ where o_w_id = %d and o_d_id = %d and o_c_id = %d\n" +
				"group by o_w_id, o_d_id, o_c_id\n", this.C_W_ID, this.C_D_ID, this.C_ID);
			ResultSet res_last_order = conn.createStatement().executeQuery(sql_get_last_order);
			
			String o_entry_d = "";
			while (res_last_order.next()) {
				o_entry_d = res_last_order.getString("last_o_entry_d");
			}

			String sql_get_order_info = String.format(
				"select o_id, o_carrier_id from order_ \n" +
				"where o_w_id = %d and o_d_id = %d and o_c_id = %d and o_entry_d = TIMESTAMP\'%s\'\n",
				this.C_W_ID, this.C_D_ID, this.C_ID, o_entry_d);
			ResultSet res_order_info = conn.createStatement().executeQuery(sql_get_order_info);


		} catch (SQLException e) {
			System.out.println(e);
		}
	}


}
