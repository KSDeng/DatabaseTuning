import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class PaymentXactHandler extends XactHandler {

	// inputs
	private int C_W_ID;
	private int C_D_ID;
	private int C_ID;

	private double PAYMENT;

	// debug
	private boolean debug;
	private boolean analyze;

	public PaymentXactHandler(Connection conn, int c_w_id, int c_d_id, int c_id, double payment) {
		super("PaymentXact", conn);
		this.C_W_ID = c_w_id;
		this.C_D_ID = c_d_id;
		this.C_ID = c_id;

		this.PAYMENT = payment;

		this.debug = true;
		this.analyze = true;
	}

	private void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	@Override
	void process() {
		System.out.printf("==========[Payment Transaction]==========\n");

		try {
			// update warehouse
			String sql_update_warehouse = String.format(
				"update warehouse2 set w_ytd = w_ytd + %f where w_id = %d\n",
				this.PAYMENT, this.C_W_ID);
			this.conn.createStatement().execute(sql_update_warehouse);

			// update district
			String sql_update_district = String.format(
				"update district2 set d_ytd = d_ytd + %f where d_w_id = %d and d_id = %d\n",
				this.PAYMENT, this.C_W_ID, this.C_D_ID);
			this.conn.createStatement().execute(sql_update_district);

			// update customer
			String sql_update_customer = String.format(
				"update customer2 set c_balance = c_balance - %f,\n" +
				"	c_ytd_payment = c_ytd_payment + %f, \n" +
				"	c_payment_cnt = c_payment_cnt + 1\n" +
				"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.PAYMENT, this.PAYMENT, this.C_W_ID, this.C_D_ID, this.C_ID);
			this.conn.createStatement().execute(sql_update_customer);

			// get customer info
			String sql_get_customer_info = String.format(
				"select * from customer1 where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.C_W_ID, this.C_D_ID, this.C_ID);
			ResultSet res_customer_info = conn.createStatement().executeQuery(sql_get_customer_info);

			String sql_get_c_balance = String.format(
				"select * from customer3 where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.C_W_ID, this.C_D_ID, this.C_ID);
			ResultSet res_c_balance = conn.createStatement().executeQuery(sql_get_c_balance);
			
			String c_first = "", c_middle = "", c_last = "";
			String c_street_1 = "", c_street_2 = "", c_city = "", c_state = "", c_zip = "";
			String c_phone = "", c_since = "", c_credit = "";
			double c_credit_lim = 0, c_discount = 0, c_balance = 0;

			while (res_customer_info.next()) {
				c_first = res_customer_info.getString("c_first");
				c_middle = res_customer_info.getString("c_middle");
				c_last = res_customer_info.getString("c_last");
				c_street_1 = res_customer_info.getString("c_street_1");
				c_street_2 = res_customer_info.getString("c_street_2");
				c_city = res_customer_info.getString("c_city");
				c_state = res_customer_info.getString("c_state");
				c_zip = res_customer_info.getString("c_zip");
				c_credit_lim = res_customer_info.getDouble("c_credit_lim");
				c_discount = res_customer_info.getDouble("c_discount");
			}
			while (res_c_balance.next()) {
				c_balance = res_c_balance.getDouble("c_balance");
			}

			System.out.printf("C_W_ID\tC_D_ID\tC_ID\tC_FIRST\tC_MIDDLE\tC_LAST\n" +
				"%s\t%s\t%s\t%s\t%s\t%s\n", this.C_W_ID, this.C_D_ID, this.C_ID, c_first, c_middle, c_last);
			System.out.printf("C_STREET_1\tC_STREET_2\tC_CITY\tC_STATE\tC_ZIP\n" +
				"%s\t%s\t%s\t%s\t%s\n", c_street_1, c_street_2, c_city, c_state, c_zip);
			System.out.printf("C_CREDIT_LIM\tC_DISCOUNT\tC_BALANCE\n" +
				"%f\t%f\t%f\n", c_credit_lim, c_discount, c_balance);
			
			// get warehouse info 
			String w_street_1 = "", w_street_2 = "", w_city = "", w_state = "", w_zip = "";
			String sql_get_warehouse_info = String.format(
				"select w_street_1, w_street_2, w_city, w_state, w_zip from warehouse1 where w_id = %d\n",
				this.C_W_ID);
			ResultSet res_warehouse_info = conn.createStatement().executeQuery(sql_get_warehouse_info);
			while (res_warehouse_info.next()){
				w_street_1 = res_warehouse_info.getString("w_street_1");
				w_street_2 = res_warehouse_info.getString("w_street_2");
				w_city = res_warehouse_info.getString("w_city");
				w_state = res_warehouse_info.getString("w_state");
				w_zip = res_warehouse_info.getString("w_zip");
			}
			System.out.printf("W_STREET_1\tW_STREET_2\tW_CITY\tW_STATE\tW_ZIP\n" +
				"%s\t%s\t%s\t%s\t%s\n", w_street_1, w_street_2, w_city, w_state, w_zip);

			// get district info
			String d_street_1 = "", d_street_2 = "", d_city = "", d_state = "", d_zip = "";
			String sql_get_district_info = String.format(
				"select d_street_1, d_street_2, d_city, d_state, d_zip from district1 where d_w_id = %d and d_id = %d\n",
				this.C_W_ID, this.C_D_ID);
			ResultSet res_district_info = conn.createStatement().executeQuery(sql_get_district_info);
			while (res_district_info.next()) {
				d_street_1 = res_district_info.getString("d_street_1");
				d_street_2 = res_district_info.getString("d_street_2");
				d_city = res_district_info.getString("d_city");
				d_state = res_district_info.getString("d_state");
				d_zip = res_district_info.getString("d_zip");
			}
			System.out.printf("D_STREET_1\tD_STREET_2\tD_CITY\tD_STATE\tD_ZIP\n" +
				"%s\t%s\t%s\t%s\t%s\n", d_street_1, d_street_2, d_city, d_state, d_zip);

			System.out.printf("PAYMENT\n%f\n", this.PAYMENT);


		} catch (SQLException e) {
			System.out.println(e);
		}

		System.out.println("========================================\n");

	}

}