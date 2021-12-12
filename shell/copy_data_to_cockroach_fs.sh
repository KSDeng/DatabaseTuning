
# params
# $1: 1,2,3,4,5 for 5 servers respetively

. /temp/DatabaseTuning/shell/common.sh
dataFilePath=/temp/DatabaseTuning/data/cockroachdb

cd ${root}/node$1
mkdir extern
cp ${dataFilePath}/* ${root}/node$1/extern/

