#!/bin/bash

CLUSTER_FOLDER="dicluster:replicated-hashmap/"

rsync -r config.properties $CLUSTER_FOLDER
rsync -r log4j2.xml $CLUSTER_FOLDER
rsync -r analysis/*.py $CLUSTER_FOLDER"analysis/"
rsync -r analysis/*.json $CLUSTER_FOLDER"analysis/"
rsync -r client/* $CLUSTER_FOLDER"client/"
rsync -r target/asdProj.jar $CLUSTER_FOLDER"target/"
