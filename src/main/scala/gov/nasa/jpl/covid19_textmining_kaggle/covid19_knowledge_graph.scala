package gov.nasa.jpl.covid19_knowledge_graph

import java.io.{BufferedWriter, File, FileReader, FileWriter, FilenameFilter}

import gov.nasa.jpl.covid19_textmining_kaggle.jena_resources
import org.apache.jena.riot.RDFDataMgr
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

object covid19_knowledge_graph {

  final val COVID_NS: String = "http://knowledge.jpl.nasa.gov/COVID-19/"

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      main(args.head, args.last)
    }
    else println("1st Arg: CORD-19 directory   2nd Arg: Annie extractions directory")
  }

  def main(inDir: String, allenAIExtractionsDir: String): Unit = {
    val iDir = new File(inDir)
    val jFiles = iDir
      .listFiles()
      .filter(_.getName.endsWith(".json"))
      .sortBy(_.getName)
    val aDir = new File(allenAIExtractionsDir)
//    val aFiles = aDir
//      .listFiles()
//      .filter(_.getName.endsWith(".json"))
//      .sortBy(_.getName)
//    val tDir = new File(tikaExtractionsDir)
//    val tFiles = tDir
//      .listFiles()
//      .filter(_.getName.endsWith(".json"))
//      .sortBy(_.getName)

    jFiles foreach { jFile =>
      val aFileJSON = loadAFileJSON(aDir, jFile.getName)
      println(s"  loading ${jFile.getCanonicalPath}")
      val graph = new File("covid19_knowledge_graph.ttl")
      graph.createNewFile()
      val model = RDFDataMgr.loadModel("covid19_knowledge_graph.ttl")
      model.setNsPrefix("covid", COVID_NS)
      model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
      model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
      model.setNsPrefix("schema", "http://schema.org/")
      loadJSONFromFile(jFile) foreach { json =>
        jena_resources.createPaperResource(json, model, aFileJSON.get)
      }
      //write to local file covid19_knowledge_graph.ttl
      val outFile = new BufferedWriter(new FileWriter(new File("covid19_knowledge_graph.ttl")))
      RDFDataMgr.write(outFile, model, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
      outFile.close()

      // Build a text search dataset
      //jena_resources.createTextSearchDataset(model, "covid19_knowledge_graph.ttl")
    }
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

  def loadAFileJSON(aFiles: File, getName: String): Option[JSONObject] = {
    val aFile = aFiles.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean =
        name.equalsIgnoreCase(getName)
    })
    println(s"      loading ${aFile.mkString}")
    loadJSONFromFile(aFile.head)
  }
}