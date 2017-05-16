package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.SinglePass
import grizzled.slf4j.Logger
import javax.jms._
import org.apache.activemq.ActiveMQConnectionFactory

import scala.collection.mutable

/**
  * Created by ifpelset on 4/17/17.
  */
class MQReceiver(algorithmParams: AlgorithmParams) extends MessageListener {
  @transient private lazy val _logger = Logger[this.type]

  private var _mqConnection: Connection = _
  private var _mqReceiverHelper: MQReceiverHelper = new MQReceiverHelper(algorithmParams)

  start()

  def start(): Unit = {
    val connectionFactory = new ActiveMQConnectionFactory()
    connectionFactory.setTrustAllPackages(true)

    _mqConnection = connectionFactory.createConnection()

    val session = _mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val dest = session.createQueue("ClusterQueue")
    val consumer = session.createConsumer(dest)
    consumer.setMessageListener(this)

    _mqConnection.start()
  }

  def stop(): Unit = {
    _mqConnection.stop()
    _mqConnection.close()
  }

  override def onMessage(message: Message): Unit = {
    message match {
      case objectMessage: ObjectMessage =>
        objectMessage.getObject match {
          case mQMessage: MQMessage =>
            mQMessage.msgType match {
              case 0 =>
                _mqReceiverHelper.doCluster(mQMessage)

              case 1 =>
                _mqReceiverHelper.doModify(mQMessage)

              case _ =>
                _logger.warn("Unknown user-defined MQMessage type")
            }
          case _ =>
            _logger.warn("Unknown object message type")
        }
      case _ =>
        _logger.warn("Unknown message type")
    }
  }

  def categoryModelMap: mutable.Map[String, SinglePass] = _mqReceiverHelper.categoryModelMap
}
