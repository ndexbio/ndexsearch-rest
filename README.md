
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
* [Maven][maven] 3.3 or higher **(to build)** -- tested with 3.6


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

Running NDEx Search REST Service
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

# Sets imageURL to use for results that lack imageURL
search.unset.image.url = http://ndexbio.org/images/new_landing_page_logo.06974471.png

# Sets Search task directory where results from queries are stored
search.task.dir = /tmp/tasks

# Run Service under embedded Jetty command line parameters
runserver.contextpath = /
runserver.log.dir = /tmp/log
runserver.port = 8080
# Valid log levels DEBUG INFO WARN ERROR ALL
runserver.log.level = INFO

# Sets name of json file containing source results.
# This file expected to reside in search.database.dir directory
source.configurations = source.configurations.json
ndex.user = bob
ndex.password = somepassword
ndex.server = public.ndexbio.org
ndex.useragent = NDExSearch/1.0

```

Replace **/tmp** paths with full path location to **searchdb** directory created
earlier.

### Step 2 Create source.configurations.json file

This file (which resides in **search.database.dir**) contains
information about services this integrated search and utilize.

Run the following to create an example **source.configurations.json** file:

```bash
java -jar ndexsearch.jar --mode examplesourceconfig > source.configurations.json
```

The **source.configurations.json** file will look like this:

```bash
{
  "sources" : [ {
    "name" : "enrichment",
    "description" : "This is a description of enrichment source",
    "endPoint" : "http://localhost:8095/enrichment",
    "uuid" : null
  }, {
    "name" : "interactome",
    "description" : "This is a description of interactome service",
    "endPoint" : "http://localhost:8096/interactome",
    "uuid" : null
  }, {
    "name" : "keyword",
    "description" : "This is a description of keyword service",
    "endPoint" : "http://localhost:8097/keyword",
    "uuid" : null
  } ]
}
```

Each service under **sources** has various fields needed to access that service.

The **uuid** should be set to a unique value for each service.

### Step 4 Run the service

```bash
jav -jar ndexsearch.jar --mode runserver --conf ndexsearch.conf
```



COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
