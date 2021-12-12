
. ./shell/common.sh           # import global variables

if [ $1 == "--sql" ];then
	echo "execute sql"
	${cockroachPath} sql --insecure --host=xcnd30 --execute="$2"
elif [ $1 == "--sqlfromfile" ];then
	echo "execute sql from file"
	${cockroachPath} sql --insecure --host=xcnd30 --file=$2
elif [ $1 == "--nodestatus" ];then
	echo "show node status"
	${cockroachPath} node status --insecure --host=xcnd30
fi
