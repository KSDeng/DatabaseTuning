# start database

. /temp/DatabaseTuning/shell/common.sh

# xcnd35: 192.168.48.254
# xcnd36: 192.168.48.255
# xcnd37: 192.168.51.0
# xcnd38: 192.168.51.1
# xcnd39: 192.168.51.2

case $1 in
	1)  echo 'start db 1'
	echo ${cockroachPath}
	${cockroachPath} start \
	--insecure \
	--store=${root}/node1 \
	--listen-addr=xcnd35:26257 \
	--http-addr=xcnd35:8080 \
	--join=xcnd35:26257,xcnd36:26257,xcnd37:26257,xcnd38:26257,xcnd39:26257 \
	--background
	;;
	2)  echo 'start db 2'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node2 \
	--listen-addr=xcnd36:26257 \
	--http-addr=xcnd36:8080 \
	--join=xcnd35:26257,xcnd36:26257,xcnd37:26257,xcnd38:26257,xcnd39:26257 \
	--background
	;;
	3)  echo 'start db 3'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node3 \
	--listen-addr=xcnd37:26257 \
	--http-addr=xcnd37:8080 \
	--join=xcnd35:26257,xcnd36:26257,xcnd37:26257,xcnd38:26257,xcnd39:26257 \
	--background
	;;
	4)  echo 'start db 4'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node4 \
	--listen-addr=xcnd38:26257 \
	--http-addr=xcnd38:8080 \
	--join=xcnd35:26257,xcnd36:26257,xcnd37:26257,xcnd38:26257,xcnd39:26257 \
	--background
	;;
	5)  echo 'start db 5'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node5 \
	--listen-addr=xcnd39:26257 \
	--http-addr=xcnd39:8080 \
	--join=xcnd35:26257,xcnd36:26257,xcnd37:26257,xcnd38:26257,xcnd39:26257 \
	--background

	${cockroachPath} init --insecure --host=xcnd39:26257
	;;
	*)  echo 'You do not select a number between 1 to 5'
	;;
esac
