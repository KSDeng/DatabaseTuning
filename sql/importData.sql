
-- Import data from csv into CockroachDB
-- You are supposed to finish the following things before running this script:
-- 1. Create database
-- 2. Create right schema of tables
-- 3. Guarantee all the tables are empty, or there may be primary key conflicts
-- 4. Move all the data files into node{n}/extern directory of EVERY nodes (n = 1, 2, 3, 4, 5)

use wholesaledata;

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


