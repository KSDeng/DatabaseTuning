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

![image-20220115163945777](/Users/kaishengdeng/Library/Application Support/typora-user-images/image-20220115163945777.png)

* SQL Statements



* Multithreading



