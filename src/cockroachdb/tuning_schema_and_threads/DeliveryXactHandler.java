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
		super("Delivery Transaction", conn);
		this.W_ID = w_id;
		this.CARRIER_ID = carrier_id;

		this.debug = true;
		this.analyze = false;
	}

	@Override
	void process() throws SQLException {
		System.out.printf("==========[Delivery Transaction]==========\n");

		/*String sql_select_order = String.format(
			"with subquery as\n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2\n" +
			"		where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id)\n" +
			"select o_carrier_id from order2\n" +
			"where order2.o_w_id = subquery.o_w_id\n" +
			"and order2.o_d_id = subquery.o_d_id\n" +
			"and order2.o_id = subquery.min_oid;\n", this.CARRIER_ID);
		if (this.debug) System.out.println(sql_select_order);
		ResultSet res_order = conn.createStatement().executeQuery(sql_select_order);
		if (res_order.next()) {*/
			String sql_select_order_for_update = String.format(
				"with subquery as\n" +
				"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
				"		from order2\n" +
				"		where o_w_id = %d and o_carrier_id is null\n" +
				"		group by o_w_id, o_d_id, o_carrier_id)\n" +
				"select order2.o_carrier_id from order2 join subquery\n" +
				"on order2.o_w_id = subquery.o_w_id\n" +
				"and order2.o_d_id = subquery.o_d_id\n" +
				"and order2.o_id = subquery.min_oid\n" +
				"for update;\n",
				this.CARRIER_ID);
			if (this.debug) System.out.println(sql_select_order_for_update);
			conn.createStatement().execute(sql_select_order_for_update);

			String sql_update_order = String.format(
				"update order2 set o_carrier_id = %d\n" +
				"from (select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
				"		from order2\n" +
				"		where o_w_id = %d and o_carrier_id is null\n" +
				"		group by o_w_id, o_d_id, o_carrier_id) as subquery \n" +
				"where order2.o_w_id = subquery.o_w_id\n" +
				"and order2.o_d_id = subquery.o_d_id\n" +
				"and order2.o_id = subquery.min_oid;\n", this.CARRIER_ID, this.W_ID);
			if (this.debug) System.out.println(sql_update_order);
			conn.createStatement().executeUpdate(sql_update_order);
		//}

		String sql_select_ol_for_update = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id),\n" +
			"	subquery as \n" +
			"	(select ol_w_id, ol_d_id, ol_o_id, ol_number \n" +
			"		from orders join (select ol_w_id, ol_d_id, ol_o_id, ol_number from order_line2 where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id and ol.ol_d_id = orders.o_d_id\n" + 
			"		and ol.ol_o_id = orders.min_oid)\n" +
			"select ol_delivery_d from order_line2 join subquery\n" +
			"on order_line2.ol_w_id = subquery.ol_w_id\n" +
			"and order_line2.ol_d_id = subquery.ol_d_id\n" +
			"and order_line2.ol_o_id = subquery.ol_o_id\n" +
			"and order_line2.ol_number = subquery.ol_number\n" +
			"for update;\n",
			this.W_ID, this.W_ID);
		if (this.debug) System.out.println(sql_select_ol_for_update);
		conn.createStatement().execute(sql_select_ol_for_update);
			
		String sql_update_ol = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id)\n" +
			"update order_line2 \n" +
			"set ol_delivery_d = current_timestamp\n" +
			"from (select ol_w_id, ol_d_id, ol_o_id, ol_number \n" +
			"		from orders join (select ol_w_id, ol_d_id, ol_o_id, ol_number from order_line2 where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id and ol.ol_d_id = orders.o_d_id\n" + 
			"		and ol.ol_o_id = orders.min_oid) as subquery\n" +
			"where order_line2.ol_w_id = subquery.ol_w_id\n" +
			"and order_line2.ol_d_id = subquery.ol_d_id\n" +
			"and order_line2.ol_o_id = subquery.ol_o_id\n" +
			"and order_line2.ol_number = subquery.ol_number\n", this.W_ID, this.W_ID);
		if (this.debug) System.out.println(sql_update_ol);
		conn.createStatement().executeUpdate(sql_update_ol);

		String sql_select_customer2_for_update = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id),\n" +
			"item_amount as \n" +
			"	(select o_w_id, o_d_id, min_oid as o_id, sum(ol_amount) as sum_amount\n" +
			"		from orders join\n" + 
			"					(select ol_w_id, ol_d_id, ol_o_id, ol_amount \n" +
			"					from order_line2 where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id\n" +
			"		and ol.ol_d_id = orders.o_d_id\n" +
			"		and ol.ol_o_id = orders.min_oid\n" +
			"	group by o_w_id, o_d_id, min_oid),\n" +
			"subquery as \n" +
			"	(select o2.o_w_id, o2.o_d_id, o_c_id, sum_amount \n" +
			"	from item_amount ia join \n"+
			"						(select o_w_id, o_d_id, o_id, o_c_id from order2\n" +
			"						where o_w_id = %d) o2\n" +
			"	on o2.o_w_id = ia.o_w_id\n" +
			"	and o2.o_d_id = ia.o_d_id\n" +
			"	and o2.o_id = ia.o_id)\n" +
			"select c_balance from customer2 join subquery\n" +
			"on c_w_id = subquery.o_w_id\n" +
			"and c_d_id = subquery.o_d_id\n" +
			"and c_id = subquery.o_c_id\n" +
			"for update;\n", this.W_ID, this.W_ID, this.W_ID);
		if (this.debug) System.out.println(sql_select_customer2_for_update);
		conn.createStatement().execute(sql_select_customer2_for_update);

		String sql_select_customer3_for_update = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id),\n" +
			"item_amount as \n" +
			"	(select o_w_id, o_d_id, min_oid as o_id, sum(ol_amount) as sum_amount\n" +
			"		from orders join\n" + 
			"					(select ol_w_id, ol_d_id, ol_o_id, ol_amount \n" +
			"					from order_line2 where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id\n" +
			"		and ol.ol_d_id = orders.o_d_id\n" +
			"		and ol.ol_o_id = orders.min_oid\n" +
			"	group by o_w_id, o_d_id, min_oid),\n" +
			"subquery as \n" +
			"	(select o2.o_w_id, o2.o_d_id, o_c_id, sum_amount \n" +
			"	from item_amount ia join \n"+
			"						(select o_w_id, o_d_id, o_id, o_c_id from order2\n" +
			"						where o_w_id = %d) o2\n" +
			"	on o2.o_w_id = ia.o_w_id\n" +
			"	and o2.o_d_id = ia.o_d_id\n" +
			"	and o2.o_id = ia.o_id)\n" +
			"select c_balance, c_delivery_cnt from customer3 join subquery\n" +
			"on c_w_id = subquery.o_w_id\n" +
			"and c_d_id = subquery.o_d_id\n" +
			"and c_id = subquery.o_c_id\n" +
			"for update;\n", this.W_ID, this.W_ID, this.W_ID);
		if (this.debug) System.out.println(sql_select_customer3_for_update);
		conn.createStatement().execute(sql_select_customer3_for_update);

		String sql_update_customer2 = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id),\n" +
			"item_amount as \n" +
			"	(select o_w_id, o_d_id, min_oid as o_id, sum(ol_amount) as sum_amount\n" +
			"		from orders join\n" + 
			"					(select ol_w_id, ol_d_id, ol_o_id, ol_amount \n" +
			"					from order_line2 where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id\n" +
			"		and ol.ol_d_id = orders.o_d_id\n" +
			"		and ol.ol_o_id = orders.min_oid\n" +
			"	group by o_w_id, o_d_id, min_oid)\n" +
			"update customer2 set c_balance = c_balance + subquery.sum_amount\n" +
			"from \n" +
			"	(select o2.o_w_id, o2.o_d_id, o_c_id, sum_amount \n" +
			"	from item_amount ia join \n"+
			"						(select o_w_id, o_d_id, o_id, o_c_id from order2\n" +
			"						where o_w_id = %d) o2\n" +
			"	on o2.o_w_id = ia.o_w_id\n" +
			"	and o2.o_d_id = ia.o_d_id\n" +
			"	and o2.o_id = ia.o_id) as subquery\n" +
			"where c_w_id = subquery.o_w_id\n" +
			"and c_d_id = subquery.o_d_id\n" +
			"and c_id = subquery.o_c_id;\n", this.W_ID, this.W_ID, this.W_ID);

		String sql_update_customer3 = String.format(
			"with orders as \n" +
			"	(select o_w_id, o_d_id, o_carrier_id, min(o_id) as min_oid\n" +
			"		from order2 where o_w_id = %d and o_carrier_id is null\n" +
			"		group by o_w_id, o_d_id, o_carrier_id),\n" +
			"item_amount as \n" +
			"	(select o_w_id, o_d_id, min_oid as o_id, sum(ol_amount) as sum_amount\n" +
			"		from orders join\n" + 
			"					(select ol_w_id, ol_d_id, ol_o_id, ol_amount \n" +
			"					from order_line where ol_w_id = %d) ol\n" +
			"		on ol.ol_w_id = orders.o_w_id\n" +
			"		and ol.ol_d_id = orders.o_d_id\n" +
			"		and ol.ol_o_id = orders.min_oid\n" +
			"	group by o_w_id, o_d_id, min_oid)\n" +
			"update customer3 set c_balance = c_balance + subquery.sum_amount,\n" +
			"					c_delivery_cnt = c_delivery_cnt + 1\n" +
			"from \n" +
			"	(select o2.o_w_id, o2.o_d_id, o_c_id, sum_amount \n" +
			"	from item_amount ia join \n"+
			"						(select o_w_id, o_d_id, o_id, o_c_id from order2\n" +
			"						where o_w_id = %d) o2\n" +
			"	on o2.o_w_id = ia.o_w_id\n" +
			"	and o2.o_d_id = ia.o_d_id\n" +
			"	and o2.o_id = ia.o_id) as subquery\n" +
			"where c_w_id = subquery.o_w_id\n" +
			"and c_d_id = subquery.o_d_id\n" +
			"and c_id = subquery.o_c_id;\n", this.W_ID, this.W_ID, this.W_ID);

		String sql_update_customer = sql_update_customer2 + sql_update_customer3;
		if (this.debug) System.out.println(sql_update_customer);
		conn.createStatement().executeUpdate(sql_update_customer);
		System.out.println("========================================\n");
	}

}
