import java.lang.*;
import java.sql.*;
import com.zaxxer.hikari.*;

public class RelatedCustomerXactHandler extends XactHandler {

	private int C_W_ID;
	private int C_D_ID;
	private int C_ID;

	private boolean debug;
	private boolean analyze;


	public RelatedCustomerXactHandler(Connection conn, int cwid, int cdid, int cid) {
		super("RelatedCustomerXact", conn);
		this.C_W_ID = cwid;
		this.C_D_ID = cdid;
		this.C_ID = cid;

		this.debug = false;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {

		System.out.printf("==========[Related Customer Transaction]==========\n");
		System.out.printf("C_W_ID\tC_D_ID\tC_ID\n%d\t%d\t%d\n", this.C_W_ID, this.C_D_ID, this.C_ID);

		String sql_get_related_customer = String.format(
			"with target_orders as \n" +
			"	(select o_w_id, o_d_id, o_id from order_ where o_w_id = %d and o_d_id = %d and o_c_id = %d),\n" +
			"	target_ols as\n" +
			"	(select o_w_id, o_d_id, o_id, ol_i_id \n" +
			"	from target_orders tos join order_line ol\n" +
			"	on ol.ol_w_id = tos.o_w_id and ol.ol_d_id = tos.o_d_id and ol.ol_o_id = tos.o_id),\n" +
			"	common_items as \n" +
			"	(select ol1.o_w_id as o1_w_id, ol1.o_d_id as o1_d_id, ol1.o_id as o1_o_id, \n" +
			"	ol2.ol_w_id as o2_w_id, ol2.ol_d_id as o2_d_id, ol2.ol_o_id as o2_o_id, \n" +
			"	count(*) as common_item_count \n" +
			"	from target_ols ol1 join order_line ol2\n" +
			"	on ol2.ol_w_id != ol1.o_w_id and ol2.ol_i_id = ol1.ol_i_id\n" +
			"	group by o_w_id, o_d_id, o_id, ol_w_id, ol_d_id, ol_o_id)\n" +
			"select o2_w_id as c2_w_id, o2_d_id as c2_d_id, o_c_id as c2_c_id\n" +
			"from common_items ci join order_ o\n" +
			"on o.o_w_id = ci.o2_w_id and o.o_d_id = ci.o2_d_id and o.o_id = ci.o2_o_id\n" +
			"where ci.common_item_count >= 2\n",
			this.C_W_ID, this.C_D_ID, this.C_ID);
		ResultSet res_related_customer = conn.createStatement().executeQuery(sql_get_related_customer);
		System.out.printf("Related Customers:\n");
		while (res_related_customer.next()) {
			int c2_w_id = res_related_customer.getInt("c2_w_id");
			int c2_d_id = res_related_customer.getInt("c2_d_id");
			int c2_c_id = res_related_customer.getInt("c2_c_id");
			System.out.printf("C_W_ID\tC_D_ID\tC_ID\n" + "%d\t%d\t%d\n", c2_w_id, c2_d_id, c2_c_id);
		}

		System.out.println("========================================\n");
	}
}
