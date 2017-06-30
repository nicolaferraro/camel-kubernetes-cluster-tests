#!/bin/bash

set -e

echo "Deleting project test if exists..."
oc delete project test && sleep 25 || true

echo "Creating project test..."
oc new-project test

echo "Creating service accounts and roles..."
oc create serviceaccount leader
oc adm policy add-role-to-user edit --serviceaccount leader

echo "Deploying target application..."
mvn clean install -N
cd leader-app
mvn clean fabric8:deploy

echo "Running tests..."
cd ../tests/
mvn clean install
mvn test -Dtest=*KT -Dnamespace.use.existing=test -e
