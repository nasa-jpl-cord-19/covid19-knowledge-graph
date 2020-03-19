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
      "org.apache.jena" % "jena-tdb" % "3.14.0",
      "org.slf4j" % "slf4j-nop" % "1.7.25",
      "com.googlecode.json-simple" % "json-simple" % "1.1.1"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
