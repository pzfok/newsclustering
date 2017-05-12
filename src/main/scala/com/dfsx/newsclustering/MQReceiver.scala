package com.dfsx.newsclustering

import java.io._
import javax.jms.{Message, MessageListener, ObjectMessage, Session}

import com.dfsx.newsclustering.singlepass.SinglePass
import grizzled.slf4j.Logger
import org.apache.activemq.ActiveMQConnectionFactory

import scala.collection.mutable

/**
  * Created by ifpelset on 4/17/17.
  */
class MQReceiver(ap: AlgorithmParams) extends MessageListener {
  @transient lazy val logger = Logger[this.type]

  // 新闻类别与聚类模型的映射
  // 当新闻类别为""时，代表全局聚类，否则为该新闻类别下的聚类
  // 在增量聚类的过程中修改该映射
  // 默认创建一个全局聚类模型映射
  var categoryModelMap: mutable.Map[String, SinglePass] = mutable.HashMap("" -> SinglePass(ap.threshold, ap.maxClusterElementsCount))//ap.categories.map(c => (c, SinglePass(ap.threshold, ap.maxClusterElementsCount))).toMap

  val modeDir = "/var/local/data"
  val modelFilename = "/var/local/data/cluster-model"

  initModel()

  val connectionFactory = new ActiveMQConnectionFactory()
  connectionFactory.setTrustAllPackages(true)
  val connection = connectionFactory.createConnection()
  val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
  val dest = session.createQueue("ClusterQueue")
  val consumer = session.createConsumer(dest)
  consumer.setMessageListener(this)
  connection.start()

  override def onMessage(message: Message): Unit = {
    if (message.isInstanceOf[ObjectMessage]) {
      val objectMessage = message.asInstanceOf[ObjectMessage].getObject
      if (objectMessage.isInstanceOf[MQMessage]) {
        val mqMessage = objectMessage.asInstanceOf[MQMessage]

        mqMessage.msgType match {
          case 0 => { // cluster
            val clusterMessage = mqMessage.clusterMessage.get
            val model = clusterMessage.model
            val newsArray = clusterMessage.newsArray

            try {
              val categoryModelMapCopy = copy()
              model.cluster(categoryModelMapCopy, newsArray)
              categoryModelMap = categoryModelMapCopy
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
          case 1 => { // modify
            val modifyMessage = mqMessage.modifyMessage.get
            val id = modifyMessage.id
            val category = modifyMessage.category

            try {
              val categoryModelMapCopy = copy()
              modifyCategory(categoryModelMapCopy, id, category)
              categoryModelMap = categoryModelMapCopy
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
          case _ => {
            logger.warn("Unknown user-defined MQMessage type")
          }
        }

      } else {
        logger.warn("Unknown object message type")
      }
    } else {
      logger.warn("Unknown message type")
    }
  }

  def modifyCategory(categoryModelMapArg: mutable.Map[String, SinglePass], id: Long, category: String): Unit = {
    // 使用find找到一个就返回，不用考虑scala的不支持“正常的”break
    val categoryModelpair = categoryModelMapArg.find(pair => {
      val findResult = pair._2.elementQueue.find(e => e.asInstanceOf[singlepass.News].id == id)

      if (findResult.nonEmpty) {
        val news = findResult.get.asInstanceOf[singlepass.News]
        news.category = category

        // 修改之后要对该新闻重新聚类
        if (!categoryModelMapArg.contains(category)) {
          logger.debug(s"create ${category}'s SinglePass Model")
          categoryModelMapArg.put(category, SinglePass(ap.threshold, ap.maxClusterElementsCount))
        }

        val singlePassModel = categoryModelMapArg(category)

        // 先删除老的模型中的数据，再重新聚类，防止改变分类为自己原本的分类的问题
        pair._2.remove(news)

        singlePassModel.clustering(news)

        true
      } else {
        false
      }
    })

    // 若model中不存在任何一个聚类了，清理map中该(category,model)项
    if (categoryModelpair.nonEmpty) {
      val testPair = categoryModelpair.get
      val testCategory = testPair._1
      val testModel = testPair._2
      if (testModel.getClusterCount < 1) {
        logger.debug(s"remove ${testCategory} from singlePassModelMap")
        categoryModelMapArg.remove(testCategory)
      }
    }
  }

  def copy() : mutable.Map[String, SinglePass]= {
    //写入字节流
    val out = new ByteArrayOutputStream()
    val obs = new ObjectOutputStream(out)
    obs.writeObject(categoryModelMap)
    obs.close()

    //分配内存，写入原始对象，生成新对象
    val ios = new ByteArrayInputStream(out.toByteArray)
    val ois = new ObjectInputStream(ios) {
      // scala存在的问题 需要重写resolveClass方法  不然报告ClassNotFoundException
      override def resolveClass(desc: ObjectStreamClass): Class[_] = {
        try { Class.forName(desc.getName, false, getClass.getClassLoader) }
        catch { case _: ClassNotFoundException => super.resolveClass(desc) }
      }
    }
    //返回生成的新对象
    val cloneObj = ois.readObject()
    ois.close()
    cloneObj.asInstanceOf[mutable.Map[String, SinglePass]]
  }

  def save() : Unit = {
    logger.info("Program will be exited, save model")
    val tempModeFile = new File(modelFilename)
    val obs = new ObjectOutputStream(new FileOutputStream(tempModeFile))

    obs.writeObject(categoryModelMap)
    obs.close()
  }

  def initModel() : Unit = {
    val dir = new File(modeDir)
    if (!dir.exists()) {
      dir.mkdirs()
    }

    val modelFile = new File(modelFilename)
    if (!modelFile.exists()) { // 第一次没有模型
      logger.info("First start, have no model")
      modelFile.createNewFile()
    } else { // 后面要载入模型
      val ois = new ObjectInputStream(new FileInputStream(modelFile)) {
        // scala存在的问题 需要重写resolveClass方法  不然报告ClassNotFoundException
        override def resolveClass(desc: ObjectStreamClass): Class[_] = {
          try { Class.forName(desc.getName, false, getClass.getClassLoader) }
          catch { case _: ClassNotFoundException => super.resolveClass(desc) }
        }
      }
      //返回生成的新对象
      val cloneObj = ois.readObject()
      ois.close()
      if (categoryModelMap.isInstanceOf[mutable.Map[String, SinglePass]]) {
        logger.info(s"Loading model from ${modelFilename}")
        categoryModelMap = cloneObj.asInstanceOf[mutable.Map[String, SinglePass]]
      } else {
        logger.warn(s"${modelFilename} is wrong model file")
      }
    }

    // 设置程序退出回调 保存模型
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = save()
    })
  }
}
