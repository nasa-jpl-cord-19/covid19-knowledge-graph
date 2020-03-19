# COVID-19 Research Knowledge Graph

Builds a knowledge graph from the [COVID-19 Open Research Dataset (CORD-19)](https://pages.semanticscholar.org/coronavirus-research) dataset. As of 2020-03-18 it has been run against the [Commercial use subset](https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/2020-03-13/comm_use_subset.tar.gz) (includes PMC content) -- 9000 papers, 186Mb.

**This project is written is Scala**... you require [sbt](https://www.scala-sbt.org/) to continue.

## Prerequsites

* Install [sbt](https://www.scala-sbt.org/)
* Download the [Commercial use subset](https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/2020-03-13/comm_use_subset.tar.gz) and extract it to some local directory
* Clone [dair-iitd/OpenIE-standalone](https://github.com/dair-iitd/OpenIE-standalone) and follow the [build instructions](https://github.com/dair-iitd/OpenIE-standalone#building)
  * `git clone  https://github.com/dair-iitd/OpenIE-standalone.git && cd OpenIE-standalone`
  * `sbt -J-Xmx10000M clean compile assembly`
  * `java -Xmx10g -XX:+UseConcMarkSweepGC -jar target/scala-2.10/openie-assembly-5.0-SNAPSHOT.jar --httpPort 8000`
  * To get an extraction from the server use the POST request on `/getExtraction` endpoint to POST **sentences**. The **sentence** will go in the body of HTTP request. An example of curl request `curl -X POST http://localhost:8000/getExtraction -d "The Jet Propulsion Laboratory is a federally funded research and development center and NASA field center in the city of La Canada Flintridge with a Pasadena mailing address, within the state of California, United States."`

## Installation

Back in this directory...

Launch sbt:

    $ sbt compile

## Running

Launch sbt:

    $ sbt

Run the program with an argument indicating the input `data` directory containing the dataset:

    > run path/to/data/containing/Commercial use subset json files

## Output

Once the program runs (this may take some time depending on how much memory your machine has) you will find a newly written file called `covid19_knowledge_graph.ttl`. This file can be loaded into [Apache Jena's Fuseki](https://jena.apache.org/documentation/fuseki2/index.html) server (or any other SPARQL server which permits ingest of TTL RDF graphs).

## Querying Data

Once the data is loaded into Fuseki, you can use Jena's powerful [full text search](https://jena.apache.org/documentation/query/text-query.html) which combines SPARQL and full text search via Lucene or ElasticSearch (built on Lucene).  It gives applications the ability to perform indexed full text searches within SPARQL queries.

## Contact

Dr. Lewis John McGibbney Ph.D., B.Sc.(Hons)
Enterprise Search Technologist
Web and Mobile Application Development Group (172B)
Application, Consulting, Development and Engineering Section (1722)
Info & Engineering Technology Planning and Development Division (1720)
Jet Propulsion Laboratory
California Institute of Technology 
4800 Oak Grove Drive
Pasadena, California 91109-8099
Mail Stop : 600-172A
Tel:  (+1) (818)-393-7402
Cell: (+1) (626)-487-3476
Fax:  (+1) (818)-393-1190
Email: lewis.j.mcgibbney@jpl.nasa.gov
ORCID: orcid.org/0000-0003-2185-928X
