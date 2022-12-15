#!/bin/bash

CLUSTER_HOST="dicluster"
FOLDER="replicated-hashmap/"
CLUSTER_FOLDER=$CLUSTER_HOST":"$FOLDER

rsync -r $CLUSTER_FOLDER"analysis/experiments/" analysis/experiments/