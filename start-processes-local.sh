#!/bin/bash

processes=$1

if [ -z $processes ] || [ $processes -lt 1 ]; then
  echo "please indicate a number of processes of at least one"
  exit 0
fi

i=0
base_p2p_port=3000
base_server_port=35000

membership="localhost:${base_p2p_port}"

read -p "------------- Press enter start. After starting, press enter to kill all servers --------------------"

i=1
while [ $i -lt $processes ]; do
    membership="${membership},localhost:$(($base_p2p_port + $i))"
    i=$(($i + 1))
done

i=0
while [ $i -lt $processes ]; do
  java -DlogFilename=logs/node$(($base_p2p_port + $i)) -cp target/asdProj.jar:./ asd.Main -conf config.properties babel_address=localhost statemachine_port=$(($base_p2p_port + $i)) hashapp_port=$(($base_server_port + $i)) statemachine_initial_membership=$membership 2>&1 | sed "s/^/[$(($base_p2p_port + $i))] /" &
  echo "launched process on p2p port $(($base_p2p_port + $i)), server port $(($base_server_port + $i))"
  sleep 1
  i=$(($i + 1))
done

sleep 2
read -p "------------- Press enter to kill servers. --------------------"

kill $(ps aux | grep 'asdProj.jar' | awk '{print $2}')

echo "All processes done!"
