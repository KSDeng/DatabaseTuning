
. ./shell/common.sh           # import global variables

if [ $1 == "--sql" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "execute sql"
	${cockroachPath} sql --insecure --host=xcnd35 --execute="$2"
elif [ $1 == "--sqlfromfile" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "execute sql from file"
	${cockroachPath} sql --insecure --host=xcnd35 --file=$2
elif [ $1 == "--nodestatus" ];then
	echo "show node status"
	${cockroachPath} node status --insecure --host=xcnd35
elif [ $1 == "--startCockroachDB" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "starting cockroachDB..."
	${projectRootPath}/shell/startCockroachDB.sh $2
elif [ $1 == "--startAllCockroachDB" ];then
	for ((c = 0; c < 5; c++))
	do
		server_no=$(expr $c + 5)
		para=$(expr $c + 1)
		ssh kaisheng@xcnd3${server_no}.comp.nus.edu.sg "${projectRootPath}/shell/startCockroachDB.sh $para &"
	done
elif [ $1 == "--copyDataToCockroachDB" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "copying data to cockroachDB..."
	${projectRootPath}/shell/copy_data_to_cockroach_fs.sh $2
elif [ $1 == "--refreshCockroachDBData" ];then
	echo "importing data to cockroachDB..."
	refreshDataScript=/temp/DatabaseTuning/sql/refreshData.sql
	${cockroachPath} sql --insecure --host=xcnd35 --file=${refreshDataScript}
elif [ $1 == "--initDesignedTables" ];then
	echo "initiating newly designed schema..."
	newSchemaInitScript=/temp/DatabaseTuning/sql/initDesignedTables.sql
	${cockroachPath} sql --insecure --host=xcnd35 --file=${newSchemaInitScript}
elif [ $1 == "--countLines" ];then
	countLinesScript=/temp/DatabaseTuning/sql/countLines.sql
	${cockroachPath} sql --insecure --host=xcnd35 --file=${countLinesScript} > countLines.txt
elif [ $1 == "--showSchema" ];then
	showSchemaScript=/temp/DatabaseTuning/sql/showSchema.sql
	${cockroachPath} sql --insecure --host=xcnd35 --file=${showSchemaScript} > schema.txt
elif [ $1 == "--quitCockroachDB" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "quiting cockroachDB..."
	${projectRootPath}/shell/quitCockroachDB.sh $2
elif [ $1 == "--quitAllCockroachDB" ];then
	for ((c = 0; c < 5; c++))
	do
		server_no=$(expr $c + 5)
		para=$(expr $c + 1)
		ssh kaisheng@xcnd3${server_no}.comp.nus.edu.sg "${projectRootPath}/shell/quitCockroachDB.sh $para"
	done
elif [ $1 == "--runExperimentsCockroachDB" ];then
	if [ $# != 3 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "running all 40 clients on xcnd35-xcnd39..."
	for ((c = 0; c < 5; c++))
	do
		server_no=$(expr $c + 5)
		ssh kaisheng@xcnd3${server_no}.comp.nus.edu.sg "cd ${projectRootPath} && git checkout main && git pull"
		ssh kaisheng@xcnd3${server_no}.comp.nus.edu.sg "cd ${projectRootPath}/src/cockroachdb/$2 && ./run_8_clients.sh ${c} $3"
	done
elif [ $1 == "--fetchLogToLocal" ];then
	if [ $# != 2 ];then
		echo "missing parameters, use --help option to see more information"
		exit 1
	fi
	echo "fetching logs from xcnd35-xcnd39 to local..."
	for ((c = 35; c <= 39; c++))
	do
		scp -r kaisheng@xcnd${c}.comp.nus.edu.sg:${projectRootPath}/src/cockroachdb/$2/log log_xcnd${c}
	done
elif [ $1 == "--help" ];then
	echo "Options:"
	echo "	--sql				Execute sql command 			(param1: sql command you want to execute)"
	echo "	--sqlfromfile			Execute sql command from file 		(param1: sql file you want to execute)"
	echo "	--nodestatus			Show node status"
	echo "	--startCockroachDB		Start cockroachDB cluster		(param1: node number, 1,2,3,4,5 for xcnd35-xcnd39 respectively, must be execute in xcnd39 lastly)"
	echo "	--startAllCockroachDB		Start all cockroachDB nodes (xcnd35-xcnd39)"
	echo "	--copyDataToCockroachDB		Copy data files to cockroachDB file system, must start cockroachDB cluster first		(param1: node number, 1,2,3,4,5 for xcnd35-xcnd39 respectively)"
	echo "	--refreshCockroachDBData	Refresh the data in cockroachDB, must copy data files to cockroachDB file system first"
	echo "	--initDesignedTables		Init newly designed schema, will also insert data into the new schema tables, must import data to original tables first"
	echo "	--countLines			Count the number of records in the current tables in wholesaledata, will output to countLines.txt, must init designed tables first"
	echo "	--showSchema			Show current schema of tables in wholesaledata, will output to schema.txt, must init designed tables first"
	echo "	--quitCockroachDB		Quit cockroachDB node, will also delete the data directory		(param1: node number, 1,2,3,4,5 for xcnd35-xcnd39 respectively)"
	echo "	--quitAllCockroachDB		Quit all cockroachDB nodes (xcnd35-xcnd39)"
	echo "	--runExperimentsCockroachDB	Run all experiments using cockroachDB				(param1: baseline, tuning_schema, tuning_threads or tuning_schema_and_threads, means which version of code to run. param2: A or B, means workload A or workload B respectively)"
	echo "	--fetchLogToLocal		Fetch all the logs to current local file system, should run on local system or xcnd35		(param1: baseline, tuning_schema, tuning_threads or tuning_schema_and_threads, means which version of logs to fetch)"
fi
