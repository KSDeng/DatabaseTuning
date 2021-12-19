
. /temp/DatabaseTuning/shell/common.sh           # import global variables

if [ $1 == "--sql" ];then
	echo "execute sql"
	${cockroachPath} sql --insecure --host=xcnd35 --execute="$2"
elif [ $1 == "--sqlfromfile" ];then
	echo "execute sql from file"
	${cockroachPath} sql --insecure --host=xcnd35 --file=$2
elif [ $1 == "--nodestatus" ];then
	echo "show node status"
	${cockroachPath} node status --insecure --host=xcnd35
elif [ $1 == "--startCockroachDB" ];then
	echo "starting cockroachDB..."
	${projectRootPath}/shell/startCockroachDB.sh $2
elif [ $1 == "--copyDataToCockroachDB" ];then
	echo "copying data to cockroachDB..."
	${projectRootPath}/shell/copy_data_to_cockroach_fs.sh $2
elif [ $1 == "--refreshCockroachDBData" ];then
	echo "importing data to cockroachDB..."
	refreshDataScript=/temp/DatabaseTuning/sql/refreshData.sql
	${cockroachPath} sql --insecure --host=xcnd35 --file=${refreshDataScript}
elif [ $1 == "--help" ];then
	echo "Options:"
	echo "	--sql				Execute sql command 			(param1: sql command you want to execute)"
	echo "	--sqlfromfile			Execute sql command from file 		(param1: sql file you want to execute)"
	echo "	--nodestatus			Show node status"
	echo "	--startCockroachDB		Start cockroachDB cluster		(param1: node number, 1,2,3,4,5 for xcnd35-xcnd39 respectively, must be execute in xcnd34 lastly)"
	echo "	--copyDataToCockroachDB		Copy data files to cockroachDB file system, must start cockroachDB cluster first		(param1: node number, 1,2,3,4,5 for xcnd35-xcnd34 respectively)"
	echo "	--refreshCockroachDBData	Refresh the data in cockroachDB, must copy data files to cockroachDB file system first"
fi
