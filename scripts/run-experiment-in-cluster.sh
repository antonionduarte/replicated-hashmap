#!/bin/bash

if [ $# -lt 2 ]
then
    echo "Usage: $0 <job id> <machine name> [exp name]"
    exit 1
fi

CLUSTER_HOST="dicluster"
FOLDER="replicated-hashmap/"
CLUSTER_FOLDER=$CLUSTER_HOST":"$FOLDER
JOB_ID=$1
MACHINE_NAME=$2

shift 2

#rsync -avzq ./analysis/experiments $CLUSTER_FOLDER"/analysis/experiments"

# Delete any previous results
echo $CLUSTER_HOST "OAR_JOB_ID=$JOB_ID oarsh $MACHINE_NAME 'cd $FOLDER && python3 analysis/executor.py $@'"
ssh $CLUSTER_HOST "OAR_JOB_ID=$JOB_ID oarsh $MACHINE_NAME 'cd $FOLDER && python3 analysis/executor.py $@'"

echo rsync -r $CLUSTER_FOLDER"analysis/experiments/" analysis/experiments/
rsync -r $CLUSTER_FOLDER"analysis/experiments/" analysis/experiments/
