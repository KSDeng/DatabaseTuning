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
		super("Order Status Transaction", conn);
		this.C_W_ID = cwid;
		this.C_D_ID = cdid;
		this.C_ID = cid;

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Order Status Transaction]==========\n");
		long t1 = System.currentTimeMillis();
		String sql_get_c_info = String.format(
			"select c_first, c_middle, c_last from customer1\n" +
			"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
			this.C_W_ID, this.C_D_ID, this.C_ID);

		if (this.debug) System.out.println(sql_get_c_info);
		ResultSet res_c_info = conn.createStatement().executeQuery(sql_get_c_info);

		String sql_get_c_balance = String.format(
			"select c_balance from customer3\n" +
			"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
			this.C_W_ID, this.C_D_ID, this.C_ID);
		if (this.debug) System.out.println(sql_get_c_balance);
		ResultSet res_c_balance = conn.createStatement().executeQuery(sql_get_c_balance);

		long t2 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_c_info", t2 - t1);
		String c_first = "", c_middle = "", c_last = "";
		double c_balance = -1;
		if (res_c_info.next()) {
			c_first = res_c_info.getString("c_first");
			c_middle = res_c_info.getString("c_middle");
			c_last = res_c_info.getString("c_last");
		}
		if (res_c_balance.next()) {
			c_balance = res_c_balance.getDouble("c_balance");
		}

		System.out.printf("C_FIRST\tC_MIDDLE\tC_LAST\tC_BALANCE\n"
			+ "%s\t%s\t%s\t%f\n", c_first, c_middle, c_last, c_balance);

		long t3 = System.currentTimeMillis();
		String sql_get_last_order = String.format(
			"select o_w_id, o_d_id, o_c_id, max(o_id) as max_oid\n" +
			"from order1 where o_w_id = %d and o_d_id = %d and o_c_id = %d\n" +
			"group by o_w_id, o_d_id, o_c_id\n",
			this.C_W_ID, this.C_D_ID, this.C_ID);

		if (this.debug) System.out.println(sql_get_last_order);
		ResultSet res_last_order = conn.createStatement().executeQuery(sql_get_last_order);
		long t4 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_last_order", t4 - t3);
		
		int o_id = -1;
		if (res_last_order.next()) {
			o_id = res_last_order.getInt("max_oid");
		}

		long t5 = System.currentTimeMillis();
		String sql_get_o_entry_d = String.format(
			"select o_entry_d from order1\n" +
			"where o_w_id = %d and o_d_id = %d and o_id = %d\n",
			this.C_W_ID, this.C_D_ID, o_id);
		ResultSet res_o_entry_d = conn.createStatement().executeQuery(sql_get_o_entry_d);

		String sql_get_o_carrier_id = String.format(
			"select o_carrier_id from order2\n" +
			"where o_w_id = %d and o_d_id = %d and o_id = %d\n",
			this.C_W_ID, this.C_D_ID, o_id);
		ResultSet res_o_carrier_id = conn.createStatement().executeQuery(sql_get_o_carrier_id);
		
		long t6 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_order_info", t6 - t5);
		String o_entry_d = "";
		int o_carrier_id = -1;
		if (res_o_entry_d.next()) {
			o_entry_d = res_o_entry_d.getString("o_entry_d");
		}
		if (res_o_carrier_id.next()) {
			o_carrier_id = res_o_carrier_id.getInt("o_carrier_id");
		}
		System.out.printf("O_ID\tO_ENTRY_D\tO_CARRIER_ID\n" + 
			"%d\t%s\t%d\n", o_id, o_entry_d, o_carrier_id);

		
		int ol_i_id = -1, ol_supply_w_id = -1, ol_quantity = -1;
		double ol_amount = -1;
		String ol_delivery_d = "";

		String sql_get_ol_info1 = String.format(
			"select ol_i_id, ol_quantity \n" +
			"from order_line1 where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
			this.C_W_ID, this.C_D_ID, o_id);
		ResultSet res_ol_info1 = conn.createStatement().executeQuery(sql_get_ol_info1);

		String sql_get_ol_info2 = String.format(
			"select ol_delivery_d, ol_amount, ol_supply_w_id \n" +
			"from order_line2 where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
			this.C_W_ID, this.C_D_ID, o_id);
		ResultSet res_ol_info2 = conn.createStatement().executeQuery(sql_get_ol_info2);

		if (res_ol_info1.next()) {
			ol_i_id = res_ol_info1.getInt("ol_i_id");
			ol_quantity = res_ol_info1.getInt("ol_quantity");
		}
		if (res_ol_info2.next()) {
			ol_supply_w_id = res_ol_info2.getInt("ol_supply_w_id");
			ol_amount = res_ol_info2.getDouble("ol_amount");
			ol_delivery_d = res_ol_info2.getString("ol_delivery_d");
		}

		System.out.printf("OL_I_ID\tOL_SUPPLY_W_ID\tOL_QUANTITY\tOL_AMOUNT\tOL_DELIVERY_D\n" +
			"%d\t%d\t%d\t%f\t%s\n", ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_delivery_d);

		System.out.println("========================================\n");

	}


}
