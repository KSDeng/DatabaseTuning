
use wholesaledata;

-- District
create table if not exists
district1 as
select d_w_id, d_id, d_tax, 
d_street_1, d_street_2, d_city,
d_state, d_zip
from district;

create table if not exists
district2 as
select d_w_id, d_id, d_next_o_id, d_ytd
from district;

-- Warehouse
create table if not exists
warehouse1 as
select w_id, w_tax, w_street_1, w_street_2,
w_city, w_state, w_zip
from warehouse;

create table if not exists
warehouse2 as
select w_id, w_ytd
from warehouse;

-- Customer
create table if not exists
customer1 as
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

create table if not exists
customer2 as
select c_w_id, c_d_id, c_id,
c_balance, c_ytd_payment, c_payment_cnt,
c_delivery_cnt
from customer;

create table if not exists
customer3 as
select c_w_id, c_d_id, c_id, c_balance
from customer;

-- Order

-- Item
create table if not exists
item1 as
select i_id, i_name
from item;

-- Order-Line

-- Stock
create table if not exists
stock1 as
select s_w_id, s_i_id,
s_quantity, s_ytd, s_order_cnt,
s_remote_cnt
from stock;

-- Customer-Order
create table if not exists
customer_order as
select c_w_id, c_d_id, c_id,
o_id, o_entry_d, o_carrier_id
from customer c
join order_ o
on o.o_w_id = c.c_w_id
and o.o_d_id = c.c_d_id
and o.o_c_id = c.c_id;

-- OrderLine-Stock
create table if not exists
orderLine_stock as
select ol_w_id, ol_d_id, ol_o_id, ol_i_id,
s_quantity
from order_line ol
join stock s
on s.s_w_id = ol.ol_w_id
and s.s_i_id = ol.ol_i_id;





































