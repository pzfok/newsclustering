package com.dfsx.newsclustering

import grizzled.slf4j.Logger
import org.json4s.MappingException

import scala.collection.mutable

/**
  * Created by ifpelset on 5/16/17.
  */
class AlgorithmHelper(val _mQSender: MQSender, val _mQReceiver: MQReceiver) {
  @transient lazy val logger = Logger[this.type]

  def handleClusterRequest(model: CorpusModel, query: Query): PredictedResult = {
    var newsArray: Array[News] = null
    try {
      newsArray = query.news.get
    } catch {
      case e: NoSuchElementException => throw MappingException(getClass.getName + ": The field news(id or timestamp or content) is required.", e)
    }

    val categoryModelMap = _mQReceiver.categoryModelMap
    _mQSender.sendObject(MQMessage(0, Some(ClusterMessage(model, newsArray)), None))

    PredictedResult(Some(categoryModelMap.keys.size), None, None, None)
  }

  def handleInfoRequest(query: Query): PredictedResult = {
    val category = query.category.getOrElse("")
    val minNewsCount = query.min_news_count.getOrElse(1)
    val maxClusterCount = query.max_cluster_count.getOrElse(10)
    // 可能 throw NoSuchElementException
    val categoryModelMap = _mQReceiver.categoryModelMap
    val singlePassModel = categoryModelMap(category)
    PredictedResult(None, Some(singlePassModel.getSummaries(minNewsCount, maxClusterCount)), None, None)
  }

  def handleModifyRequest(query: Query): PredictedResult = {
    val id = query.id.get
    val category = query.category.get

    _mQSender.sendObject(MQMessage(1, None, Some(ModifyMessage(id, category))))

    PredictedResult(None, None, Some(id), None)
  }

  def handleRecommendRequest(query: Query): PredictedResult = {
    val id = query.id.get
    val topN = query.top.getOrElse(10)

    // 首先查找该id属于的模型
    val categoryModelMap = _mQReceiver.categoryModelMap
    val categoryModelpair = categoryModelMap.find(pair => {
      val findResult = pair._2.elementQueue.find(e => e.asInstanceOf[singlepass.News].id == id)

      if (findResult.nonEmpty) {
        true
      } else {
        false
      }
    })

    if (categoryModelpair.isEmpty) {
      logger.error("categoryModelpair is empty")
      return PredictedResult(None, None, None, Some(mutable.ListBuffer[Long]()))
    }

    val pair = categoryModelpair.get
    val singlePassModel = pair._2

    PredictedResult(None, None, None, Some(singlePassModel.getTopNSimilarities(id, topN)))
  }
}
