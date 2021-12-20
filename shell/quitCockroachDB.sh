
# 1. Get the process that occupies the port that runs cockroachdb
# 2. Kill the process
# 3. Remove the data directory for the current node

if [ $# != 1 ];then
	echo "Parameter number invalid, the first parameter should be 1,2,3,4,5 for xcnd35-xcnd39 respectively"
	exit 1
fi

portResult=`netstat -tunlp | grep 26257`

if [[ $portResult =~ "cockroach" ]];then
	echo "containts cockroach"
	strs=($portResult)
	last_str=${strs[${#strs[*]}-1]}
	array=(${last_str//\// })
	port_number=${array[0]}
	echo ${port_number}
	kill -9 ${port_number}
fi

rm -rf /temp/node$1
