
drop database if exists wholesaledata;
create database if not exists wholesaledata;

use wholesaledata;

-- Tables used to import data
-- Warehouse
CREATE TABLE IF NOT EXISTS warehouse (
	W_ID INT NOT NULL,
	W_NAME VARCHAR(10),
	W_STREET_1 VARCHAR(20),
	W_STREET_2 VARCHAR(20),
	W_CITY VARCHAR(20),
	W_STATE CHAR(2),
	W_ZIP CHAR(9),
	W_TAX DECIMAL(4,4),
	W_YTD DECIMAL(12,2),

	PRIMARY KEY (W_ID),

	FAMILY pk (W_ID),
	FAMILY ytd (W_YTD),
	FAMILY address (W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP),
	FAMILY others (W_NAME, W_TAX)
);

-- District
CREATE TABLE IF NOT EXISTS district (
	D_W_ID INT NOT NULL,
	D_ID INT NOT NULL,
	D_NAME VARCHAR(10),
	D_STREET_1 VARCHAR(20),
	D_STREET_2 VARCHAR(20),
	D_CITY VARCHAR(20),
	D_STATE CHAR(2),
	D_ZIP CHAR(9),
	D_TAX DECIMAL(4,4),
	D_YTD DECIMAL(12,2),
	D_NEXT_O_ID INT,

	PRIMARY KEY (D_W_ID, D_ID),

	FAMILY pk (D_W_ID, D_ID),
	FAMILY ytd (D_YTD),
	FAMILY next_o_id (D_NEXT_O_ID),
	FAMILY address (D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP),
	FAMILY others (D_NAME, D_TAX)
);

-- Customer
CREATE TABLE IF NOT EXISTS customer (
	C_W_ID INT NOT NULL,
	C_D_ID INT NOT NULL,
	C_ID INT NOT NULL,
	C_FIRST VARCHAR(16),
	C_MIDDLE CHAR(2),
	C_LAST VARCHAR(16),
	C_STREET_1 VARCHAR(20),
	C_STREET_2 VARCHAR(20),
	C_CITY VARCHAR(20),
	C_STATE CHAR(2),
	C_ZIP CHAR(9),
	C_PHONE CHAR(16),
	C_SINCE TIMESTAMP,
	C_CREDIT CHAR(2),
	C_CREDIT_LIM DECIMAL(12,2),
	C_DISCOUNT DECIMAL(4,4),
	C_BALANCE DECIMAL(12,2),
	C_YTD_PAYMENT FLOAT,
	C_PAYMENT_CNT INT,
	C_DELIVERY_CNT INT,
	C_DATA VARCHAR(500),

	PRIMARY KEY (C_W_ID, C_D_ID, C_ID),

	FAMILY pk (C_W_ID, C_D_ID, C_ID),
	FAMILY name (C_FIRST, C_MIDDLE, C_LAST),
	FAMILY address (C_STREET_1, C_STREET_2, C_CITY, C_STATE, C_ZIP),
	FAMILY balance (C_BALANCE),
	FAMILY ytd_payment (C_YTD_PAYMENT),
	FAMILY payment_cnt (C_PAYMENT_CNT),
	FAMILY delivery_cnt (C_DELIVERY_CNT),
	FAMILY data (C_DATA),
	FAMILY others (C_PHONE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT)
);

-- Order
CREATE TABLE IF NOT EXISTS order_ (
	O_W_ID INT NOT NULL,
	O_D_ID INT NOT NULL,
	O_ID INT NOT NULL,
	O_C_ID INT,
	O_CARRIER_ID INT,
	O_OL_CNT DECIMAL(2,0),
	O_ALL_LOCAL DECIMAL(1,0),
	O_ENTRY_D TIMESTAMP,

	PRIMARY KEY (O_W_ID, O_D_ID, O_ID),

	FAMILY pk (O_W_ID, O_D_ID, O_ID),
	FAMILY carrier_id (O_CARRIER_ID),
	FAMILY others (O_C_ID, O_OL_CNT, O_ALL_LOCAL, O_ENTRY_D)
);

-- Item
CREATE TABLE IF NOT EXISTS item (
	I_ID INT NOT NULL,
	I_NAME VARCHAR(24),
	I_PRICE DECIMAL(5,2),
	I_IM_ID INT,
	I_DATA VARCHAR(50),

	PRIMARY KEY (I_ID),

	FAMILY pk (I_ID),
	FAMILY info (I_NAME, I_PRICE, I_IM_ID),
	FAMILY data (I_DATA)
);

-- Order-Line
CREATE TABLE IF NOT EXISTS order_line (
	OL_W_ID INT NOT NULL,
	OL_D_ID INT NOT NULL,
	OL_O_ID INT NOT NULL,
	OL_NUMBER INT NOT NULL,
	OL_I_ID INT,
	OL_DELIVERY_D TIMESTAMP,
	OL_AMOUNT DECIMAL(6,2),
	OL_SUPPLY_W_ID INT,
	OL_QUANTITY DECIMAL(2,0),
	OL_DIST_INFO CHAR(24),

	PRIMARY KEY (OL_W_ID, OL_D_ID, OL_O_ID, OL_NUMBER),

	FAMILY pk (OL_W_ID, OL_D_ID, OL_O_ID, OL_NUMBER),
	FAMILY delivery_d (OL_DELIVERY_D),
	FAMILY dist_info (OL_DIST_INFO),
	FAMILY others (OL_I_ID, OL_AMOUNT, OL_SUPPLY_W_ID, OL_QUANTITY)
);

-- Stock
CREATE TABLE IF NOT EXISTS stock (
	S_W_ID INT NOT NULL,
	S_I_ID INT NOT NULL,
	S_QUANTITY DECIMAL(4,0),
	S_YTD DECIMAL(8,2),
	S_ORDER_CNT INT,
	S_REMOTE_CNT INT,
	S_DIST_01 CHAR(24),
	S_DIST_02 CHAR(24),
	S_DIST_03 CHAR(24),
	S_DIST_04 CHAR(24),
	S_DIST_05 CHAR(24),
	S_DIST_06 CHAR(24),
	S_DIST_07 CHAR(24),
	S_DIST_08 CHAR(24),
	S_DIST_09 CHAR(24),
	S_DIST_10 CHAR(24),
	S_DATA VARCHAR(50),

	PRIMARY KEY (S_W_ID, S_I_ID),

	FAMILY pk (S_W_ID, S_I_ID),
	FAMILY quantity (S_QUANTITY),
	FAMILY ytd (S_YTD),
	FAMILY order_cnt (S_ORDER_CNT),
	FAMILY remote_cnt (S_REMOTE_CNT),
	FAMILY dist (S_DIST_01, S_DIST_02, S_DIST_03, S_DIST_04, S_DIST_05, S_DIST_06, S_DIST_07, S_DIST_08, S_DIST_09, S_DIST_10),
	FAMILY data (S_DATA)
);

-- Indexes

-- mainly used by Top Balance Transaction
drop index if exists balance_idx;
create index if not exists balance_idx on customer(c_balance);

-- mainly used by Delivery Transaction
drop index if exists carrier_idx;
create index if not exists carrier_idx on order_(o_carrier_id);

-- mainly used by Related Customer Transaction
drop index if exists item_idx;
create index if not exists item_idx on order_line(ol_i_id);

import into district
	CSV DATA(
		'nodelocal://self/district.csv'
	);

import into order_
	CSV DATA(
		'nodelocal://self/order.csv'
	) with nullif = 'null';

import into warehouse
	CSV DATA(
		'nodelocal://self/warehouse.csv'
	);

import into customer
	CSV DATA(
		'nodelocal://self/customer-1.csv',
		'nodelocal://self/customer-2.csv',
		'nodelocal://self/customer-3.csv',
		'nodelocal://self/customer-4.csv'
	);

import into order_line
	CSV DATA(
		'nodelocal://self/order-line-1.csv',
		'nodelocal://self/order-line-2.csv',
		'nodelocal://self/order-line-3.csv',
		'nodelocal://self/order-line-4.csv',
		'nodelocal://self/order-line-5.csv',
		'nodelocal://self/order-line-6.csv'
	) with nullif = '';

import into item
	CSV DATA(
		'nodelocal://self/item.csv'
	);

import into stock
	CSV DATA(
		'nodelocal://self/stock-1.csv',
		'nodelocal://self/stock-2.csv',
		'nodelocal://self/stock-3.csv',
		'nodelocal://self/stock-4.csv',
		'nodelocal://self/stock-5.csv'
	);


