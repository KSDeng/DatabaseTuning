import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class DeliveryXactHandler extends XactHandler {
	// inputs
	private int W_ID;
	private int CARRIER_ID;

	// debug
	private boolean debug;
	private boolean analyze;

	public DeliveryXactHandler(Connection conn, int w_id, int carrier_id) {
		super("DeliveryXact", conn);
		this.W_ID = w_id;
		this.CARRIER_ID = carrier_id;

		this.debug = true;
		this.analyze = true;
	}

	private void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	@Override
	void process() {
		System.out.printf("==========[Delivery Transaction]==========\n");
		try {
			for (int district_no = 1; district_no <= 10; ++district_no) {

				long t1 = System.currentTimeMillis();

				String sql_get_min_oid = String.format(
					"select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
					"from order_ \n" +
					"where o_w_id = %d and o_d_id = %d and o_carrier_id is null\n" +
					"group by o_w_id, o_d_id, o_carrier_id\n",
					this.W_ID, district_no);
				if (this.debug) System.out.println(sql_get_min_oid);
				ResultSet res_min_oid = conn.createStatement().executeQuery(sql_get_min_oid);

				long t2 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_get_min_oid", t2 - t1);

				int min_oid = -1;
				while (res_min_oid.next()) {
					min_oid = res_min_oid.getInt("min_oid");
				}
				if (min_oid == -1) {
					throw new SQLException("[Delivery Transaction] sql_get_min_oid failed, min_oid not found");
				}

				long t3 = System.currentTimeMillis();

				String sql_update_order = String.format(
					"update order_ set o_carrier_id = %d\n" +
					"where o_w_id = %d and o_d_id = %d and o_id = %d\n",
					this.CARRIER_ID, this.W_ID, district_no, min_oid);
				if (this.debug) System.out.println(sql_update_order);
				conn.createStatement().executeUpdate(sql_update_order);

				long t4 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_update_order", t4 - t3);

				String sql_update_ol = String.format(
					"update order_line set ol_delivery_d = current_timestamp\n" +
					"where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
					this.W_ID, district_no, min_oid);

				if (this.debug) System.out.println(sql_update_ol);
				conn.createStatement().executeUpdate(sql_update_ol);
				long t5 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_update_ol", t5 - t4);

				String sql_get_sum_amount = String.format(
					"select ol_w_id, ol_d_id, ol_o_id, sum(ol_amount) as sum_amount\n" +
					"from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n" +
					"group by ol_w_id, ol_d_id, ol_o_id\n",
					this.W_ID, district_no, min_oid);

				if (this.debug) System.out.println(sql_get_sum_amount);
				ResultSet res_sum_amount = conn.createStatement().executeQuery(sql_get_sum_amount);
				long t6 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_get_sum_amount", t6 - t5);

				double sum_amount = -1;
				while (res_sum_amount.next()) {
					sum_amount = res_sum_amount.getDouble("sum_amount");
				}
				if (sum_amount == -1) {
					throw new SQLException("[Delivery Transaction] sql_get_sum_amount failed, sum_amount not found");
				}

				long t7 = System.currentTimeMillis();

				String sql_get_cid = String.format(
					"select o_c_id from order_ where o_w_id = %d and o_d_id = %d and o_id = %d\n",
					this.W_ID, district_no, min_oid);
				if (this.debug) System.out.println(sql_get_cid);
				ResultSet res_cid = conn.createStatement().executeQuery(sql_get_cid);
				long t8 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_get_cid", t8 - t7);
				int cid = -1;
				while(res_cid.next()) {
					cid = res_cid.getInt("o_c_id");
				}
				if (cid == -1) {
					throw new SQLException("[Delivery Transaction] sql_get_cid failed, o_c_id not found");
				}

				long t9 = System.currentTimeMillis();
				String sql_update_customer = String.format(
					"update customer2\n" +
					"set c_balance = c_balance + %f, c_delivery_cnt = c_delivery_cnt + 1\n" +
					"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
					sum_amount, this.W_ID, district_no, cid);
				if (this.debug) System.out.println(sql_update_customer);
				long t10 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_update_customer", t10 - t9);

				conn.createStatement().executeUpdate(sql_update_customer);
			}

		} catch (SQLException e) {
			System.out.println(e);
		}
		System.out.println("========================================\n");
	}

}
