package gov.nasa.jpl.covid19_knowledge_graph

import java.io._

import gov.nasa.jpl.covid19_textmining_kaggle.jena_resources
import org.apache.jena.riot.RDFDataMgr
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

object covid19_knowledge_graph {

  final val COVID_NS: String = "http://knowledge.jpl.nasa.gov/COVID-19/"

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

    files foreach { file =>
      println(s"  loading ${file.getCanonicalPath}")
      val graph = new File("covid19_knowledge_graph.ttl")
      graph.createNewFile()
      val model = RDFDataMgr.loadModel("covid19_knowledge_graph.ttl")
      model.setNsPrefix("covid", COVID_NS)
      model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
      model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
      model.setNsPrefix("schema", "http://schema.org/")
      loadJSONFromFile(file) foreach { json =>
        jena_resources.createPaperResource(json, model)
      }

      //write to local file covid19_knowledge_graph.ttl
      val outFile = new BufferedWriter(new FileWriter(new File("covid19_knowledge_graph.ttl")))
      RDFDataMgr.write(outFile, model, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
      outFile.close()

      // Build a text search dataset
      jena_resources.createTextSearchDataset(model, "covid19_knowledge_graph.ttl")
    }

    def loadJSONFromFile(file: File): Option[JSONObject] = {
      val parser = new JSONParser
      try Some(parser.parse(new FileReader(file)).asInstanceOf[JSONObject])
      catch {
        case e: Exception =>
          println(s"ERROR: $file: ${e.getMessage}")
          None
      }
    }
  }
}