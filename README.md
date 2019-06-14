
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/

[make]: https://www.gnu.org/software/make

NDEx Search REST Service
========================

[![Build Status](https://travis-ci.org/ndexbio/ndexsearch-rest.svg?branch=master)](https://travis-ci.org/ndexbio/ndexsearch-rest) 
[![Coverage Status](https://coveralls.io/repos/github/ndexbio/ndexsearch-rest/badge.svg?branch=master)](https://coveralls.io/github/ndexbio/ndexsearch-rest?branch=master)

Provides integrated search REST service using NDEx as a backend.
This service runs using an embedded [Jetty][jetty] server and is invoked
from the command line. 

Requirements
============

* Centos 7+, Ubuntu 12+, and most other linux distributions should work
* [Java][java] 8+ **(jdk to build)**
* [Make][make] **(to build)**
* [Maven][maven] 3.3 or higher **(to build)** -- tested with 3.6

Special Java modules to install (cause we haven't put these into maven central)

* [ndex-enrichment-rest-model](https://github.com/ndexbio/ndex-enrichment-rest-model) built and installed via `mvn install`
* [ndex-enrichment-rest-client](https://github.com/ndexbio/ndex-enrichment-rest-client) built and installed via `mvn install`
* [ndex-object-model](https://github.com/ndexbio/ndex-object-model) built and installed via `mvn install`
* [ndex-java-client](https://github.com/ndexbio/ndex-java-client) built and installed via `mvn install`
* [ndex-interactome-search](https://github.com/ndexbio/ndex-interactome-search) built and installed via `mvn install -DskipTests=true`

Building NDEx Search REST Service  
=================================

NDEx Search REST Service build requirements:

* [Java 8+][java] JDK
* [Make][make] **(to build)**
* [Maven][maven] 3.0 or higher **(to build)**


Commands below build NDEx Search REST Service assuming machine has [Git][git] command line tools 
installed and above Java modules have been installed:

```Bash
# In lieu of git one can just download repo and unzip it
git clone https://github.com/ndexbio/ndexsearch-rest.git

cd ndexsearch-rest
mvn clean test install
```

The above command will create a jar file under **target/** named  
**ndexsearch-rest-\<VERSION\>-jar-with-dependencies.jar** that
is a command line application

Running Enrichment REST Service
===============================

The following steps cover how to configure and run integrated search REST service.
In the steps below **ndexsearch.jar** refers to the jar
created previously named **ndexsearch-rest-\<VERSION\>-jar-with-dependencies.jar**

### Step 1 Create directories and configuration file

```bash
# create directory
mkdir -p searchdb/logs searchdb/tasks
cd searchdb

# Generate template configuration file
java -jar ndexsearch.jar --mode exampleconf > ndexsearch.conf
```

The `ndexsearch.conf` file will look like the following:

```bash
# Example configuration file for Search service


# Sets Search database directory
search.database.dir = /tmp

# Sets Search task directory where results from queries are stored
search.task.dir = /tmp/tasks

# Run Service under embedded Jetty command line parameters
runserver.contextpath = /
runserver.log.dir = /tmp/log
runserver.port = 8080

# Sets name of json file containing source results.
# This file expected to reside in search.database.dir directory
sourceresults.json = sourceresults.json
ndex.user = bob
ndex.password = somepassword
ndex.server = public.ndexbio.org
ndex.useragent = NDExSearch/1.0
```

Replace **/tmp** paths with full path location to **searchdb** directory created
earlier.

### Step 2 Create sourceresults.json file

This file (which resides in **search.database.dir**) contains
information about services this integrated search and utilize.

Run the following to create an example **searchresults.json** file:

```bash
java -jar ndexsearch.jar --mode examplesourceresults > searchresults.json
```

The **searchresults.json** file will look like this:

```bash
{
  "results" : [ {
    "name" : "enrichment",
    "description" : "This is a description of enrichment source",
    "numberOfNetworks" : "350",
    "uuid" : "eeb4af50-83c4-4e33-ac21-87142403589b",
    "endPoint" : "http://localhost:8085/enrichment",
    "version" : "0.1.0",
    "status" : "ok",
    "databases" : [ {
      "name" : "signor",
      "description" : "This is a description of a signor database",
      "numberOfNetworks" : "50",
      "uuid" : "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6"
    }, {
      "name" : "ncipid",
      "description" : "This is a description of a ncipid database",
      "numberOfNetworks" : "200",
      "uuid" : "e508cf31-79af-463e-b8b6-ff34c87e1734"
    } ]
  }, {
    "name" : "interactome",
    "description" : "This is a description of interactome service",
    "numberOfNetworks" : "2009",
    "uuid" : "0857a397-3453-4ae4-8208-e33a283c85ec",
    "endPoint" : "http://localhost:8086/interactome",
    "version" : "0.1.1a1",
    "status" : "ok",
    "databases" : null
  }, {
    "name" : "keyword",
    "description" : "This is a description of keyword service",
    "numberOfNetworks" : "2009",
    "uuid" : "33b9c3ca-13e5-48b9-bcd2-09070203350a",
    "endPoint" : "http://localhost:8086/keyword",
    "version" : "0.2.0",
    "status" : "ok",
    "databases" : null
  } ]
}
```

Each service under **results** has various fields needed to access that service

### Step 4 Run the service

```bash
jav -jar ndexsearch.jar --mode runserver --conf enrichment.conf
```



COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
