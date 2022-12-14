#!/bin/bash

mvn clean compile package
./scripts/update-files-in-cluster.sh