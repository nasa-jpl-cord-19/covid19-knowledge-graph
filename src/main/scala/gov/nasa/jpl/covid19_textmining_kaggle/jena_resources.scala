package gov.nasa.jpl.covid19_textmining_kaggle

import java.util.Locale
import java.util.UUID.randomUUID

import com.ibm.icu.text.BreakIterator
import gov.nasa.jpl.covid19_knowledge_graph.covid19_knowledge_graph.COVID_NS
import org.apache.jena.atlas.json.JsonNull
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.query.text.{EntityDefinition, TextDatasetFactory, TextIndexConfig}
import org.apache.jena.query.{DatasetFactory, ReadWrite}
import org.apache.jena.rdf.model.{Model, RDFNode, Resource, ResourceFactory}
import org.apache.jena.vocabulary.RDFS
import org.apache.lucene.store.NIOFSDirectory
import org.json.simple.{JSONArray, JSONObject}
import util.control.Breaks._

object jena_resources {

  def createTextSearchDataset(model: Model, file: String) = {
    // Base data
    val ds1 = DatasetFactory.create(model)
    // Define the index mapping
    val entDef = new EntityDefinition("uri", "text")
    entDef.setPrimaryPredicate(RDFS.label.asNode)
    // Lucene, in memory.
    val dir = new NIOFSDirectory(java.nio.file.Paths.get("."));
    // Join together into a dataset
    val ds = TextDatasetFactory.createLucene(ds1, dir, new TextIndexConfig(entDef))
    ds.begin(ReadWrite.WRITE)
    ds.commit()
    ds.end()
    dir.close()
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

  def createExtractionGroup(model: Model, sentenceIterator: Iterator[String], paperId: String): RDFNode = {
    val uuidG = randomUUID().toString
    val extractionGroupResource = model.createResource(COVID_NS + uuidG,
      ResourceFactory.createResource(model.expandPrefix("covid:OpenIEExtractionGroup")))
    extractionGroupResource.addProperty(
      ResourceFactory.createProperty(model.expandPrefix("schema:isPartOf")),
      ResourceFactory.createProperty(model.expandPrefix(s"covid:$paperId")))
    while (sentenceIterator.hasNext) {
      val sentence = sentenceIterator.next.toString
        .replace("+", "")
        .replace("-", "")
        .replaceAll("[^\\x00-\\x7F]", "")
      if(sentence.contains(" ")){
        val extractions = openie_operations.postTextToOpenIE(sentence)
        val iterator = extractions.iterator
        while (iterator.hasNext) {
          val extractionArray = iterator.next.asInstanceOf[JSONArray]
          val extractionIterator = extractionArray.iterator
          while (extractionIterator.hasNext) {
            val uuid = randomUUID().toString
            val extractionResource = model.createResource(COVID_NS + uuid,
              ResourceFactory.createResource(model.expandPrefix("covid:OpenIEExtraction")))
            extractionResource.addProperty(
              ResourceFactory.createProperty(model.expandPrefix("covid:wasExtractedFrom")),
              ResourceFactory.createProperty(model.expandPrefix(s"covid:$paperId")))
            extractionResource.addProperty(
              ResourceFactory.createProperty(model.expandPrefix("schema:isPartOf")),
              ResourceFactory.createProperty(model.expandPrefix(s"covid:$uuidG")))
            val extractionObj = extractionIterator.next().asInstanceOf[JSONObject]
            val sentence = extractionObj.get("sentence").asInstanceOf[String]
            extractionResource.addProperty(
              ResourceFactory.createProperty(model.expandPrefix("covid:hasSentence")),
              ResourceFactory.createLangLiteral(sentence, "en"))
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
    }
    extractionGroupResource
  }

  def createAuthorsResource(authors: JSONArray, model: Model, paperId: String): RDFNode = {
    val authorsIterator = authors.iterator
    val uuidAs = randomUUID().toString
    val authorsResource = model.createResource(COVID_NS + uuidAs,
      ResourceFactory.createResource(model.expandPrefix("covid:AuthorsGroup")))
    authorsResource.addProperty(
      ResourceFactory.createProperty(model.expandPrefix("schema:isPartOf")),
      ResourceFactory.createProperty(model.expandPrefix(s"covid:$paperId")))
    while (authorsIterator.hasNext) {
      val uuidA = randomUUID().toString
      val authorResource = model.createResource(COVID_NS + uuidA,
        ResourceFactory.createResource(model.expandPrefix("schema:Person")))
      val author = authorsIterator.next().asInstanceOf[JSONObject]
      val first = author.get("first").asInstanceOf[String]
      authorResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:givenName")), first)
      val middleNames = author.get("middle").asInstanceOf[JSONArray]
      val middleNameIterator = middleNames.iterator
      while (middleNameIterator.hasNext) {
        authorResource.addProperty(
          ResourceFactory.createProperty(model.expandPrefix("schema:additionalName")),
          middleNameIterator.next.asInstanceOf[String])
      }
      val last = author.get("last").asInstanceOf[String]
      authorResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:familyName")), last)
      val suffix = author.get("suffix").asInstanceOf[String]
      authorResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:honorificSuffix")), suffix)
      //      val affiliation = author.get("affiliation").asInstanceOf[JSONObject]
      //      authorAffiliationResource.addProperty(ResourceFactory.createProperty(
      //        model.expandPrefix("schema:affiliation")), authorResource)
      //      val laboratory =
      //      authorResource.addProperty(ResourceFactory.createProperty(
      //        model.expandPrefix("schema:affiliation")), authorResource)
      val email = author.get("email").asInstanceOf[String]
      authorResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:email")), email)
      authorResource.addProperty(
        ResourceFactory.createProperty(model.expandPrefix("schema:isPartOf")),
        ResourceFactory.createProperty(model.expandPrefix(s"covid:$uuidAs")))
      authorsResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:author")), authorResource)
    }
    authorsResource
  }

  def createAnnieExtractions(aFileJSON: JSONObject, model: Model, paperResource: Resource) = {
    val iterator = aFileJSON.keySet.iterator
    while (iterator.hasNext) {
      val key = iterator.next.asInstanceOf[String]
      breakable {
        if ("abstract" == key ||
          "body_text" == key ||
          "sha" == key) {
          break
        }
        if (aFileJSON.get(key) != null) {
          checkType(key, aFileJSON.get(key), model, paperResource)
        }
      }
    }
  }

  def processString(key: String, fieldValue: String, model: Model, paperResource: Resource) = {
    if (fieldValue.startsWith("http")) {
      paperResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix(s"covid:$key")), fieldValue)
    } else {
      paperResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix(s"covid:$key")), fieldValue, "en")
    }
  }

  def processArray(key: String, fieldValue: JSONArray, model: Model, paperResource: Resource) = {
    val fieldIterator = fieldValue.iterator
    while (fieldIterator.hasNext) {
      paperResource.addProperty(
        ResourceFactory.createProperty(model.expandPrefix(s"covid:$key")),
        fieldIterator.next.asInstanceOf[String], "en")
    }
  }

  def processBoolean(key: String, boolean: Boolean, model: Model, paperResource: Resource) = {
    paperResource.addProperty(
      ResourceFactory.createProperty(model.expandPrefix(s"covid:$key")),
      ResourceFactory.createTypedLiteral(java.lang.Boolean.toString(boolean), XSDDatatype.XSDboolean))
  }

  def checkType[T](key: String, fieldValue: T, model: Model, paperResource: Resource) = fieldValue match {
    case _: String    => processString(key, fieldValue.asInstanceOf[String], model, paperResource)
    case _: JSONArray => processArray(key, fieldValue.asInstanceOf[JSONArray], model, paperResource)
    case _: Boolean => processBoolean(key, fieldValue.asInstanceOf[Boolean], model, paperResource)
    case e: Exception =>
      println(s"ERROR: ${e.getMessage}")
      None
  }

  def createPaperResource(json: JSONObject, model: Model, aFileJSON:  JSONObject) {
    val paperId = json.get("paper_id").asInstanceOf[String]
    val paperResource = model.createResource(COVID_NS + paperId,
      ResourceFactory.createResource(model.expandPrefix("schema:ScholarlyArticle")))
    paperResource.addProperty(ResourceFactory.createProperty(
      model.expandPrefix("rdf:type")), ResourceFactory.createProperty(
      model.expandPrefix("owl:NamedIndividual")))

    createAnnieExtractions(aFileJSON, model, paperResource)

    val metadata = json.get("metadata").asInstanceOf[JSONObject]
    val title = metadata.get("title").asInstanceOf[String]
    paperResource.addProperty(ResourceFactory.createProperty(
      model.expandPrefix("schema:headline")), title, "en")

    val authors = metadata.get("authors").asInstanceOf[JSONArray]
    val authorsResource = createAuthorsResource(authors, model, paperId)
    paperResource.addProperty(ResourceFactory.createProperty(
      model.expandPrefix("covid:hasAuthorsGroup")), authorsResource)
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
      val sectionObj = bodyIterator.next.asInstanceOf[JSONObject]
      val section = sectionObj.get("section").asInstanceOf[String]
      paperResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("schema:articleSection")), title, "en")
      val sectionText = sectionObj.get("text").asInstanceOf[String]
      //the section text is huge and really bloats the file.
      //          paperResource.addProperty(ResourceFactory.createProperty(
      //            model.expandPrefix("schema:text")), sectionText, "en")
      val sentences = getSentences(sectionText)
      val sentenceIterator = sentences.iterator
      val extractionGroup = createExtractionGroup(model, sentenceIterator, paperId)
      paperResource.addProperty(ResourceFactory.createProperty(
        model.expandPrefix("covid:hasOpenIEExtractionGroup")), extractionGroup)
    }
  }
}