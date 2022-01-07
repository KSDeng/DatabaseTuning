
# $1: 0,1,2,3,4, the server number, means xcnd35-xcnd39 respectively
# $2: A or B, means workload A or workload B

if [ $# != 2 ]; then
	echo "\$1: 0,1,2,3,4, the server number, means xcnd35-xcnd39 respectively"
	echo "\$2: A or B, means workload A or workload B"
	exit 1;
fi

if [ $2 != 'A' ] && [ $2 != 'B' ]; then
	echo "\$1: 0,1,2,3,4, the server number, means xcnd35-xcnd39 respectively"
	echo "\$2: A or B, means workload A or workload B"
	exit 1;
fi

./compile.sh
echo "Compile source code succeeded."

if [ -d "log" ]; then
	rm -rf log
fi

mkdir log
mkdir log/output
mkdir log/statistics

xact_file_path=/temp/DatabaseTuning/input/xact_files_$2

for ((c = $1; c < 40; c += 5))
do
	echo "Running client ${c}..."
	java -cp .:libs/checker-qual-3.5.0.jar:libs/HikariCP-5.0.0.jar:libs/postgresql-42.2.24.jar:libs/slf4j-api-1.7.30.jar:libs/slf4j-simple-1.7.30.jar:libs/commons-math3-3.6.1.jar mainDriver ${xact_file_path}/$c.txt >log/output/${c}_output.txt 2>log/statistics/${c}_stat.txt &
done

