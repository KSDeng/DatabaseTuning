
# params
# $1: 1,2,3,4,5 for 5 servers respetively

. /temp/DatabaseTuning/shell/common.sh
dataFilePath=/temp/DatabaseTuning/data/cockroachdb

cd ${root}
if [ ! -d "node$1" ];then
	echo "CockroachDB file system does not exists, make sure you have started the cluster first."
	exit 1
fi

cd ${root}/node$1

if [ -d "extern" ];then
	rm -rf extern
fi
mkdir extern
cp ${dataFilePath}/* ${root}/node$1/extern/

