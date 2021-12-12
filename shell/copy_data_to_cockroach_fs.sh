
# params
# $1: 1,2,3,4,5 for 5 servers respetively

. /temp/DatabaseTuning/shell/common.sh
dataFilePath=/temp/DatabaseTuning/data/cockroachdb

cd ${cockroachPath}/node$1
mkdir extern
cp ${dataFilePath}/* ${cockroachPath}/node$1/extern/

