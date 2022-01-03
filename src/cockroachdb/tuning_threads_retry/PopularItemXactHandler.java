import java.lang.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import com.zaxxer.hikari.*;

public class PopularItemXactHandler extends XactHandler {
	// inputs
	private int W_ID;
	private int D_ID;
	private int L;

	// debug
	private boolean debug;
	private boolean analyze;

	public PopularItemXactHandler(Connection conn, int wid, int did, int l) {
		super("PopuarItemXact", conn);
		this.W_ID = wid;
		this.D_ID = did;
		this.L = l;

		this.debug = false;
		this.analyze = false;
	}

	public class PopularItemPercInfo {
		String i_name;
		double percentage;

		public PopularItemPercInfo(String name, double perc) {
			this.i_name = name;
			this.percentage = perc;
		}
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Popular Item Transaction]==========\n");
		System.out.printf("W_ID\tD_ID\tL\n" + "%d\t%d\t%d\n", this.W_ID, this.D_ID, this.L);

		String sql_get_next_o_id = String.format(
			"select d_next_o_id from district where d_w_id = %d and d_id = %d\n",
			this.W_ID, this.D_ID);
		if (this.debug) System.out.println(sql_get_next_o_id);
		ResultSet res_next_o_id = conn.createStatement().executeQuery(sql_get_next_o_id);
		int d_next_o_id = -1;
		if (res_next_o_id.next()) {
			d_next_o_id = res_next_o_id.getInt("d_next_o_id");
		}
		if (d_next_o_id == -1) {
			throw new SQLException("[Popular Item Transaction] sql_get_next_o_id failed, d_next_o_id not found");
		}

		final int d_next_o_id_f = d_next_o_id;
		FutureTask<ArrayList<PopularItemPercInfo>> ft_popular_item_perc = new FutureTask<ArrayList<PopularItemPercInfo>>(()->{
			try {
				long t3 = System.currentTimeMillis();

				String sql_calculate_pop_percentage = String.format(
					"with sub_order as\n" +
					"	(select o_w_id, o_d_id, o_id from order_ where o_w_id = %d and o_d_id = %d and o_id >= %d and o_id < %d),\n" +
					"sub_order_line as\n" +
					"	(select ol_w_id, ol_d_id, ol_o_id, ol_i_id, ol_quantity\n" +
					"	from sub_order join \n" +
					"		(select ol_w_id, ol_d_id, ol_o_id, ol_i_id, ol_quantity from order_line\n" +
					"		where ol_w_id = %d and ol_d_id = %d) ol\n" +
					"	on o_w_id = ol.ol_w_id and o_d_id = ol.ol_d_id and o_id = ol.ol_o_id),\n" +
					"pop_items as\n" +
					"	(select ol1.ol_w_id, ol1.ol_d_id, ol1.ol_o_id, ol1.ol_i_id from\n" +
					"	sub_order_line ol1\n" +
					"	join (select ol_w_id, ol_d_id, ol_o_id, max(ol_quantity) as max_quantity\n" +
					"			from sub_order_line\n" +
					"			group by ol_w_id, ol_d_id, ol_o_id) ol2\n" +
					"	on ol1.ol_w_id = ol2.ol_w_id and ol1.ol_d_id = ol2.ol_d_id and ol1.ol_o_id = ol2.ol_o_id\n" +
					"		and ol1.ol_quantity = ol2.max_quantity)\n" +
					"select ol_i_id, count(*)/(select count(*) from sub_order) as perc\n" +
					"from sub_order join\n" +
					"	(select ol1.ol_w_id, ol1.ol_d_id, ol1.ol_o_id, ol1.ol_i_id\n" +
					"		from order_line ol1 join (select distinct(ol_i_id) from pop_items) pi\n" +
					"		on ol1.ol_i_id = pi.ol_i_id) ol\n" +
					"on o_w_id = ol_w_id and o_d_id = ol_d_id and o_id = ol_o_id\n" +
					"group by ol_i_id\n",
					this.W_ID, this.D_ID, d_next_o_id_f - this.L, d_next_o_id_f, this.W_ID, this.D_ID);
				if (this.debug) System.out.println(sql_calculate_pop_percentage);
				ResultSet res_pop_percentage = conn.createStatement().executeQuery(sql_calculate_pop_percentage);
				long t4 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("sql_calculate_pop_percentage", t4 - t3);

				ArrayList<PopularItemPercInfo> pop_item_perc_array = new ArrayList<>();
				while (res_pop_percentage.next()) {
					int ol_i_id = res_pop_percentage.getInt("ol_i_id");
					double perc = res_pop_percentage.getDouble("perc");
					String sql_get_i_name_perc = String.format(
						"select i_name from item where i_id = %d\n", ol_i_id);
					ResultSet res_i_name = conn.createStatement().executeQuery(sql_get_i_name_perc);
					String i_name = "";
					if (res_i_name.next()) {
						i_name = res_i_name.getString("i_name");
					}
					if (i_name == "") {
						throw new SQLException("[Popular Item Transaction] sql_get_i_name_perc failed, i_name not found");
					}

					pop_item_perc_array.add(new PopularItemPercInfo(i_name, perc));
				}

				long t5 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("future task", t5 - t3);

				return pop_item_perc_array;

			} catch(SQLException e) {
				System.out.println(e);
			}
			return null;
		});

		Thread t_popular_item_percentage = new Thread(ft_popular_item_perc);
		t_popular_item_percentage.start();

		String sql_get_orders = String.format(
			"select o_id, o_c_id, o_entry_d from order_ where o_w_id = %d and o_d_id = %d and o_id >= %d and o_id < %d\n",
			this.W_ID, this.D_ID, d_next_o_id - this.L, d_next_o_id);
		if (this.debug) System.out.println(sql_get_orders);
		ResultSet res_orders = conn.createStatement().executeQuery(sql_get_orders);

		long t_loop_start = System.currentTimeMillis();

		while (res_orders.next()) {
			int o_id = res_orders.getInt("o_id");
			int o_c_id = res_orders.getInt("o_c_id");
			String o_entry_d = res_orders.getString("o_entry_d");

			String sql_get_c_info = String.format(
				"select c_first, c_middle, c_last from customer\n" +
				"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
				this.W_ID, this.D_ID, o_c_id);
			if (this.debug) System.out.println(sql_get_c_info);
			ResultSet res_c_info = conn.createStatement().executeQuery(sql_get_c_info);
			String c_first = "", c_middle = "", c_last = "";
			if (res_c_info.next()) {
				c_first = res_c_info.getString("c_first");
				c_middle = res_c_info.getString("c_middle");
				c_last = res_c_info.getString("c_last");
			}
			// Output
			System.out.printf("O_ID\tO_ENTRY_D\tC_FIRST\tC_MIDDLE\tC_LAST\n" +
				"%d\t%s\t%s\t%s\t%s\n", o_id, o_entry_d, c_first, c_middle, c_last);

			long t1 = System.currentTimeMillis();
			String sql_get_popular_items = String.format(
				"with sub_order_line as\n" +
				"	(select ol_w_id, ol_d_id, ol_o_id, ol_i_id, ol_quantity from order_line\n" +
				"		where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d),\n" +
				"	pop_items as\n" +
				"	(select ol1.ol_w_id, ol1.ol_d_id, ol1.ol_o_id, ol1.ol_i_id, ol2.max_quantity from \n" +
				"		sub_order_line ol1\n" +
				"		join (select ol_w_id, ol_d_id, ol_o_id, max(ol_quantity) as max_quantity\n" +
				"			from sub_order_line\n" +
				"			group by ol_w_id, ol_d_id, ol_o_id) ol2\n" +
				"		on ol1.ol_w_id = ol2.ol_w_id and ol1.ol_d_id = ol2.ol_d_id and ol1.ol_quantity = ol2.max_quantity)\n" +
				"select i_name, max_quantity from pop_items join item on ol_i_id = i_id\n",
				this.W_ID, this.D_ID, o_id);
			if (this.debug) System.out.println(sql_get_popular_items);
			ResultSet res_pop_items = conn.createStatement().executeQuery(sql_get_popular_items);
			long t2 = System.currentTimeMillis();
			if (this.analyze) printTimeInfo("sql_get_popular_items", t2 - t1);

			System.out.printf("Popular Items:\n");
			while (res_pop_items.next()) {
				String i_name = res_pop_items.getString("i_name");
				int max_quantity = res_pop_items.getInt("max_quantity");
				// Output
				System.out.printf("I_NAME\tOL_QUANTITY\n%s\t%d\n", i_name, max_quantity);
			}
		}
		long t_loop_end = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("popular item loop", t_loop_end - t_loop_start);

		// Output percentage
		ArrayList<PopularItemPercInfo> pop_item_perc_info = null;
		try {
			pop_item_perc_info = ft_popular_item_perc.get();
		} catch (InterruptedException e) {
			System.err.println(e);
		} catch (ExecutionException e) {
			System.err.println(e);
		}
		if (pop_item_perc_info == null) {
			throw new SQLException("ft_popular_item_perc failed, get null return value");
		}

		if (this.debug) System.out.printf("perc length:%d\n", pop_item_perc_info.size());
		for (PopularItemPercInfo pop_info: pop_item_perc_info) {
			String i_name = pop_info.i_name;
			double perc = pop_info.percentage;
			System.out.printf("I_NAME\tPERCENTAGE\n%s\t%f%%\n", i_name, perc * 100);
		}

		System.out.println("========================================\n");

	}

}
