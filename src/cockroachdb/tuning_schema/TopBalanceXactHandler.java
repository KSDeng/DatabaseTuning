import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class TopBalanceXactHandler extends XactHandler {

	// debug
	private boolean debug;
	private boolean analyze;

	public TopBalanceXactHandler(Connection conn) {
		super("Top Balance Transaction", conn);

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Top Balance Transaction]==========\n");

		String sql_get_top_customers = 
			"select c_balance, c_first, c_middle, c_last, c_w_name, c_d_name \n" +
			"from customer3 c3 join customer1 c1 \n" +
			"on c3.c_w_id = c1.c_w_id and c3.c_d_id = c1.c_d_id and c3.c_id = c1.c_id\n" +
			"order by c_balance desc limit 10;\n";
		if (this.debug) System.out.println(sql_get_top_customers);
		ResultSet res_top_customers = conn.createStatement().executeQuery(sql_get_top_customers);

		while (res_top_customers.next()) {
			double c_balance = res_top_customers.getDouble("c_balance");
			String c_first = res_top_customers.getString("c_first");
			String c_middle = res_top_customers.getString("c_middle");
			String c_last = res_top_customers.getString("c_last");
			String w_name = res_top_customers.getString("c_w_name");
			String d_name = res_top_customers.getString("c_d_name");

			System.out.printf("C_FIRST\tC_MIDDLE\tC_LAST\tC_BALANCE\tW_NAME\tD_NAME\n" + 
				"%s\t%s\t%s\t%f\t%s\t%s\n", c_first, c_middle, c_last, c_balance, w_name, d_name);
		}

		System.out.println("========================================\n");
	}
}
