# start database


case $1 in
	1)  echo 'start db 1'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node1 \
	--listen-addr=192.168.48.249:26257 \
	--http-addr=192.168.48.249:8080 \
	--join=192.168.48.249:26257,192.168.48.250:26257,192.168.48.251:26257,192.168.48.252:26257,192.168.48.253:26257 \
	--background
	;;
	2)  echo 'start db 2'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node2 \
	--listen-addr=192.168.48.250:26257 \
	--http-addr=192.168.48.250:8080 \
	--join=192.168.48.249:26257,192.168.48.250:26257,192.168.48.251:26257,192.168.48.252:26257,192.168.48.253:26257 \
	--background
	;;
	3)  echo 'start db 3'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node3 \
	--listen-addr=192.168.48.251:26257 \
	--http-addr=192.168.48.251:8080 \
	--join=192.168.48.249:26257,192.168.48.250:26257,192.168.48.251:26257,192.168.48.252:26257,192.168.48.253:26257 \
	--background
	;;
	4)  echo 'start db 4'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node4 \
	--listen-addr=192.168.48.252:26257 \
	--http-addr=192.168.48.252:8080 \
	--join=192.168.48.249:26257,192.168.48.250:26257,192.168.48.251:26257,192.168.48.252:26257,192.168.48.253:26257 \
	--background
	;;
	5)  echo 'start db 5'
	${cockroachPath} start \
	--insecure \
	--store=${root}/node5 \
	--listen-addr=192.168.48.253:26257 \
	--http-addr=192.168.48.253:8080 \
	--join=192.168.48.249:26257,192.168.48.250:26257,192.168.48.251:26257,192.168.48.252:26257,192.168.48.253:26257 \
	--background

	${cockroachPath} init --insecure --host=192.168.48.253:26257
	;;
	*)  echo 'You do not select a number between 1 to 5'
	;;
esac
