package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.SinglePass
import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.spark.SparkContext
import grizzled.slf4j.Logger
import org.json4s.MappingException

import scala.collection.mutable

/**
  * Created by ifpelset on 3/23/17.
  */
class Algorithm(val ap: AlgorithmParams)
  // extends PAlgorithm if Model contains RDD[]
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  val mqReceiver = if (Algorithm.shouldBeLoaded) new MQReceiver(ap) else null
  val mqSender = if (Algorithm.shouldBeLoaded) new MQSender else null
//  val categoryModelMap: mutable.Map[String, SinglePass] = if (Algorithm.shouldBeLoaded) mqReceiver.categoryModelMap else null

  if (!Algorithm.shouldBeLoaded) {
    Algorithm.shouldBeLoaded = true
  }

  def train(sc: SparkContext, data: PreparedData): Model = {
    new Model(data.tfIdf, data.namedEntitySet, ap)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    var predictedResult: PredictedResult = null
    try {
      predictedResult = query.action_type match {
        case 0 => {
          var newsArray: Array[News] = null
          try {
            newsArray = query.news.get
          } catch {
            case e: NoSuchElementException => throw MappingException(getClass.getName + ": The field news(id or timestamp or content) is required.", e)
          }

          //model.cluster(categoryModelMap, newsArray)
          mqSender.sendObject(MQMessage(0, Some(ClusterMessage(model, newsArray)), None))

          val categoryModelMap = mqReceiver.categoryModelMap
          PredictedResult(Some(categoryModelMap.keys.size), None, None, None)
        }
        case 1 => {
          val category = query.category.getOrElse("")
          val minNewsCount = query.min_news_count.getOrElse(1)
          val maxClusterCount = query.max_cluster_count.getOrElse(10)
          // 可能 throw NoSuchElementException
          val categoryModelMap = mqReceiver.categoryModelMap
          val singlePassModel = categoryModelMap(category)
          PredictedResult(None, Some(singlePassModel.getSummaries(minNewsCount, maxClusterCount)), None, None)
        }
        case 2 => {
          val id = query.id.get
          val category = query.category.get

          mqSender.sendObject(MQMessage(1, None, Some(ModifyMessage(id, category))))

          PredictedResult(None, None, Some(id), None)
        }
        case 3 => { // 新闻推荐
          val id = query.id.get
          val topN = query.top.getOrElse(10)

          // 首先查找该id属于的模型
          val categoryModelMap = mqReceiver.categoryModelMap
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
//        case 4 =>  { // for debug
//          categoryModelMap.values.zipWithIndex.foreach(s => {
//            println(s"category id: ${s._2}, element queue size: ${s._1.elementQueue.size}")
//          })
//          PredictedResult(None, None, Some(-9), None)
//        }
        case _ => throw MappingException(getClass.getName + ": Unknown action.", new Exception("Unknown action."))
      }
    } catch {
      case e: NullPointerException => throw MappingException(getClass.getName + ": The field action_type is required.", e)
      case e: MappingException => throw e
      case e: NoSuchElementException => throw MappingException(getClass.getName + s": The category(or id) is not exists.", e)
      case e: Exception => throw MappingException(getClass.getName + ": Unknown error.", e)
    }
    predictedResult
  }
}

object Algorithm {
  var shouldBeLoaded = false // 用来控制pio deploy的第二次创建Algorithm执行的代码
}
