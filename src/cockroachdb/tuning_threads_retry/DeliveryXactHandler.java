import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class DeliveryXactHandler {
	// inputs
	private int W_ID;
	private int CARRIER_ID;

	// executor
	private SqlExecutor sqlExecutor;

	// debug
	private boolean debug;
	private boolean analyze;

	public DeliveryXactHandler(Connection conn, int w_id, int carrier_id) {
		this.sqlExecutor = new SqlExecutor("DeliveryXact", conn);

		this.W_ID = w_id;
		this.CARRIER_ID = carrier_id;

		this.debug = false;
		this.analyze = false;
	}
	
	private void printTimeInfo(String name, double timeInMillis) {
		System.out.printf("%s completed in %8.3f milliseconds \n", name, timeInMillis);
	}

	public boolean execute() {

		System.out.printf("==========[Delivery Transaction]==========\n");

		Thread[] threads = new Thread[10];
		boolean[] flags = new boolean[10];

		for (int ii = 1; ii <= 10; ++ii) {
			flags[ii-1] = true;
			final int i = ii;
			final int district_no = ii;

			threads[ii-1] = new Thread(()->{
				
				try {
					long t_start = System.currentTimeMillis();
					long t1 = System.currentTimeMillis();

					String sql_get_min_oid = String.format(
						"select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
						"from order_ \n" +
						"where o_w_id = %d and o_d_id = %d and o_carrier_id is null\n" +
						"group by o_w_id, o_d_id, o_carrier_id\n",
						this.W_ID, district_no);
					if (this.debug) System.out.println(sql_get_min_oid);
					ResultSet res_min_oid = this.sqlExecutor.execute(sql_get_min_oid);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_get_min_oid failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}

					long t2 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_get_min_oid", t2 - t1);

					int min_oid = -1;
					if (res_min_oid.next()) {
						min_oid = res_min_oid.getInt("min_oid");
					}
					if (min_oid == -1) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d min_oid not found.\n", i-1);
						//flags[i - 1] = false;
						return;
					}

					long t3 = System.currentTimeMillis();

					String sql_update_order = String.format(
						"update order_ set o_carrier_id = %d\n" +
						"where o_w_id = %d and o_d_id = %d and o_id = %d\n",
						this.CARRIER_ID, this.W_ID, district_no, min_oid);
					if (this.debug) System.out.println(sql_update_order);
					this.sqlExecutor.execute(sql_update_order);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_update_order failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}

					long t4 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_update_order", t4 - t3);

					String sql_update_ol = String.format(
						"update order_line set ol_delivery_d = current_timestamp\n" +
						"where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
						this.W_ID, district_no, min_oid);

					if (this.debug) System.out.println(sql_update_ol);
					this.sqlExecutor.execute(sql_update_ol);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_get_update_ol failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}
					long t5 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_update_ol", t5 - t4);

					String sql_get_sum_amount = String.format(
						"select ol_w_id, ol_d_id, ol_o_id, sum(ol_amount) as sum_amount\n" +
						"from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n" +
						"group by ol_w_id, ol_d_id, ol_o_id\n",
						this.W_ID, district_no, min_oid);

					if (this.debug) System.out.println(sql_get_sum_amount);
					ResultSet res_sum_amount = this.sqlExecutor.execute(sql_get_sum_amount);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_get_sum_amount failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}
					long t6 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_get_sum_amount", t6 - t5);

					double sum_amount = -1;
					
					if (res_sum_amount.next()) {
						sum_amount = res_sum_amount.getDouble("sum_amount");
					}
					if (sum_amount == -1) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sum_amount not found.\n", i-1);
						flags[i - 1] = false;
						return;
					}

					long t7 = System.currentTimeMillis();

					String sql_get_cid = String.format(
						"select o_c_id from order_ where o_w_id = %d and o_d_id = %d and o_id = %d\n",
						this.W_ID, district_no, min_oid);
					if (this.debug) System.out.println(sql_get_cid);
					ResultSet res_cid = this.sqlExecutor.execute(sql_get_cid);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_get_cid failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}
					long t8 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_get_cid", t8 - t7);
					int cid = -1;
					if (res_cid.next()) {
						cid = res_cid.getInt("o_c_id");
					}
					if (cid == -1) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d cid not found.\n", i-1);
						flags[i - 1] = false;
						return;
					}

					long t9 = System.currentTimeMillis();
					String sql_update_customer = String.format(
						"update customer\n" +
						"set c_balance = c_balance + %f, c_delivery_cnt = c_delivery_cnt + 1\n" +
						"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
						sum_amount, this.W_ID, district_no, cid);
					if (this.debug) System.out.println(sql_update_customer);
					this.sqlExecutor.execute(sql_update_customer);
					if (!this.sqlExecutor.suc) {
						if (this.debug) System.out.printf("[DeliveryXact]thread %d sql_update_customer failed.\n", i-1);
						flags[i - 1] = false;
						return;
					}
					long t10 = System.currentTimeMillis();
					if (this.analyze) printTimeInfo("sql_update_customer", t10 - t9);

					long t_end = System.currentTimeMillis();
					if (this.analyze) printTimeInfo(String.format("Thread %d", district_no-1), t_end - t_start); 
					System.out.printf("update district %d succssfully\n", district_no);

				} catch (SQLException e) {
					System.err.println("[DeliveryXact]" + e);
				}


			});
			threads[ii-1].start();
		}
		if (this.debug) {
			for (int i = 0; i < 10; ++i) {
				System.out.println(flags[i]?"true":"false");
			}
			System.out.println("");
		}

		for (int i = 0; i < 10; ++i) {

			try {
				threads[i].join();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
			if (!flags[i]) {
				System.out.printf("thread %d failed\n", i);
				return false;		// some thread(s) failed
			}
		}
		System.out.println("========================================\n");
		return true;
	}

}
