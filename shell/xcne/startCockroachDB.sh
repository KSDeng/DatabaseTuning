# start database

. /temp/DatabaseTuning/shell/common.sh

# xcne1: 192.168.51.84
# xcne2: 192.168.51.85
# xcne3: 192.168.51.86
# xcne4: 192.168.51.87
# xcne5: 192.168.51.88

case $1 in
	1)  echo 'start db 1'
	echo ${cockroachPath}
	${cockroachPath} start \
	--insecure \
	--store=${root}/node1 \
	--listen-addr=xcne1:26257 \
	--http-addr=xcne1:8080 \
	--join=xcne1:26257,xcne2:26257,xcne3:26257,xcne4:26257,xcne5:26257 \
	--background
	;;
	2)  echo 'start db 2'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node2 \
	--listen-addr=xcne2:26257 \
	--http-addr=xcne2:8080 \
	--join=xcne1:26257,xcne2:26257,xcne3:26257,xcne4:26257,xcne5:26257 \
	--background
	;;
	3)  echo 'start db 3'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node3 \
	--listen-addr=xcne3:26257 \
	--http-addr=xcne3:8080 \
	--join=xcne1:26257,xcne2:26257,xcne3:26257,xcne4:26257,xcne5:26257 \
	--background
	;;
	4)  echo 'start db 4'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node4 \
	--listen-addr=xcne4:26257 \
	--http-addr=xcne4:8080 \
	--join=xcne1:26257,xcne2:26257,xcne3:26257,xcne4:26257,xcne5:26257 \
	--background
	;;
	5)  echo 'start db 5'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node5 \
	--listen-addr=xcne5:26257 \
	--http-addr=xcne5:8080 \
	--join=xcne1:26257,xcne2:26257,xcne3:26257,xcne4:26257,xcne5:26257 \
	--background

	${cockroachPath} init --insecure --host=xcne5:26257
	;;
	*)  echo 'You do not select a number between 1 to 5'
	;;
esac
