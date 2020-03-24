package gov.nasa.jpl.covid19_textmining_kaggle

import java.io.{IOException, StringReader}

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.simple.JSONArray
import org.json.simple.parser.JSONParser

object openie_operations {

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

  def loadJSONFromString(string: String): Option[JSONArray] = {
    val parser = new JSONParser
    try Some(parser.parse(new StringReader(string)).asInstanceOf[JSONArray])
    catch {
      case e: Exception =>
        println(s"ERROR: $string: ${e.getMessage}")
        None
    }
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

}
