# Camel Kubernetes Cluster Service: System Tests

This repository contains system tests for the Camel Kubernetes Cluster service. The purpose of the cluster service is to 
keep one and only one active route in all pods of the cluster.

## Installation

1. Build Kubernetes Client from [here](https://github.com/nicolaferraro/kubernetes-client/tree/767-optimistic-lock)

```
mvn clean install -DskipTests
```

2. Build Camel from [here]() 

```
mvn clean install -P fastinstall
```

3. Start Openshift

```
oc cluster up
```

4. Run the tests

```
./runSystemTests.sh
```