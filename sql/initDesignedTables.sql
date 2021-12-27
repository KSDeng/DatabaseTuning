
use wholesaledata;

-- District
drop table if exists district1;

create table if not exists district1 (
	d_w_id int not null,
	d_id int not null,
	d_tax decimal(4,4),
	d_street_1 varchar(20),
	d_street_2 varchar(20),
	d_city varchar(20),
	d_state char(2),
	d_zip char(9),

	primary key (d_w_id, d_id),

	family pk (d_w_id, d_id),
	family tax (d_tax),
	family address (d_street_1, d_street_2, d_city, d_state, d_zip)
);

insert into district1
select d_w_id, d_id, d_tax, 
d_street_1, d_street_2, d_city,
d_state, d_zip
from district;

drop table if exists district2;

create table if not exists district2 (
	d_w_id int not null,
	d_id int not null,
	d_next_o_id int,
	d_ytd decimal(12,2),

	primary key (d_w_id, d_id),

	family pk (d_w_id, d_id),
	family others (d_next_o_id, d_ytd)
);

insert into district2
select d_w_id, d_id, d_next_o_id, d_ytd
from district;

-- Warehouse

drop table if exists warehouse1;

create table if not exists warehouse1 (
	w_id int not null,
	w_tax decimal(4,4),
	w_street_1 varchar(20),
	w_street_2 varchar(20),
	w_city varchar(20),
	w_state char(2),
	w_zip char(9),

	primary key (w_id),

	family pk (w_id),
	family tax (w_tax),
	family address (w_street_1, w_street_2, w_city, w_state, w_zip)
);

insert into warehouse1
select w_id, w_tax, w_street_1, w_street_2,
w_city, w_state, w_zip
from warehouse;

drop table if exists warehouse2;

create table if not exists warehouse2 (
	w_id int not null,
	w_ytd decimal(12,2),

	primary key (w_id),

	family pk (w_id),
	family others (w_ytd)
);

insert into warehouse2
select w_id, w_ytd
from warehouse;

-- Customer

drop table if exists customer1;

create table if not exists customer1 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_first varchar(16),
	c_middle char(2),
	c_last varchar(16),
	c_credit char(2),
	c_discount decimal(4,4),
	c_street_1 varchar(20),
	c_street_2 varchar(20),
	c_city varchar(20),
	c_state char(2),
	c_zip char(9),
	c_phone char(16),
	c_since timestamp,
	c_credit_lim decimal(12,2),
	c_w_name varchar(10),
	c_d_name varchar(10),

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family name (c_first, c_middle, c_last),
	family bank (c_credit, c_discount),
	family address (c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_since, c_credit_lim),
	family others (c_w_name, c_d_name)
);

insert into customer1
select c_w_id, c_d_id, c_id,
c_first, c_middle, c_last,
c_credit, c_discount, c_street_1, c_street_2,
c_city, c_state, c_zip, c_phone, c_since, c_credit_lim,
w_name, d_name
from customer c
join warehouse w
on w.w_id = c.c_w_id
join district d
on d.d_w_id = c.c_w_id
and d.d_id = c.c_d_id;

drop table if exists customer2;

create table if not exists customer2 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_balance decimal(12,2),
	c_ytd_payment float,
	c_payment_cnt int,
	c_delivery_cnt int,

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family balance (c_balance),
	family ytd_payment (c_ytd_payment),
	family payment_cnt (c_payment_cnt),
	family delivery_cnt (c_delivery_cnt)
);

insert into customer2
select c_w_id, c_d_id, c_id,
c_balance, c_ytd_payment, c_payment_cnt,
c_delivery_cnt
from customer;

drop table if exists customer3;

create table if not exists customer3 (
	c_w_id int not null,
	c_d_id int not null,
	c_id int not null,
	c_balance decimal(12,2),

	primary key (c_w_id, c_d_id, c_id),

	family pk (c_w_id, c_d_id, c_id),
	family balance (c_balance)
);

insert into customer3
select c_w_id, c_d_id, c_id, c_balance
from customer;

drop index if exists balance_idx;
create index if not exists balance_idx on customer3 (c_balance);

-- Order

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

insert into item1
select i_id, i_name, i_price
from item;

-- Order-Line
drop index if exists item_idx;
create index if not exists item_idx on order_line (ol_i_id);

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

insert into stock1
select s_w_id, s_i_id,
s_quantity, s_ytd, s_order_cnt,
s_remote_cnt
from stock;

-- Customer-Order
drop table if exists customer_order;

create table if not exists customer_order (
	c_w_id int not null, 
	c_d_id int not null,
	c_id int not null,
	o_id int not null,
	o_entry_d timestamp,
	o_carrier_id int,

	primary key (c_w_id, c_d_id, c_id, o_id),

	family cid (c_w_id, c_d_id, c_id),
	family oid (o_id),
	family entry_d (o_entry_d),
	family carrier_id (o_carrier_id)
);

insert into customer_order
select c_w_id, c_d_id, c_id,
o_id, o_entry_d, o_carrier_id
from customer c
join order_ o
on o.o_w_id = c.c_w_id
and o.o_d_id = c.c_d_id
and o.o_c_id = c.c_id;

-- Order_orderLine_item

drop table if exists order_orderLine;

create table if not exists order_orderLine (
	o_w_id int not null,
	o_d_id int not null,
	o_id int not null,
	ol_number int not null,
	o_c_id int,
	o_entry_d timestamp,
	ol_i_id int,
	ol_quantity decimal(2,0),

	primary key (o_w_id, o_d_id, o_id, ol_number),

	family pk (o_w_id, o_d_id, o_id, ol_number),
	family others (o_c_id, o_entry_d, ol_i_id, ol_quantity)
);

insert into order_orderLine
select o_w_id, o_d_id, o_id, ol_number, o_c_id, o_entry_d,
ol_i_id, ol_quantity
from order_ o
join order_line ol
on o.o_w_id = ol.ol_w_id
and o.o_d_id = ol.ol_d_id
and o.o_id = ol.ol_o_id;




































