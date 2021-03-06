import java.lang.*;
import java.sql.*;
import java.util.*;
import com.zaxxer.hikari.*;
import static edu.rice.pcdp.PCDP.*;
import java.util.stream.Stream;

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

	public class Item {
		public final int i_id;
		public final String i_name;

		public Item(int iid, String iname) {
			this.i_id = iid;
			this.i_name = iname;
		}
	}

	public class Order_Info {
		public int o_id;
		public int o_c_id;
		public String o_entry_d;

		public Order_Info(int oid, int ocid, String oentryd) {
			this.o_id = oid;
			this.o_c_id = ocid;
			this.o_entry_d = oentryd;
		}
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Popular Item Transaction]==========\n");

		System.out.printf("W_ID\tD_ID\tL\n" + "%d\t%d\t%d\n", this.W_ID, this.D_ID, this.L);

		String sql_get_next_o_id = String.format(
			"select d_next_o_id from district where d_w_id = %d and d_id = %d\n",
			this.W_ID, this.D_ID);
		ResultSet res_next_o_id = conn.createStatement().executeQuery(sql_get_next_o_id);
		int d_next_o_id = -1;
		if (res_next_o_id.next()) {
			d_next_o_id = res_next_o_id.getInt("d_next_o_id");
		}
		if (d_next_o_id == -1) {
			throw new SQLException("[Popular Item Transaction] sql_get_next_o_id failed, d_next_o_id not found");
		}

		String sql_get_orders = String.format(
			"select o_id, o_c_id, o_entry_d from order_ where o_w_id = %d and o_d_id = %d and o_id >= %d and o_id < %d\n",
			this.W_ID, this.D_ID, d_next_o_id - this.L, d_next_o_id);
		ResultSet res_orders = conn.createStatement().executeQuery(sql_get_orders);

		ArrayList<Order_Info> order_info_list = new ArrayList<>();
		while (res_orders.next()) {
			int o_id = res_orders.getInt("o_id");
			int o_c_id = res_orders.getInt("o_c_id");
			String o_entry_d = res_orders.getString("o_entry_d");
			order_info_list.add(new Order_Info(o_id, o_c_id, o_entry_d));
		}

		Set<Integer> order_ids = new HashSet<>();
		Set<Item> popular_items = new HashSet<>();

		long t_loop_start = System.currentTimeMillis();
		int n = order_info_list.size();
		forall(0, n-1, ((i)-> {
			try {
				long t_start = System.currentTimeMillis();

				Order_Info info = order_info_list.get(i);
				int o_id = info.o_id;
				int o_c_id = info.o_c_id;
				String o_entry_d = info.o_entry_d;

				order_ids.add(Integer.valueOf(o_id));

				String sql_get_c_info = String.format(
					"select c_first, c_middle, c_last from customer\n" +
					"where c_w_id = %d and c_d_id = %d and c_id = %d\n",
					this.W_ID, this.D_ID, o_c_id);
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

				String sql_get_max_quantity = String.format(
					"select ol_w_id, ol_d_id, ol_o_id, max(ol_quantity) as max_quantity\n" +
					"from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n" +
					"group by ol_w_id, ol_d_id, ol_o_id\n",
					this.W_ID, this.D_ID, o_id);

				ResultSet res_max_quantity = conn.createStatement().executeQuery(sql_get_max_quantity);
				int max_quantity = -1;
				if (res_max_quantity.next()) {
					max_quantity = res_max_quantity.getInt("max_quantity");
				}
				if (max_quantity == -1) {
					throw new SQLException("[Popular Item Transaction] sql_get_max_quantity failed, max_quantity not found");
				}

				String sql_get_ol_info = String.format(
					"select ol_i_id from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d and ol_quantity = %d\n",
					this.W_ID, this.D_ID, o_id, max_quantity);
				ResultSet res_ol_info = conn.createStatement().executeQuery(sql_get_ol_info);

				while (res_ol_info.next()) {
					int ol_i_id = res_ol_info.getInt("ol_i_id");
					String sql_get_i_name = String.format(
						"select i_name from item where i_id = %d\n", ol_i_id);
					ResultSet res_i_name = conn.createStatement().executeQuery(sql_get_i_name);
					String i_name = "";
					if (res_i_name.next()) {
						i_name = res_i_name.getString("i_name");
					}
					if (i_name == "") {
						throw new SQLException("[Popular Item Transaction] sql_get_i_name failed, i_name not found");
					}
					popular_items.add(new Item(ol_i_id, i_name));
					// Output
					System.out.printf("I_NAME\tOL_QUANTITY\n%s\t%d\n", i_name, max_quantity);
				}
				long t_end = System.currentTimeMillis();
				if (this.analyze) printTimeInfo(String.format("Thread %d", i), t_end - t_start);

			} catch (SQLException e) {
				System.out.println(e);
			}

		}));

		long t_loop_end = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("forall loop", t_loop_end - t_loop_start);

		long t1 = System.currentTimeMillis();
		Item[] popular_item_array = new Item[popular_items.size()];
		popular_items.toArray(popular_item_array);
		int n_pop = popular_item_array.length;
		forallChunked(1, n_pop-1, ((i)->{
			try {
				Item item = popular_item_array[i];
				long t3 = System.currentTimeMillis();
				int count = 0;
				for (int o_id: order_ids) {
					String sql_get_items = String.format(
						"select ol_i_id from order_line where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d\n",
						this.W_ID, this.D_ID, o_id);
					ResultSet res_items = conn.createStatement().executeQuery(sql_get_items);
					while (res_items.next()) {
						int ol_i_id = res_items.getInt("ol_i_id");
						
						if (ol_i_id == item.i_id) {
							count += 1;
							break;
						}
					}
				}
				double percentage = (double)count / (double)this.L * 100;
				String i_name = item.i_name;

				System.out.printf("I_NAME\tPERCENTAGE\n" + "%s\t%f%%\n", i_name, percentage);
				long t4 = System.currentTimeMillis();
				if (this.analyze) printTimeInfo("process item", t4 - t3);

			} catch (SQLException e) {
				System.out.println(e);
			}

		}));
		/*
		Stream.of(popular_item_array)
			.parallel()
			.forEach((item)->{

		});
		*/
		long t2 = System.currentTimeMillis();
		if (this.analyze) printTimeInfo("percentage calculation", t2 - t1);

		System.out.println("========================================\n");

	}

}
