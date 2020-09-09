# Datasketches sandbox project

[Apache Datasketches](https://datasketches.apache.org/) is a software library of stochastic streaming algorithms.

This repository provides a simple HTTP interface to evaluate datasketches on your own data.  Datasketches is order insensitive
to input data and only has to see a data item once ("one touch").

For large datasets, the following problems are typically difficult or impossible to measure exactly:
- distinct count
- quantile summaries
- frequent items 

The service maintains a stateful in-memory sketch for each dataset, which can be periodically interrogated for approximate results.
Each sketch needs to be assigned a key for reference.  This allows set operations between sketches.

# Distinct count

Problem: Gather a distinct count of identities, independent of the order of the input. 

<pre>

# Adds a single identifier to for a user in Japan, where `country-jp` is the sketch key
curl -X PUT http://127.0.0.1:8099/v1/distinct/count/country-jp/user-id1
→ Accepted

# Adds a single identifier to for a user in the US, where `country-us` is the sketch key
curl -X PUT http://127.0.0.1:8099/v1/distinct/count/country-us/user-id2
→ Accepted

# Estimate the union of the distinct users in both the US and Japan
curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-us/union/country-jp
→ {"value":2.0,"lowerBound":2.0,"upperBound":2.0}

# Estimate the intersection of the distinct users in both the US and Japan
curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-us/intersect/country-jp
→ {"value":0.0,"lowerBound":0.0,"upperBound":0.0}

# Estimate the number of the users in Japan and not the US
curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-us/anotb/country-jp
→ {"value":1.0,"lowerBound":1.0,"upperBound":1.0}

# Upload a file of line separated user identifiers from Japan user base  
curl -F filename=@identifiers-jp.txt  http://127.0.0.1:8099/v1/distinct/count/country-jp
→ Accepted

# Upload a file of line separated user identifiers from Japan user base in exact mode
curl -F filename=@identifiers-jp.txt  http://127.0.0.1:8099/v1/distinct/count/country-jp?exact
→ Accepted

# Clear the sketch for Japan
curl -X DELETE http://127.0.0.1:8099/v1/distinct/count/country-jp
→ Ok

# Clear the sketch for the US
curl -X DELETE http://127.0.0.1:8099/v1/distinct/count/country-us
→ Ok

</pre>

For comparison purposes, any of the above URLs can have the ?exact flag set to perform an exact
count distinct.  Uploading large input streams can be twice as slow on the exact endpoints, whereas
the sketches grow sub-linearly in relation to the input data size.

# Building a Linux executable

1. Build the Docker image in the `docker` directory

<pre>

docker build -f docker/GraalDockerfile -t datasketches-sandbox/graalvm-native-image .

</pre>

2. Run the `nativeImage` task from sbt. The result will be a Linux executable. 

# Builds the lightweight docker image locally
docker build -f docker/SandboxDockerfile -t datasketches-sandbox/ds-sandbox-server .

# Running in Docker

<pre>

# Starts the published container from Github container service
docker run -d -p 8099:8080/tcp ghcr.io/davecromberge/datasketches-sandbox/ds-sandbox-server:latest
→ container-id

# Tests the container
curl -X GET http://0.0.0.0:8099/ping
→ pong

# Stops the container
docker stop <container-id>

</pre>

# Environment variables

By default, the sketch nominal entries setting is 2^16, and affects the accuracy of the final estimate.

To alter the default, run the docker image with a different default:

<pre>

docker run -d --env SKETCH_ACCURACY=12 -p 8099:8080/tcp datasketches-sandbox/ds-sandbox-server

</pre>

# Acknowledgements

- The [Apache Datasketches](https://datasketches.apache.org/) team and community for the incredibly useful library.
- This [blog post](https://www.inner-product.com/posts/serverless-scala-services-with-graalvm/) by Noel Welsh describes how
  to build a GraalVM service using SBT and docker.
