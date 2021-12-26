
import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class NewOrderXactHandler extends XactHandler {

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
		super("NewOrderXact", conn);
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

	@Override
	void process() {
		System.out.printf("==========[New Order Transaction]==========\n");
	
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

			if (this.debug) System.out.println(sql_update_d_next_o_id);
			conn.createStatement().execute(sql_update_d_next_o_id);
			if (this.analyze) getTimeMillis();

			int all_local = 1;
			for (int i = 0; i < this.NUM_ITEMS; ++i) {
				if (this.W_ID != this.SUPPLIER_WAREHOUSE[i]) {
					all_local = 0;
					break;
				}
			}

			Timestamp ts = new Timestamp(System.currentTimeMillis());
			String ts_string = ts.toString();

			String sql_create_order = String.format(
				"insert into order_ values \n" +
				"(%d, %d, %d, %d, %s, null, %d, %d)\n",
				d_next_o_id, this.D_ID, this.W_ID, this.C_ID, 
				ts_string, this.NUM_ITEMS, all_local);

			if (this.debug) System.out.println(sql_create_order);
			conn.createStatement().execute(sql_create_order);
			if (this.analyze) getTimeMillis();

			double total_amount = 0;
			for (int i = 0; i < this.NUM_ITEMS; ++i) {
				String sql_get_s_quantity = String.format(
					"select s_quantity from stock1 where s_w_id = %d and s_i_id = %d\n",
					this.ITEM_NUMBER[i], this.SUPPLIER_WAREHOUSE[i]);
				Statement stmt_get_s_quantity = conn.createStatement();

				if (this.debug) System.out.println(sql_get_s_quantity);
				stmt_get_s_quantity.execute(sql_get_s_quantity);
				if (this.analyze) getTimeMillis();

				ResultSet res_s_quantity = stmt_get_s_quantity.getResultSet();
				int s_quantity = res_s_quantity.getInt("s_quantity");

				int adjusted_qty = s_quantity - this.QUANTITY[i];
				if (adjusted_qty < 10) adjusted_qty += 100;

				int remote_inc = 0;
				if (this.SUPPLIER_WAREHOUSE[i] != this.W_ID) remote_inc = 1;
				String sql_update_stock = String.format(
					"update stock1 \n" +
					"set s_quantity = %d \n" +
					"	 s_ytd = s_ytd + %d \n" +
					"	 s_order_cnt = s_order_cnt + 1 \n" +
					"	 s_remote_cnt = s_remote_cnt + %d\n",
					adjusted_qty, this.QUANTITY[i], remote_inc);

				if (this.debug) System.out.println(sql_update_stock);
				conn.createStatement().execute(sql_update_stock);
				if (this.analyze) getTimeMillis();

				String sql_get_i_info = String.format(
					"select i_price, i_name from item1 where i_id = %d\n", this.ITEM_NUMBER[i]);
				Statement stmt_get_i_info = conn.createStatement();

				if (this.debug) System.out.println(sql_get_i_info);
				stmt_get_i_info.execute(sql_get_i_info);
				if (this.analyze) getTimeMillis();

				ResultSet res_i_info = stmt_get_i_info.getResultSet();
				double i_price = res_i_info.getDouble("i_price");
				String i_name = res_i_info.getString("i_name");

				double item_amount = this.QUANTITY[i] * i_price;

				total_amount += item_amount;

				String dist_info = String.format("S_DIST_%d", this.D_ID);
				String sql_create_ol = String.format(
					"insert into order_line values \n" +
					"(%d, %d, %d, %d, %d, %d, %d, %f, null, %s)\n",
					d_next_o_id, this.D_ID, this.W_ID, i,
					this.ITEM_NUMBER[i], this.SUPPLIER_WAREHOUSE[i],
					this.QUANTITY[i], item_amount, dist_info);

				if (this.debug) System.out.println(sql_create_ol);
				conn.createStatement().execute(sql_create_ol);
				if (this.analyze) getTimeMillis();

				// Output
				System.out.printf(
					"ITEM_NUMBER[i]\tI_NAME\tSUPPLIER_WAREHOUSE[i]\tQUANTITY[i]\tOL_AMOUNT\tS_QUANTITY\n" +
					"%d\t%s\t%d\t%d\t%f\t%d\n",
					this.ITEM_NUMBER[i], i_name, this.SUPPLIER_WAREHOUSE[i],
					this.QUANTITY[i], item_amount, adjusted_qty);
			}

			// get d_tax
			String sql_get_d_tax = String.format(
				"select d_tax from district1 where d_w_id = %d and d_id = %d\n",
				this.W_ID, this.D_ID);
			Statement stmt_get_d_tax = conn.createStatement();

			if (this.debug) System.out.println(sql_get_d_tax);
			stmt_get_d_tax.execute(sql_get_d_tax);
			if (this.analyze) getTimeMillis();

			ResultSet res_get_d_tax = stmt_get_d_tax.getResultSet();
			double d_tax = res_get_d_tax.getDouble("d_tax");

			// get w_tax
			String sql_get_w_tax = String.format(
				"select w_tax from warehouse1 where w_id = %d\n",
				this.W_ID);
			Statement stmt_get_w_tax = conn.createStatement();

			if (this.debug) System.out.println(sql_get_w_tax);
			stmt_get_w_tax.execute(sql_get_w_tax);
			if (this.analyze) getTimeMillis();

			ResultSet res_get_w_tax = stmt_get_w_tax.getResultSet();
			double w_tax = res_get_w_tax.getDouble("w_tax");

			// get c_discount
			String sql_get_c_info = String.format(
				"select c_last, c_credit, c_discount from customer1 where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.W_ID, this.D_ID, this.C_ID);
			Statement stmt_get_c_info = conn.createStatement();

			if (this.debug) System.out.println(sql_get_c_info);
			stmt_get_c_info.execute(sql_get_c_info);
			if (this.analyze) getTimeMillis();

			ResultSet res_get_c_info = stmt_get_c_info.getResultSet();
			double c_discount = res_get_c_info.getDouble("c_discount");
			String c_last = res_get_c_info.getString("c_last");
			String c_credit = res_get_c_info.getString("c_credit");

			total_amount = total_amount * (1 + d_tax + w_tax) * (1 - c_discount);

			// Output
			System.out.printf(
				"W_ID\tD_ID\tC_ID\tC_LAST\tC_CREDIT\tC_DISCOUNT\n" +
				"%d\t%d\t%d\t%s\t%s\t%f\n",
				this.W_ID, this.D_ID, this.C_ID, c_last, c_credit, c_discount);
			System.out.printf(
				"W_TAX\tD_TAX\tO_ID\tO_ENTRY_D\tNUM_ITEMS\tTOTAL_AMOUNT\n" +
				"%f\t%f\t%d\t%s\t%d\t%f\n", 
				w_tax, d_tax, d_next_o_id, ts_string, this.NUM_ITEMS, total_amount);
			
		} catch (SQLException e) {
			System.out.println(e);
		}	

		System.out.println("====================\n");

	}

}
