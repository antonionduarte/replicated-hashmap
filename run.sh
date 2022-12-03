#!/bin/sh

if [ "$#" -ne "2" ]; then
	echo "usage: $0 <process count> <port offset>"
	exit 1
fi

PROCESS_COUNT=$1
PORT_OFFSET=$2
BASE_P2P_PORT=3000
BASE_SERVER_PORT=35000
P2P_PORT=$(python -c "print($BASE_P2P_PORT + $PORT_OFFSET)")
SERVER_PORT=$(python -c "print($BASE_SERVER_PORT + $PORT_OFFSET)")
MEMBERSHIP=$(python -c "print(','.join(['localhost:'+str($BASE_P2P_PORT+i) for i in range($PROCESS_COUNT)]))")

#echo "PORT_OFFSET: $PORT_OFFSET"
#echo "P2P_PORT: $P2P_PORT"
#echo "SERVER_PORT: $SERVER_PORT"
#echo "MEMBERSHIP: $MEMBERSHIP"

watchexec \
	--no-vcs-ignore \
	--restart \
	--clear \
	--watch target/asdProj.jar \
	"java -ea -cp target/asdProj.jar:./ asd.PaxosMain -conf config.properties babel_address=localhost statemachine_port=$P2P_PORT hashapp_port=$SERVER_PORT statemachine_initial_membership=$MEMBERSHIP"
