
use wholesaledata;

-- District
drop table if exists district1;
create table if not exists district1 (
	d_w_id int not null,
	d_id int not null,
	d_name varchar(10),
	d_street_1 varchar(20),
	d_street_2 varchar(20),
	d_city varchar(20),
	d_state char(2),
	d_zip char(9),
	d_ytd decimal(12,2),

	primary key (d_w_id, d_id),

	family pk (d_w_id, d_id),
	family name (d_name),
	family address (d_street_1, d_street_2, d_city, d_state, d_zip),
	family ytd (d_ytd)
);

import into district1
	CSV DATA(
		'nodelocal://self/vertical_splitted/district-1.csv'
	);

drop table if exists district2;
create table if not exists district2 (
	d_w_id int not null,
	d_id int not null,
	d_tax decimal(4,4),
	d_next_o_id int,

	primary key (d_w_id, d_id),

	family pk (d_w_id, d_id),
	family tax (d_tax),
	family next_o_id (d_next_o_id)
);

import into district2
	CSV DATA(
		'nodelocal://self/vertical_splitted/district-2.csv'
	);

-- Warehouse

drop table if exists warehouse1;
create table if not exists warehouse1 (
	w_id int not null,
	w_name varchar(10),
	w_street_1 varchar(20),
	w_street_2 varchar(20),
	w_city varchar(20),
	w_state char(2),
	w_zip char(9),
	w_tax decimal(4,4),

	primary key (w_id),

	family pk (w_id),
	family name (w_name),
	family address (w_street_1, w_street_2, w_city, w_state, w_zip),
	family tax (w_tax)
);

import into warehouse1
	CSV DATA(
		'nodelocal://self/vertical_splitted/warehouse-1.csv'
	);


drop table if exists warehouse2;
create table if not exists warehouse2 (
	w_id int not null,
	w_ytd decimal(12,2),

	primary key (w_id),

	family pk (w_id),
	family ytd (w_ytd)
);

import into warehouse2
	CSV DATA(
		'nodelocal://self/vertical_splitted/warehouse-2.csv'
	);

-- Customer

drop table if exists customer1;
create table if not exists customer1 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_first varchar(16),
	c_middle char(2),
	c_last varchar(16),
	c_street_1 varchar(20),
	c_street_2 varchar(20),
	c_city varchar(20),
	c_state char(2),
	c_zip char(9),
	c_phone char(16),
	c_since timestamp,
	c_credit_lim decimal(12,2),
	c_discount decimal(4,4),

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family name (c_first, c_middle, c_last),
	family others (c_street_1, c_street_2, c_city, c_state,
		c_zip, c_phone, c_since, c_credit_lim, c_discount)
);

import into customer1
	CSV DATA(
		'nodelocal://self/vertical_splitted/customer-1.csv'
	);

drop table if exists customer2;
create table if not exists customer2 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_balance decimal(12,2),
	c_ytd_payment float,
	c_payment_cnt int,

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family balance (c_balance),
	family payment (c_ytd_payment, c_payment_cnt)
);

import into customer2
	CSV DATA(
		'nodelocal://self/vertical_splitted/customer-2.csv'
	);

drop index if exists balance_idx_cus2;
create index if not exists balance_idx_cus2 on customer2 (c_balance);

drop table if exists customer3;
create table if not exists customer3 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_balance decimal(12,2),
	c_delivery_cnt int,

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family balance (c_balance),
	family delivery_cnt (c_delivery_cnt)
);

import into customer3
	CSV DATA(
		'nodelocal://self/vertical_splitted/customer-3.csv'
	);

-- Order
drop table if exists order1;
create table if not exists order1 (
	o_w_id int not null,
	o_d_id int not null,
	o_id int not null,
	o_c_id int,
	o_ol_cnt decimal(2,0),
	o_all_local decimal(1,0),
	o_entry_d timestamp,

	primary key (o_w_id, o_d_id, o_id),

	family pk (o_w_id, o_d_id, o_id),
	family c_id (o_c_id),
	family entry_d (o_entry_d),
	family others (o_ol_cnt, o_all_local)
);

import into order1
	CSV DATA(
		'nodelocal://self/vertical_splitted/order-1.csv'
	);

drop table if exists order2;
create table if not exists order2 (
	o_w_id int not null,
	o_d_id int not null,
	o_id int not null,
	o_carrier_id int,

	primary key (o_w_id, o_d_id, o_id),

	family pk (o_w_id, o_d_id, o_id),
	family carrier_id (o_carrier_id)
);


import into order2
	CSV DATA(
		'nodelocal://self/vertical_splitted/order-2.csv'
	) with nullif = '';

drop index if exists carrier_idx_order2;
create index if not exists carrier_idx_order2 on order2(o_carrier_id);

-- Item
drop table if exists item1;

create table if not exists item1 (
	i_id int not null,
	i_name varchar(24),
	i_price decimal(5,2),

	primary key (i_id),

	family pk (i_id),
	family info (i_name, i_price)
);

import into item1
	CSV DATA(
		'nodelocal://self/vertical_splitted/item-1.csv'
	);

-- Order-Line
drop table if exists order_line1;
create table if not exists order_line1 (
	ol_w_id int not null,
	ol_d_id int not null,
	ol_o_id int not null,
	ol_number int not null,
	ol_i_id int,
	ol_quantity decimal(2,0),

	primary key (ol_w_id, ol_d_id, ol_o_id, ol_number),

	family pk (ol_w_id, ol_d_id, ol_o_id, ol_number),
	family i_id (ol_i_id),
	family quantity (ol_quantity)
);

drop index if exists item_idx_ol1;
create index if not exists item_idx_ol2 on order_line1 (ol_i_id);

import into order_line1
	CSV DATA(
		'nodelocal://self/vertical_splitted/order-line-1-1.csv',
		'nodelocal://self/vertical_splitted/order-line-1-2.csv'
	);	

drop table if exists order_line2;
create table if not exists order_line2 (
	ol_w_id int not null,
	ol_d_id int not null,
	ol_o_id int not null,
	ol_number int not null,
	ol_delivery_d timestamp,
	ol_amount decimal(6,2),
	ol_supply_w_id int,
	ol_dist_info char(24),

	primary key (ol_w_id, ol_d_id, ol_o_id, ol_number),

	family pk (ol_w_id, ol_d_id, ol_o_id, ol_number),
	family delivery (ol_delivery_d),
	family dist_info (ol_dist_info),
	family others (ol_amount, ol_supply_w_id)
);

import into order_line2
	CSV DATA(
		'nodelocal://self/vertical_splitted/order-line-2-1.csv',
		'nodelocal://self/vertical_splitted/order-line-2-2.csv',
		'nodelocal://self/vertical_splitted/order-line-2-3.csv',
		'nodelocal://self/vertical_splitted/order-line-2-4.csv',
		'nodelocal://self/vertical_splitted/order-line-2-5.csv'
	) with nullif = '';	

-- Stock
drop table if exists stock1;

create table if not exists stock1 (
	s_w_id int not null,
	s_i_id int not null,
	s_quantity decimal(4,0),
	s_ytd decimal(8,2),
	s_order_cnt int,
	s_remote_cnt int,

	primary key (s_w_id, s_i_id),

	family pk (s_w_id, s_i_id),
	family others (s_quantity, s_ytd, s_order_cnt, s_remote_cnt)
);

import into stock1
	CSV DATA(
		'nodelocal://self/vertical_splitted/stock-1.csv'
	);




































