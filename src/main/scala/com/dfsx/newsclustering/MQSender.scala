package com.dfsx.newsclustering

import javax.jms.{MessageProducer, ResourceAllocationException, Session}

import org.apache.activemq.ActiveMQConnectionFactory
import org.json4s.MappingException

/**
  * Created by ifpelset on 4/17/17.
  */
class MQSender {
  // ActiveMQConnectionFactory三参数：ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://localhost:61616"
  private var _session: Session = _
  private var _messageProducer: MessageProducer = _

  initialize()

  private def initialize(): Unit = {
    val connectionFactory = new ActiveMQConnectionFactory()
    val connection = connectionFactory.createConnection()

    _session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val dest = _session.createQueue("ClusterQueue")
    _messageProducer = _session.createProducer(dest)
  }

  def sendObject(obj: Serializable): Unit = {
    try {
      val message = _session.createObjectMessage(obj)
      _messageProducer.send(message)
    } catch {
      case e: ResourceAllocationException => throw MappingException(getClass.getName + ": cluster request too quickly.", e)
      case e: Exception => e.printStackTrace()
    }
  }
}
