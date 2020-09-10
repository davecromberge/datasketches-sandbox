# Datasketches sandbox project

[Apache Datasketches](https://datasketches.apache.org/) is a software library of stochastic streaming algorithms.

This repository provides a simple HTTP interface to evaluate datasketches on your own data.

For large datasets, the following problems are typically difficult to measure exactly using limited resources:
- distinct count
- quantiles and histograms
- frequent items 
- reservoir sampling

Datasketches makes use of sketches with mathematically proven error bounds to provide robust solutions to these problems.  Moreover, it is order insensitive
to input data and only has to see a data item once ("one touch") making it ideal for streaming and big data use cases.

# Usage notes

The service maintains a stateful in-memory sketch/exact copy for each dataset, which can be periodically interrogated for approximate results.
This stateful operation allows set operations between sketches.

In order to use the exact equivalent to a sketch, append the `?exact` flag to the endpoint.

Each sketch needs to be assigned a key for reference, which typically adheres to the following format:

`dataset-dimension1-dimension2-dimensionN`

For example:

<pre>

# country dataset, country code
country-jp
country-us

# occupation dataset, job name, state
occupation-technician-ca
occupation-surgeon-co
occupation-surgeon-tx

</pre>

Finally, see the useful helper scripts in the `scripts` directory.

# Running in Docker

<pre>

# Starts the published container from Github container service
docker run -d -p 8099:8080/tcp ghcr.io/davecromberge/datasketches-sandbox/ds-sandbox-server:latest
→ container-id

# Tests the container
curl -X GET http://0.0.0.0:8099/ping
→ pong

# Stops the container
docker stop container-id

</pre>

# Distinct count

Problem: Gather a distinct count of identities, independent of the order of the input. 

<pre>

curl -X PUT http://127.0.0.1:8099/v1/distinct/count/country-jp/user-id1
→ Accepted

curl -X PUT http://127.0.0.1:8099/v1/distinct/count/country-jp/user-id2
→ Accepted

curl -X PUT http://127.0.0.1:8099/v1/distinct/count/country-us/user-id2
→ Accepted

curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-jp
→ {"value":2.0,"lowerBound":2.0,"upperBound":2.0}

curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-us/union/country-jp
→ {"value":2.0,"lowerBound":2.0,"upperBound":2.0}

curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-us/intersect/country-jp
→ {"value":1.0,"lowerBound":1.0,"upperBound":1.0}

curl -X GET http://127.0.0.1:8099/v1/distinct/count/country-jp/anotb/country-us
→ {"value":1.0,"lowerBound":1.0,"upperBound":1.0}

curl -F filename=@identifiers-us.txt  http://127.0.0.1:8099/v1/distinct/count/country-us
→ Accepted

curl -F filename=@identifiers-jp.txt  http://127.0.0.1:8099/v1/distinct/count/country-jp?exact
→ Accepted

curl -X DELETE http://127.0.0.1:8099/v1/distinct/count/country-jp
→ Ok

curl -X DELETE http://127.0.0.1:8099/v1/distinct/count/country-us
→ Ok

</pre>

For comparison purposes, any of the above URLs can have the `?exact` flag set to perform an exact
count distinct.  Uploading large input streams to the exact endpoints can be orders of magnitude slower,
whereas the sketches grow sub-linearly in relation to the input data size.

# Environment variables

By default, the sketch nominal entries setting is 2^16, and affects the accuracy of the final estimate.

To alter the defaults, run the docker image with the relevant environment variables set:

<pre>

docker run -d --env SKETCH_ACCURACY=12 -p 8099:8080/tcp datasketches-sandbox/ds-sandbox-server

</pre>

# Building a Linux executable

1. Build the Docker image in the `docker` directory

<pre>

docker build -f docker/GraalDockerfile -t datasketches-sandbox/graalvm-native-image .

</pre>

2. Run the `nativeImage` task from sbt. The result will be a Linux executable. 

3. Build the lightweight docker image locally

<pre>

docker build -f docker/SandboxDockerfile -t datasketches-sandbox/ds-sandbox-server .

</pre>

# Acknowledgements

- The [Apache Datasketches](https://datasketches.apache.org/) team and community for the incredibly useful library.
- This [blog post](https://www.inner-product.com/posts/serverless-scala-services-with-graalvm/) by Noel Welsh describes how
  to build a GraalVM service using SBT and docker.
  
# Todos

- Support more sketch types
- Create a java equivalent for the Apache organisation
- Add better documentation
- Github actions for automatically publishing the package to ghcr
