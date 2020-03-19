package gov.nasa.jpl.covid19_knowledge_graph

import java.io.{BufferedWriter, File, FileReader, FileWriter}

import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.json.simple.parser.JSONParser
import org.json.simple.{JSONArray, JSONObject}

object covid19_knowledge_graph {

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      main(args.head)
    }
    else println("Missing directory argument")
  }

  def main(inDir: String): Unit = {
    val dirFile = new File(inDir)
    val files = dirFile
      .listFiles()
      .filter(_.getName.endsWith(".json"))
      .sortBy(_.getName)

    val graph = new File("covid19_knowledge_graph.ttl")
    graph.createNewFile()
    val model = RDFDataMgr.loadModel("covid19_knowledge_graph.ttl")
    model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
    model.setNsPrefix("schema", "http://schema.org/")

    files foreach { file =>
      println(s"  loading ${file.getCanonicalPath}")
      loadJSON(file) foreach { json =>
        val COVID_NS: String = "http://knowledge.jpl.nasa.gov/COVID-19/"

        val paperId = json.get("paper_id").asInstanceOf[String]
        val paperResource = model.createResource(COVID_NS + paperId,
          ResourceFactory.createResource(model.expandPrefix("schema:ScholarlyArticle")))

        val title = json.get("metadata").asInstanceOf[JSONObject].get("title").asInstanceOf[String]
        paperResource.addProperty(ResourceFactory.createProperty(
            model.expandPrefix("schema:title")), title, "en")

        val paperAbstractArray = json.get("abstract").asInstanceOf[JSONArray]
        val abstractIterator = paperAbstractArray.iterator
        while (abstractIterator.hasNext) {
          paperResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("schema:abstract")),
            abstractIterator.next.asInstanceOf[JSONObject].get("text").asInstanceOf[String], "en")
        }

        val bodyTextArray = json.get("body_text").asInstanceOf[JSONArray]
        val bodyIterator = bodyTextArray.iterator
        while (bodyIterator.hasNext) {
          val section = bodyIterator.next.asInstanceOf[JSONObject]
          paperResource.addProperty(ResourceFactory.createProperty(
            model.expandPrefix("schema:text")), section.get("text").asInstanceOf[String], "en")
        }
      }
      val outFile = new BufferedWriter(new FileWriter(new File("covid19_knowledge_graph.ttl")))
      RDFDataMgr.write(outFile, model, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
      outFile.close()
    }

    def loadJSON(file: File): Option[JSONObject] = {
      val parser = new JSONParser
      try Some(parser.parse(new FileReader(file)).asInstanceOf[JSONObject])
      catch {
        case e: Exception =>
          println(s"ERROR: $file: ${e.getMessage}")
          None
      }
    }
//
//    def executeWikidataDescriptionQuery(label: String): Literal = {
//      val query = getWikidataDescriptionQuery(label)
//      val response: Unit = tryWith(
//          QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query)){ qexec =>
//        val results = qexec.execSelect()
//        if (results.hasNext) {
//          val soln = results.next
//          return soln.getLiteral("o")
//        }
//      }
//      return null
//    }
//
//    def getWikidataDescriptionQuery(label: String): String = {
//      s"""PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
//         |PREFIX schema: <http://schema.org/>
//         |SELECT ?o WHERE {
//         |    ?s rdfs:label "$label"@en .
//         |    ?s schema:description ?o .
//         |    FILTER(LANGMATCHES(LANG(?o), "en"))
//         |    FILTER(STRLEN(?o) > 15)
//         |}
//         |""".stripMargin
//    }
//
//    def getValueAsString(node: RDFNode): String = node match {
//      case lit: Literal  => lit.getLexicalForm
//      case res: Resource => res.getURI
//    }
//
//    def tryWith[R, T <: AutoCloseable](resource: T)(doWork: T => R): R = {
//      try {
//
//        doWork(resource)
//      }
//      finally {
//        try {
//          if (resource != null) {
//            resource.close()
//          }
//        }
//        catch {
//          case e: Exception => throw e
//        }
//      }
//    }
  }
}

