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

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() {
		System.out.printf("==========[Order Status Transaction]==========\n");
		try {
			long t1 = System.currentTimeMillis();
			String sql_get_c_info = String.format(
				"select c_first, c_middle, c_last, c_balance from customer\n" +
				"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.C_W_ID, this.C_D_ID, this.C_ID);

			if (this.debug) System.out.println(sql_get_c_info);
			ResultSet res_c_info = conn.createStatement().executeQuery(sql_get_c_info);

			long t2 = System.currentTimeMillis();
			if (this.analyze) printTimeInfo("sql_get_c_info", t2 - t1);
			String c_first = "", c_middle = "", c_last = "";
			double c_balance = -1;
			while (res_c_info.next()) {
				c_first = res_c_info.getString("c_first");
				c_middle = res_c_info.getString("c_middle");
				c_last = res_c_info.getString("c_last");
				c_balance = res_c_info.getDouble("c_balance");
			}

			System.out.printf("C_FIRST\tC_MIDDLE\tC_LAST\tC_BALANCE\n"
				+ "%s\t%s\t%s\t%f\n", c_first, c_middle, c_last, c_balance);

			long t3 = System.currentTimeMillis();
			String sql_get_last_order = String.format(
				"select o_w_id, o_d_id, o_c_id, max(o_entry_d) as last_o_entry_d\n" +
				"from order_ where o_w_id = %d and o_d_id = %d and o_c_id = %d\n" +
				"group by o_w_id, o_d_id, o_c_id\n", this.C_W_ID, this.C_D_ID, this.C_ID);
			if (this.debug) System.out.println(sql_get_last_order);
			ResultSet res_last_order = conn.createStatement().executeQuery(sql_get_last_order);
			long t4 = System.currentTimeMillis();
			if (this.analyze) printTimeInfo("sql_get_last_order", t4 - t3);
			
			String o_entry_d = "";
			while (res_last_order.next()) {
				o_entry_d = res_last_order.getString("last_o_entry_d");
			}

			long t5 = System.currentTimeMillis();
			String sql_get_order_info = String.format(
				"select o_id, o_carrier_id from order_ \n" +
				"where o_w_id = %d and o_d_id = %d and o_c_id = %d and o_entry_d = TIMESTAMP\'%s\'\n",
				this.C_W_ID, this.C_D_ID, this.C_ID, o_entry_d);
			if (this.debug) System.out.println(sql_get_order_info);
			ResultSet res_order_info = conn.createStatement().executeQuery(sql_get_order_info);

			long t6 = System.currentTimeMillis();
			if (this.analyze) printTimeInfo("sql_get_order_info", t6 - t5);
			int o_id = -1, o_carrier_id = -1;
			while (res_order_info.next()) {
				o_id = res_order_info.getInt("o_id");
				o_carrier_id = res_order_info.getInt("o_carrier_id");
			}
			System.out.printf("O_ID\tO_ENTRY_D\tO_CARRIER_ID\n" + 
				"%d\t%s\t%d\n", o_id, o_entry_d, o_carrier_id);

			
			int ol_i_id = -1, ol_supply_w_id = -1, ol_quantity = -1;
			double ol_amount = -1;
			String ol_delivery_d = "";

			long t7 = System.currentTimeMillis();
			String sql_get_ol_info = String.format(
				"select ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_delivery_d\n" +
				"from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
				this.C_W_ID, this.C_D_ID, o_id);
			if (this.debug) System.out.println(sql_get_ol_info);
			ResultSet res_ol_info = conn.createStatement().executeQuery(sql_get_ol_info);
			long t8 = System.currentTimeMillis();
			if (this.analyze) printTimeInfo("sql_get_ol_info", t8 - t7);

			while (res_ol_info.next()) {
				ol_i_id = res_ol_info.getInt("ol_i_id");
				ol_supply_w_id = res_ol_info.getInt("ol_supply_w_id");
				ol_quantity = res_ol_info.getInt("ol_quantity");
				ol_amount = res_ol_info.getDouble("ol_amount");
				ol_delivery_d = res_ol_info.getString("ol_delivery_d");
			}
			System.out.printf("OL_I_ID\tOL_SUPPLY_W_ID\tOL_QUANTITY\tOL_AMOUNT\tOL_DELIVERY_D\n" +
				"%d\t%d\t%d\t%f\t%s\n", ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_delivery_d);

			System.out.println("========================================\n");

		} catch (SQLException e) {
			System.out.println(e);
		}
	}


}
