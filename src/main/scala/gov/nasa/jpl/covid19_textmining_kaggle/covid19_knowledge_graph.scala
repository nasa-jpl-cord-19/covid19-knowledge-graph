package gov.nasa.jpl.covid19_knowledge_graph

import java.io._
import java.text.BreakIterator
import java.util.Locale
import java.util.UUID.randomUUID

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.{Model, RDFNode, ResourceFactory}
import org.apache.jena.riot.RDFDataMgr
import org.json.simple.parser.JSONParser
import org.json.simple.{JSONArray, JSONObject}

object covid19_knowledge_graph {

  final val COVID_NS: String = "http://knowledge.jpl.nasa.gov/COVID-19/"

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      main(args.head)
    }
    else println("Missing directory argument")
  }

  def getSentences(text: String): List[String] = {
    var sentences: List[String] = List()
    val iterator: BreakIterator = BreakIterator.getSentenceInstance(Locale.US)
    val source: String = text
    iterator.setText(source)
    var start: Int = iterator.first
    val sb: StringBuffer = new StringBuffer
    var end: Int = iterator.next
    while (end != BreakIterator.DONE) {
      val sentence: String = source.substring(start, end)
      sentences = sentences ++ List(sentence)
      start = end
      end = iterator.next
    }
    sentences
  }

  class CustomResponseHandler extends ResponseHandler[String] {
    @throws[IOException]
    def handleResponse(response: HttpResponse): String = {
      //Get the status of the response
      val status = response.getStatusLine.getStatusCode
      if (status >= 200 && status < 300) {
        val entity = response.getEntity
        if (entity == null) ""
        else EntityUtils.toString(entity)
      }
      else "" + status
    }
  }

  def loadJSONFromString(string: String): Option[JSONArray] = {
    val parser = new JSONParser
    try Some(parser.parse(new StringReader(string)).asInstanceOf[JSONArray])
    catch {
      case e: Exception =>
        println(s"ERROR: $string: ${e.getMessage}")
        None
    }
  }

  def postTextToOpenIE(passage: String): Option[JSONArray] = {
    val httpclient = HttpClients.createDefault()
    val httpPost = new HttpPost("http://localhost:8000/getExtraction")
    //httpPost.setHeader("Accept", "application/json")
    //httpPost.setHeader("Content-type", "application/json")
    val stringEntity = new StringEntity(passage)
    httpPost.setEntity(stringEntity)
    val responseHandler = new CustomResponseHandler
    loadJSONFromString(httpclient.execute(httpPost, responseHandler))
  }

  def createExtractionGroup(model: Model, sentenceIterator: Iterator[String], paperId: String): RDFNode = {
    val uuidG = randomUUID().toString
    val extractionGroupResource = model.createResource(COVID_NS + uuidG,
      ResourceFactory.createResource(model.expandPrefix("covid:ExtractionGroup")))
    extractionGroupResource.addProperty(
      ResourceFactory.createProperty(model.expandPrefix("schema:isPartOf")),
      ResourceFactory.createProperty(model.expandPrefix(s"covid:$paperId")))
    while (sentenceIterator.hasNext) {
      val sentence = sentenceIterator.next.toString
      val extractions = postTextToOpenIE(sentence
        .replace("+", "")
        .replace("-", "")
        .replaceAll("[^\\x00-\\x7F]", ""))
      println(extractions.mkString)
      val iterator = extractions.iterator
      while (iterator.hasNext) {
        val uuid = randomUUID().toString
        val extractionResource = model.createResource(COVID_NS + uuid,
          ResourceFactory.createResource(model.expandPrefix("covid:Extraction")))
        extractionResource.addProperty(
          ResourceFactory.createProperty(model.expandPrefix("covid:extractedFrom")),
          ResourceFactory.createProperty(model.expandPrefix(s"covid:$paperId")))
        //println(iterator.next.toJSONString)
        val extractionArray = iterator.next.asInstanceOf[JSONArray]
        val extractionIterator = extractionArray.iterator
        val counter = 0
        while (extractionIterator.hasNext) {
          val extractionObj = extractionIterator.next().asInstanceOf[JSONObject]
          val confidence = java.lang.Double.toString(extractionObj.get("confidence").asInstanceOf[Double])
          extractionResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("covid:hasConfidence")),
            ResourceFactory.createTypedLiteral(confidence, XSDDatatype.XSDdouble))
          val extraction = extractionObj.get("extraction").asInstanceOf[JSONObject]
          val arg1 = extraction.get("arg1").asInstanceOf[JSONObject]
          val arg1Text = arg1.get("text").asInstanceOf[String]
          extractionResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("covid:hasArg1")),
            ResourceFactory.createLangLiteral(arg1Text, "en"))
          val rel = extraction.get("rel").asInstanceOf[JSONObject]
          val relText = rel.get("text").asInstanceOf[String]
          extractionResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("covid:hasRelation")),
            ResourceFactory.createLangLiteral(relText, "en"))
          val arg2s = extraction.get("arg2s").asInstanceOf[JSONArray]
          val arg2sIterator = arg2s.iterator
          while (arg2sIterator.hasNext) {
            val arg2Obj = arg2sIterator.next.asInstanceOf[JSONObject]
            val arg2Text = arg2Obj.get("text").asInstanceOf[String]
            extractionResource.addProperty(
              ResourceFactory.createProperty(model.expandPrefix("covid:hasArg2")),
              ResourceFactory.createLangLiteral(arg2Text, "en"))
          }
          val context = extraction.get("context").asInstanceOf[JSONObject]
          if(context != null) {
            val contextText = context.get("text").asInstanceOf[String]
            extractionResource.addProperty(
              ResourceFactory.createProperty(model.expandPrefix("covid:hasContext")),
              ResourceFactory.createLangLiteral(contextText, "en"))
          }
          val negated = java.lang.Boolean.toString(extraction.get("negated").asInstanceOf[Boolean])
          extractionResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("covid:isNegated")),
            ResourceFactory.createTypedLiteral(negated, XSDDatatype.XSDboolean))
          val passive = java.lang.Boolean.toString(extraction.get("passive").asInstanceOf[Boolean])
          extractionResource.addProperty(
            ResourceFactory.createProperty(model.expandPrefix("covid:isPassive")),
            ResourceFactory.createTypedLiteral(passive, XSDDatatype.XSDboolean))
          extractionGroupResource.addProperty(ResourceFactory.createProperty(
            model.expandPrefix("covid:hasExtraction")), extractionResource)
        }
      }
    }
    extractionGroupResource
  }

  def createPaperResource(json: JSONObject, model: Model) {
    val paperId = json.get("paper_id").asInstanceOf[String]
    val paperResource = model.createResource(COVID_NS + paperId,
      ResourceFactory.createResource(model.expandPrefix("schema:ScholarlyArticle")))
    paperResource.addProperty(ResourceFactory.createProperty(
      model.expandPrefix("rdf:type")), ResourceFactory.createProperty(
      model.expandPrefix("owl:NamedIndividual")))

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
      val sectionText = section.get("text").asInstanceOf[String]
      //the section text is huge and really bloats the file.
      //          paperResource.addProperty(ResourceFactory.createProperty(
      //            model.expandPrefix("schema:text")), sectionText, "en")
      val sentences = getSentences(sectionText)
      val sentenceIterator = sentences.iterator
      val extractionGroup = createExtractionGroup(model, sentenceIterator, paperId)
      paperResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("covid:hasExtractionGroup")), extractionGroup)
    }
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
        createPaperResource(json, model)

      }
      val outFile = new BufferedWriter(new FileWriter(new File("covid19_knowledge_graph.ttl")))
      RDFDataMgr.write(outFile, model, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY)
      outFile.close()
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