
import java.io.{StringReader, ByteArrayOutputStream}
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller



import scala.reflect.ClassTag

//import org.apache.nifi.web.api.dto._
//import org.apache.nifi.web.api.dto._

import org.apache.nifi.web.api.dto._
import org.apache.nifi.web.api.dto.search._
import org.apache.nifi.web.api.entity._


//TODO We can re-use one context (loaded up with all the entity types we plan on handling) and one marshaller for xml and one for json,
//once we knew that contexts and marshallers are thread safe. Note that the json context and the xml context can be seeded with the
//ability to parse marshall several different types, like so:
//val context = new JSONJAXBContext(config, classOf[SearchResultsEntity], classOf[TemplateEntity])
//Also should be able to end up with less copy-paste
object JaxBConverters {

  object XmlConverters {
    def toXmlString[T](jaxBObj: T)(implicit CT: ClassTag[T]): String = {

      val context = JAXBContext.newInstance(CT.runtimeClass)
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream
      context.createMarshaller().marshal(jaxBObj, baos)
      try {
        baos.toString("UTF-8")
      } finally {
        baos.close()
      }
    }

    def fromXmlString[T](str: String)(implicit CT: ClassTag[T]): T = {
      val context = JAXBContext.newInstance(CT.runtimeClass)
      val stringReader = new StringReader(str)
      try {
        //TODO Perhaps we should make our return a Try[T]. unmarshall only gives us an Object return
        //unless we give it a some Java-ish xml argument. But in that case we should probably make the analogous fromXmlString
        //a Try too.
        context.createUnmarshaller().unmarshal(stringReader).asInstanceOf[T]   //(stringReader, CT.runtimeClass).asInstanceOf[T]
      } finally {
        stringReader.close
      }

    }


  }

  object JsonConverters {
    import com.sun.jersey.api.json._


    //Got example of how to use JSONConfiguration and JSONJAXBContext from http://krasserm.blogspot.com/2012/02/using-jaxb-for-xml-and-json-apis-in.html
    //rootUnwrapping was originally false.  I think this variable tells whether we expect an outer wrapper, and, at least for the SearchResultsEntity that
    //I've received so far from my local NIFI server, NFI results don't come with an outer wrapper.
    lazy val jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(true).build()
    //TODO maybe just have one jsonContext like this, assuming JSONJAXBContext is thread safe:
    //val context = new JSONJAXBContext(config, classOf[SearchResultsEntity], classOf[TemplateEntity])

    def toJsonString[T](jaxBObj: T)(implicit CT: ClassTag[T]): String = {
      val context = new JSONJAXBContext(jsonConfiguration, CT.runtimeClass)
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream
      context.createJSONMarshaller().marshallToJSON(jaxBObj, baos)
      try {
        baos.toString("UTF-8")
      } finally {
        baos.close()
      }
    }

    //import scala.reflect.ClassTag
    //import scala.reflect._
    def fromJsonString[T](str: String)(implicit CT: ClassTag[T]): T = {
      val context = new JSONJAXBContext(jsonConfiguration, CT.runtimeClass)
      val stringReader = new StringReader(str)
      try {
        //Note: classTag[T].runtimeClass also works here, so long as we import scala.reflect._
        context.createJSONUnmarshaller().unmarshalFromJSON(stringReader, CT.runtimeClass).asInstanceOf[T]
      } finally {
        stringReader.close
      }

    }

  }



}
