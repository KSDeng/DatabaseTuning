
# params
# $1: 1,2,3,4,5 for 5 servers respetively

cockroachPath=/temp/CS5424A/
dataFilePath=/temp/CS5424A/NUS-CS5424-DistributedDatabase/Data/cockroachdb

cd ${cockroachPath}/node$1
mkdir extern
cp ${dataFilePath}/* ${cockroachPath}/node$1/extern/

