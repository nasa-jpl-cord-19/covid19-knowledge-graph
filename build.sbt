import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "gov.nasa.jpl"
ThisBuild / organizationName := "covid19_textmining_kaggle"

lazy val root = (project in file("."))
  .settings(
    name := "covid19_knowledge_graph",
    libraryDependencies ++= Seq(
      ("org.apache.jena" % "jena-core" % "3.14.0").exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.jena" % "jena-text" % "3.14.0",
      "org.apache.jena" % "jena-tdb" % "3.14.0",
      //peg at 7.4.0 for Jena compatability
      //https://jena.apache.org/documentation/query/text-query.html
      "org.apache.lucene" % "lucene-core" % "7.5.0",
      "com.googlecode.json-simple" % "json-simple" % "1.1.1",
      "org.apache.httpcomponents" % "httpclient" % "4.5.12",
      "com.ibm.icu" % "icu4j" % "66.1"
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
