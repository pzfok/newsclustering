package com.dfsx.newsclustering

import javax.jms.{ResourceAllocationException, Session}

import org.apache.activemq.ActiveMQConnectionFactory
import org.json4s.MappingException

/**
  * Created by ifpelset on 4/17/17.
  */
class MQSender {
  // ActiveMQConnectionFactory三参数：ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://localhost:61616"
  val connectionFactory = new ActiveMQConnectionFactory()
  val connection = connectionFactory.createConnection()
  val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
  val dest = session.createQueue("ClusterQueue")
  val producer = session.createProducer(dest)

  def sendObject(obj: Serializable): Unit = {
    try {
      val message = session.createObjectMessage(obj)
      producer.send(message)
    } catch {
      case e: ResourceAllocationException => throw MappingException(getClass.getName + ": Cluster request too quickly.", e)
      case e: Exception => e.printStackTrace()
    }
  }
}
