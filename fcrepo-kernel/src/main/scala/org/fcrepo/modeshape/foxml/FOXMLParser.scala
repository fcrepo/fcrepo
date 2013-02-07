package org.fcrepo.modeshape.foxml

import java.io.ByteArrayInputStream
import java.io.InputStream
import scala.language.postfixOps
import scala.xml.XML

import org.modeshape.common.logging.Logger
import org.modeshape.jcr.api.JcrTools

import javax.jcr.Node

class FOXMLParser {

  val jcrTools: JcrTools = new JcrTools()

  def parse(input: InputStream, objNode: Node) {

    val log: Logger = Logger.getLogger("FOXMLParser");

    log.debug("Operating to alter node: " + objNode.getPath())

    val foxmlObj = XML.load(input)
    //log.debug("Found object XML: \n" + foxmlObj.toString)

    objNode.addMixin("fedora:object")

    val objProperties = foxmlObj \ "objectProperties" \ "property"
    /*for (property <- objProperties) {
      log.debug("Found object property: " + property.toString)
    }*/
    val ownerProperty = objProperties filter (p => ((p.\("@NAME")) text) == "info:fedora/fedora-system:def/model#ownerId")
    val ownerId = (ownerProperty \ "@VALUE") text

    //log.debug("Found owner ID: " + ownerId)

    objNode.setProperty("fedora:ownerId", ownerId)

    for (datastream <- foxmlObj \\ "datastream") {
      //log.debug("Found datastream: " + datastream.toString)
      val dsId = (datastream \ "@ID").text
      val controlGroup = datastream \ "@CONTROL_GROUP" 
      //log.debug("Found control group: " + controlGroup) 
      var latestVersion: String = null
      if (controlGroup.toString == "X") {
        latestVersion = (datastream \\ "xmlContent" \ "_").head.toString
        log.debug("Found content: \n" + latestVersion)
      } else {
        // insert placeholder
    	  	latestVersion = "PLACEHOLDER"
      }
      val dsNode = jcrTools.uploadFile(objNode.getSession(), objNode.getPath() + "/" + dsId, new ByteArrayInputStream(latestVersion.getBytes))
      dsNode.addMixin("fedora:datastream")
      dsNode.setProperty("fedora:contentType",
        (datastream \ "datastreamVersion").head \ "@MIMETYPE" text)
    }

  }

  def report = {
    "FOXML parser for ffmodeshapeprototype"
  }

}