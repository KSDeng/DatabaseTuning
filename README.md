# DatabaseTuning
Database application performance tuning, CockroachDB and Cassandra.



### Project instroduction



The scenario of this project is a database system of e-commerce background.

The initial data consists of 8 tables, including warehouse, region, customer, order, goods, inventory and other information. The tables are related to each other through various foreign keys, the smallest table has 10 rows, the largest table has more than 3 million rows. see project.PDF.



In this project, I use relational database CockroachDB to implement the processing of processing 8 different transactions, and to process 40 user inputs in parallel on 5 servers. Each server processes eight user inputs, each represented by an input file with 20,000 calls to different transactions. 



The eight transactions to be processed include tasks such as order creation, payment, delivery, order status, inventory, hot items, and user information and relationship analysis. 

Workload A and Workload B are divided into two groups: Workload A and Workload B. The former group writes more tasks while the latter reads more.



### Tuning techniques

The tuning includes 3 aspects, schema design, SQL statements tuning and multithreading.



* Schema Design

The schema design mainly focuses on reducing contention. I list all the fields that will be read/write in respect of each table, and try to divide 1 table into 2 or more to reducing the number of xacts that share the same table.

Take order table for example.

![alt text](https://github.com/KSDeng/DatabaseTuning/blob/main/pics/image-20220115163945777.png?raw=true)

I use shortcuts for each transaction, NO for New Order Transaction, DE for Delivery Transaction, OS for Order Status Transaction, PI for Popular Item Transaction and RC for Related Customer Transaction. The red fonts means the specific transaction will write (update/insert) the certain field and the blue fonts mean the transaction will read the certain field.

 

In the original order table we can see that NO, DE, OS, PI and RC share the same table, so I divide the Order table into Order1 and Order2, letting them containing primary key and other certain fields. In this way, only 4 transactions share Order1 table and 3 transactions share Order2 table, and I also create index on o_carrier_id on Order2 table to accelerate Delivery Transaction since I found there will be a lot of filtering on o_carrier_id in DE. 



The same techniques are applied to other tables, and the fields that are either read or written are simply ignored. See the full schema in file Schema_Design.xlsx



* SQL Statements

The second important technique is to write faster SQL statements, take Popular Item Transaction for example, the following SQL statement is used to **get the item name and the quantity of the popular items for each order that is considered**.

```sql
with sub_order_line as
	(select ol_w_id, ol_d_id, ol_o_id, ol_i_id, ol_quantity from order_line1
		where ol_w_id = %d and ol_d_id = %d and ol_o_id = %d),
	pop_items as
	(select ol1.ol_w_id, ol1.ol_d_id, ol1.ol_o_id, ol1.ol_i_id, ol2.max_quantity from
		sub_order_line ol1
		join (select ol_w_id, ol_d_id, ol_o_id, max(ol_quantity) as max_quantity
		from sub_order_line
		group by ol_w_id, ol_d_id, ol_o_id) ol2
		on ol1.ol_w_id = ol2.ol_w_id and ol1.ol_d_id = ol2.ol_d_id and ol1.ol_quantity = ol2.max_quantity)
select i_name, max_quantity from pop_items join item1 on ol_i_id = i_id;
```

By using this kind of SQL statements, **the amount of SQL statements executed to finish certain task is reduced to the minimum**, therefore, the overhead of submitting tasks to the Database through the driver is minimized.



Another way to write more efficient SQL is to **improve the execution plan to minimize the amount of data in the join process**. Take the Related Customer Transaction for example.

```sql
with target_orders as
	(select o_w_id, o_d_id, o_id from order1 where o_w_id = %d and o_d_id = %d and o_c_id = %d),
	target_ols as
	(select o_w_id, o_d_id, o_id, ol_i_id
		from target_orders tos join order_line1 ol
		on ol.ol_w_id = tos.o_w_id and ol.ol_d_id = tos.o_d_id and ol.ol_o_id = tos.o_id),
	common_items as 
	(select ol1.o_w_id as o1_w_id, ol1.o_d_id as o1_d_id, ol1.o_id as o1_o_id, 
		ol2.ol_w_id as o2_w_id, ol2.ol_d_id as o2_d_id, ol2.ol_o_id as o2_o_id, 
		count(*) as common_item_count
		from target_ols ol1 join order_line1 ol2
		on ol2.ol_w_id != ol1.o_w_id and ol2.ol_i_id = ol1.ol_i_id
		group by o_w_id, o_d_id, o_id, ol_w_id, ol_d_id, ol_o_id)
select o2_w_id as c2_w_id, o2_d_id as c2_d_id, o_c_id as c2_c_id
from common_items ci join order1 o
on o.o_w_id = ci.o2_w_id and o.o_d_id = ci.o2_d_id and o.o_id = ci.o2_o_id
where ci.common_item_count >= 2;
```

In this transaction, we input a customer's identifier, and need to find the customers that near different warehouse and have bought at least two items in common.

Here are some observations,

1. We don't need to use the Customer table since the Order table have all we need (o_w_id, o_d_id, o_c_id)
2. We need to join the order_line table with itself to find items in common, which can be very time-consuming since order_line table is very huge (more than 3 million rows)

In conclusion we need to do the following steps,

![](https://github.com/KSDeng/DatabaseTuning/blob/main/pics/image-20220115233934014.png?raw=true)



The first join is to find the items that the input customer has bought, the second join is to find the orders that have at least 2 items in common, the third join is to get the identifier of the customers who order these orders.

**The optimization of the Execution plan is that the data used can be filtered prior to each stage, greatly reducing the amount of data in the join process.**



* Proper Indexes

Using the right indexes can significantly improve efficiency. In the project I used `explain` statements to analyze the SQL execution plans and found potential ways in which indexing could greatly improve efficiency. Finally I decided to create 3 indexes,

1. Index on `order_line (ol_i_id)`, which greatly improves the efficiency of Related Customer Transaction
2. Index on `customer (c_balance)`, which greatly improves the efficiency of Top Balance Transaction
3. Index on `order (o_carrier_id)`, which greatly improves the efficiency of Delivery Transaction

Take related customer transaction for example,

The execution plan of the SQL statements is like the following,

```
  • lookup join
  │ estimated row count: 53
  │ table: order_@primary
  │ equality: (o2_w_id, o2_d_id, o2_o_id) = (o_w_id,o_d_id,o_id)
  │ equality cols are key
  │
  └── • render
      │ estimated row count: 53
      │
      └── • filter
          │ estimated row count: 53
          │ filter: count_rows >= 2
          │
          └── • group
              │ estimated row count: 159
              │ group by: o_id, ol_w_id, ol_d_id, ol_o_id
              │
              └── • hash join
                  │ estimated row count: 159
                  │ equality: (ol_i_id) = (ol_i_id)
                  │ pred: ol_w_id != o_w_id
                  │
                  ├── • scan
                  │     estimated row count: 3,748,849 (100% of the table; stats collected 7 minutes ago)
                  │     table: order_line@primary
                  │     spans: FULL SCAN
                  │
                  └── • render
                      │ estimated row count: 13
                      │
                      └── • lookup join
                          │ estimated row count: 13
                          │ table: order_line@primary
                          │ equality: (o_w_id, o_d_id, o_id) = (ol_w_id,ol_d_id,ol_o_id)
                          │
                          └── • render
                              │ estimated row count: 1
                              │
                              └── • filter
                                  │ estimated row count: 1
                                  │ filter: o_c_id = 1875
                                  │
                                  └── • scan
                                        estimated row count: 3,035 (1.0% of the table; stats collected 10 minutes ago)
                                        table: order_@primary
                                        spans: [/5/1 - /5/1]
```

We can find in this execution plan there is a full scan on the `order_line` table，followed by a hash join process using `ol_i_id` and`ol_w_id` . Since `ol_w_id	` is already primary key, creating an index on `ol_i_id` will greatly increase the efficiency of this join process.

Below is the new execution plan after creating index on `ol_i_id`,

```
  • lookup join
  │ estimated row count: 53
  │ table: order_@primary
  │ equality: (o2_w_id, o2_d_id, o2_o_id) = (o_w_id,o_d_id,o_id)
  │ equality cols are key
  │
  └── • render
      │ estimated row count: 53
      │
      └── • filter
          │ estimated row count: 53
          │ filter: count_rows >= 2
          │
          └── • group
              │ estimated row count: 159
              │ group by: o_id, ol_w_id, ol_d_id, ol_o_id
              │
              └── • lookup join
                  │ estimated row count: 159
                  │ table: order_line@item_idx
                  │ equality: (ol_i_id) = (ol_i_id)
                  │ pred: ol_w_id != o_w_id
                  │
                  └── • render
                      │ estimated row count: 13
                      │
                      └── • lookup join
                          │ estimated row count: 13
                          │ table: order_line@primary
                          │ equality: (o_w_id, o_d_id, o_id) = (ol_w_id,ol_d_id,ol_o_id)
                          │
                          └── • render
                              │ estimated row count: 1
                              │
                              └── • filter
                                  │ estimated row count: 1
                                  │ filter: o_c_id = 1875
                                  │
                                  └── • scan
                                        estimated row count: 3,035 (1.0% of the table; stats collected 19 minutes ago)
                                        table: order_@primary
                                        spans: [/5/1 - /5/1]
```

 After creating the index on `order_line(ol_i_id)`, the full scan on `order_line` table is eliminated, which greatly increase the efficiency.



* Multithreading



