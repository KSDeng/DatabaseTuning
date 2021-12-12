./truncate_newOrderInfoTable.sh xcnd30
java -cp .:libs/postgresql.jar:libs/commons-math3.jar mainDriver $1 $2
