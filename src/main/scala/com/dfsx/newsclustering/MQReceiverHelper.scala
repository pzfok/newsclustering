package com.dfsx.newsclustering

import com.dfsx.newsclustering.common.{FileUtils, ObjectUtils}
import com.dfsx.newsclustering.singlepass.SinglePass
import grizzled.slf4j.Logger

import scala.collection.mutable

/**
  * Created by ifpelset on 5/15/17.
  */
class MQReceiverHelper(val _algorithmParams: AlgorithmParams) {
  @transient lazy val logger = Logger[this.type]

  private val _modeDir = "/var/local/data"
  private val _modelFilename = _modeDir + "/cluster-model"

  // 新闻类别与聚类模型的映射
  // 当新闻类别为""时，代表全局聚类，否则为该新闻类别下的聚类
  // 在增量聚类的过程中修改该映射
  // 默认创建一个全局聚类模型映射
  private var _categoryModelMap: mutable.Map[String, SinglePass] = mutable.HashMap("" -> SinglePass(_algorithmParams.threshold, _algorithmParams.maxClusterElementsCount))
  private var _clusterReceiver: ClusterReceiver = new ClusterReceiver(_algorithmParams)
  private var _modifyReceiver: ModifyReceiver = new ModifyReceiver(_algorithmParams)

  initializeModel()

  def categoryModelMap: mutable.Map[String, SinglePass] = _categoryModelMap

  def doCluster(mQMessage: MQMessage): Unit = {
    val clusterMessage = mQMessage.clusterMessage.get
    val model = clusterMessage.model
    val newsArray = clusterMessage.newsArray

    try {
      val categoryModelMapCopy = cloneModel
      _clusterReceiver.cluster(model, categoryModelMapCopy, newsArray)
      _categoryModelMap = categoryModelMapCopy
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def doModify(mQMessage: MQMessage): Unit = {
    val modifyMessage = mQMessage.modifyMessage.get
    val id = modifyMessage.id
    val category = modifyMessage.category

    try {
      val categoryModelMapCopy = cloneModel
      _modifyReceiver.modifyNewsCategoryFromCategoryToModelMap(categoryModelMapCopy, id, category)
      _categoryModelMap = categoryModelMapCopy
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private def cloneModel: mutable.Map[String, SinglePass] = {
    ObjectUtils.cloneObject(_categoryModelMap).asInstanceOf[mutable.Map[String, SinglePass]]
  }

  private def saveModel(): Unit = {
    logger.info("Program will be exited, save model")
    ObjectUtils.writeObjectToFile(_categoryModelMap, _modelFilename)
  }

  private def registerSaveModelCallback(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = saveModel()
    })
  }

  private def loadModel(): Unit = {
    ObjectUtils.readObjectFromFile(_modelFilename) match {
      case model: mutable.Map[String, SinglePass] =>
        logger.info(s"Loading model from ${_modelFilename}")
        _categoryModelMap = model

      case None =>
        logger.info(s"${_modelFilename} is empty")

      case _ =>
        logger.warn(s"${_modelFilename} is wrong model file")
    }
  }

  private def initializeModel(): Unit = {
    FileUtils.createDirIfNotExist(_modeDir)
    FileUtils.createFileIfNotExist(_modelFilename)

    loadModel()

    registerSaveModelCallback()
  }
}
