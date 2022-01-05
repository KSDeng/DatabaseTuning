
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
		
		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[New Order Transaction]==========\n");
			
		long t1 = System.currentTimeMillis();
		// get d_next_o_id
		String sql_get_d_next_o_id = String.format(
			"select d_next_o_id from district where d_w_id = %d and d_id = %d\n",
			this.W_ID, this.D_ID);

		if (this.debug) System.out.println(sql_get_d_next_o_id);	// debug
		ResultSet res_d_next_o_id = conn.createStatement().executeQuery(sql_get_d_next_o_id);

		long t2 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_d_next_o_id", t2 - t1);

		int d_next_o_id = -1;
		while (res_d_next_o_id.next()) {
			d_next_o_id = res_d_next_o_id.getInt("d_next_o_id");
		}
		if (d_next_o_id == -1) {
			throw new SQLException("[New Order Transaction] Query failed, d_next_o_id not found");
		}

		// update d_next_o_id
		
		long t3 = System.currentTimeMillis();
		String sql_update_d_next_o_id = String.format(
			"update district set d_next_o_id = %d where d_w_id = %d and d_id = %d\n", 
			d_next_o_id + 1, this.W_ID, this.D_ID);

		if (this.debug) System.out.println(sql_update_d_next_o_id);
		conn.createStatement().executeUpdate(sql_update_d_next_o_id);
		long t4 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_update_d_next_o_id", t4 - t3);

		int all_local = 1;
		for (int i = 0; i < this.NUM_ITEMS; ++i) {
			if (this.W_ID != this.SUPPLIER_WAREHOUSE[i]) {
				all_local = 0;
				break;
			}
		}

		Timestamp ts = new Timestamp(System.currentTimeMillis());
		String ts_string = ts.toString();

		long t5 = System.currentTimeMillis();

		String sql_create_order = String.format(
			"insert into order_ \n" + 
			"(o_id, o_d_id, o_w_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local) \n" +
			"values (%d, %d, %d, %d, TIMESTAMP\'%s\', null, %d, %d)\n",
			d_next_o_id, this.D_ID, this.W_ID, this.C_ID, 
			ts_string, this.NUM_ITEMS, all_local);

		if (this.debug) System.out.println(sql_create_order);
		conn.createStatement().executeUpdate(sql_create_order);
		long t6 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_create_order", t6 - t5);

		double[] item_amounts = new double[this.NUM_ITEMS];
		Thread[] threads = new Thread[this.NUM_ITEMS];

		long loop_start_time = System.currentTimeMillis();

		for (int ii = 0; ii < this.NUM_ITEMS; ++ii) {

			final int i = ii;
			final int d_next_o_id_f = d_next_o_id;

			threads[ii] = new Thread(()->{
				try {
					long t_start = System.currentTimeMillis();

					String sql_get_s_quantity = String.format(
						"select s_quantity from stock where s_w_id = %d and s_i_id = %d\n",
						this.ITEM_NUMBER[i], this.SUPPLIER_WAREHOUSE[i]);

					if (this.debug) System.out.println(sql_get_s_quantity);
					ResultSet res_s_quantity = conn.createStatement().executeQuery(sql_get_s_quantity);
					int s_quantity = -1;
					while (res_s_quantity.next()) {
						s_quantity = res_s_quantity.getInt("s_quantity");
					}

					int adjusted_qty = s_quantity - this.QUANTITY[i];
					if (adjusted_qty < 10) adjusted_qty += 100;

					int remote_inc = 0;
					if (this.SUPPLIER_WAREHOUSE[i] != this.W_ID) remote_inc = 1;
					String sql_update_stock = String.format(
						"update stock \n" +
						"set s_quantity = %d, \n" +
						"	 s_ytd = s_ytd + %d, \n" +
						"	 s_order_cnt = s_order_cnt + 1, \n" +
						"	 s_remote_cnt = s_remote_cnt + %d\n" +
						"where s_w_id = %d and s_i_id = %d\n",
						adjusted_qty, this.QUANTITY[i], remote_inc,
						this.ITEM_NUMBER[i], this.SUPPLIER_WAREHOUSE[i]);

					if (this.debug) System.out.println(sql_update_stock);
					conn.createStatement().executeUpdate(sql_update_stock);

					String sql_get_i_info = String.format(
						"select i_price, i_name from item where i_id = %d\n", this.ITEM_NUMBER[i]);

					if (this.debug) System.out.println(sql_get_i_info);
					ResultSet res_i_info = conn.createStatement().executeQuery(sql_get_i_info);
					double i_price = -1;
					String i_name = "";
					while (res_i_info.next()) {
						i_price = res_i_info.getDouble("i_price");
						i_name = res_i_info.getString("i_name");
					}
					if (i_price == -1) {
						throw new SQLException("[New Order Transaction] Query failed, i_price not found");
					}
					if (i_name == "") {
						throw new SQLException("[New Order Transaction] Query failed, i_name not found");
					}

					double item_amount = this.QUANTITY[i] * i_price;
					item_amounts[i] = item_amount;

					String dist_info = String.format("S_DIST_%d", this.D_ID);
					String sql_create_ol = String.format(
						"insert into order_line \n" + 
						"(ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, \n" +
						"ol_supply_w_id, ol_quantity, ol_amount, ol_delivery_d, ol_dist_info) \n" +
						"values (%d, %d, %d, %d, %d, %d, %d, %f, null, \'%s\')\n",
						d_next_o_id_f, this.D_ID, this.W_ID, i,
						this.ITEM_NUMBER[i], this.SUPPLIER_WAREHOUSE[i],
						this.QUANTITY[i], item_amount, dist_info);

					if (this.debug) System.out.println(sql_create_ol);
					conn.createStatement().executeUpdate(sql_create_ol);

					// Output
					System.out.printf(
						"ITEM_NUMBER[i]\tI_NAME\tSUPPLIER_WAREHOUSE[i]\tQUANTITY[i]\tOL_AMOUNT\tS_QUANTITY\n" +
						"%d\t%s\t%d\t%d\t%f\t%d\n",
						this.ITEM_NUMBER[i], i_name, this.SUPPLIER_WAREHOUSE[i],
						this.QUANTITY[i], item_amount, adjusted_qty);

					long t_end = System.currentTimeMillis();
					if (this.analyze) printTimeInfo(String.format("Thread %d", i), t_end - t_start);

				} catch (SQLException e) {
					System.err.println("[New Order Transaction]" + e);
				}
			});
			threads[ii].start();
		}

		for (int i = 0; i < this.NUM_ITEMS; ++i) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				System.err.println("[New Order Transaction]" + e);
			}
		}

		long loop_end_time = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("for loop", loop_end_time - loop_start_time);

		double total_amount = 0;
		for (int i = 0; i < this.NUM_ITEMS; ++i) {
			total_amount += item_amounts[i];
		}

		// get d_tax
		long t7 = System.currentTimeMillis();
		String sql_get_d_tax = String.format(
			"select d_tax from district where d_w_id = %d and d_id = %d\n",
			this.W_ID, this.D_ID);

		if (this.debug) System.out.println(sql_get_d_tax);
		ResultSet res_get_d_tax = conn.createStatement().executeQuery(sql_get_d_tax);
		long t8 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_d_tax", t8 - t7);

		double d_tax = -1;
		while (res_get_d_tax.next()) {
			d_tax = res_get_d_tax.getDouble("d_tax");
		}
		if (d_tax == -1) {
			throw new SQLException("[New Order Transaction] Query failed, d_tax not found");
		}

		// get w_tax
		long t9 = System.currentTimeMillis();
		String sql_get_w_tax = String.format(
			"select w_tax from warehouse where w_id = %d\n",
			this.W_ID);

		if (this.debug) System.out.println(sql_get_w_tax);
		ResultSet res_get_w_tax = conn.createStatement().executeQuery(sql_get_w_tax);
		long t10 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_w_tax", t10 - t9);

		double w_tax = -1;
		while (res_get_w_tax.next()) {
			w_tax = res_get_w_tax.getDouble("w_tax");
		}
		if (w_tax == -1) {
			throw new SQLException("[New Order Transaction] Query failed, w_tax not found");
		}

		// get c_discount
		long t11 = System.currentTimeMillis();
		String sql_get_c_info = String.format(
			"select c_last, c_credit, c_discount from customer where c_w_id = %d and c_d_id = %d and c_id = %d\n",
			this.W_ID, this.D_ID, this.C_ID);

		if (this.debug) System.out.println(sql_get_c_info);
		ResultSet res_get_c_info = conn.createStatement().executeQuery(sql_get_c_info);
		long t12 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("sql_get_c_info", t12 -  t11);

		double c_discount = -1;
		String c_last = "", c_credit = "";

		while (res_get_c_info.next()) {
			c_discount = res_get_c_info.getDouble("c_discount");
			c_last = res_get_c_info.getString("c_last");
			c_credit = res_get_c_info.getString("c_credit");
		}
		if (c_discount == -1) {
			throw new SQLException("[New Order Transaction] Query failed, c_discount not found");
		}
		if (c_last == "") {
			throw new SQLException("[New Order Transaction] Query failed, c_last not found");
		}
		if (c_credit == "") {
			throw new SQLException("[New Order Transaction] Query failed, c_credit not found");
		}

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
		

		System.out.println("========================================\n");

	}

}
