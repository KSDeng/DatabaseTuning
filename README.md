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

![image-20220115163945777](/Users/kaishengdeng/Library/Application Support/typora-user-images/image-20220115163945777.png)

![alt text](https://github.com/KSDeng/DatabaseTuning/blob/main/pics/image-20220115163945777.png?raw=true)

I use shortcuts for each transaction, NO for New Order Transaction, DE for Delivery Transaction, OS for Order Status Transaction, PI for Popular Item Transaction and RC for Related Customer Transaction. The red fonts means the specific transaction will write (update/insert) the certain field and the blue fonts mean the transaction will read the certain field.

 

In the original order table we can see that NO, DE, OS, PI and RC share the same table, so I divide the Order table into Order1 and Order2, letting them containing primary key and other certain fields. In this way, only 4 transactions share Order1 table and 3 transactions share Order2 table, and I also create index on o_carrier_id on Order2 table to accelerate Delivery Transaction since I found there will be a lot of filtering on o_carrier_id in DE. 



The same techniques are applied to other tables, and the fields that are either read or written are simply ignored. See the full schema in file Schema_Design.xlsx



* SQL Statements





* Multithreading



