import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class TopBalanceXactHandler extends XactHandler {

	// debug
	private boolean debug;
	private boolean analyze;

	public TopBalanceXactHandler(Connection conn) {
		super("TopBalanceXact", conn);

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() {
		System.out.printf("==========[Top Balance Transaction]==========\n");

		try {

			String sql_get_top_customers = "select c_w_id, c_d_id, c_balance, c_first, c_middle, c_last \n" + 
				"from customer order by c_balance desc limit 10\n";
			ResultSet res_top_customers = conn.createStatement().executeQuery(sql_get_top_customers);

			while (res_top_customers.next()) {
				int c_w_id = res_top_customers.getInt("c_w_id");
				int c_d_id = res_top_customers.getInt("c_d_id");
				double c_balance = res_top_customers.getDouble("c_balance");
				String c_first = res_top_customers.getString("c_first");
				String c_middle = res_top_customers.getString("c_middle");
				String c_last = res_top_customers.getString("c_last");

				String sql_get_w_name = String.format("select w_name from warehouse where w_id = %d\n", c_w_id);
				ResultSet res_w_name = conn.createStatement().executeQuery(sql_get_w_name);
				String w_name = "";
				if (res_w_name.next()) {
					w_name = res_w_name.getString("w_name");
				}

				String sql_get_d_name = String.format("select d_name from district where d_w_id = %d and d_id = %d\n",
					c_w_id, c_d_id);
				ResultSet res_d_name = conn.createStatement().executeQuery(sql_get_d_name);
				String d_name = "";
				if (res_d_name.next()) {
					d_name = res_d_name.getString("d_name");
				}

				System.out.printf("C_FIRST\tC_MIDDLE\tC_LAST\tC_BALANCE\tW_NAME\tD_NAME\n" + 
					"%s\t%s\t%s\t%f\t%s\t%s\n", c_first, c_middle, c_last, c_balance, w_name, d_name);

			}
			
		} catch (SQLException e) {
			System.out.println(e);
		}

		System.out.println("========================================\n");
	}
}
