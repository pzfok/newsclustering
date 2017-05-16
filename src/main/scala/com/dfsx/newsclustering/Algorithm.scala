package com.dfsx.newsclustering

import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.spark.SparkContext
import org.json4s.MappingException

/**
  * Created by ifpelset on 3/23/17.
  */
class Algorithm(val ap: AlgorithmParams)
  // extends PAlgorithm if Model contains RDD[]
  extends P2LAlgorithm[PreparedData, CorpusModel, Query, PredictedResult] {

  val _mQReceiver: MQReceiver = if (Algorithm._shouldBeLoaded) new MQReceiver(ap) else null
  val _mQSender: MQSender = if (Algorithm._shouldBeLoaded) new MQSender else null
  val _algorithmHelper: AlgorithmHelper = if (Algorithm._shouldBeLoaded) new AlgorithmHelper(_mQSender, _mQReceiver) else null

  if (!Algorithm._shouldBeLoaded) {
    Algorithm._shouldBeLoaded = true
  }

  def train(sc: SparkContext, data: PreparedData): CorpusModel = {
    new CorpusModel(data.tfIdfModel, data.namedEntitySet)
  }

  def predict(model: CorpusModel, query: Query): PredictedResult = {
    var predictedResult: PredictedResult = null
    try {
      predictedResult = query.action_type match {
        case 0 =>
          _algorithmHelper.handleClusterRequest(model, query)

        case 1 =>
          _algorithmHelper.handleInfoRequest(query)

        case 2 =>
          _algorithmHelper.handleModifyRequest(query)

        case 3 =>
          _algorithmHelper.handleRecommendRequest(query)

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
  var _shouldBeLoaded = false // 用来控制pio deploy的第二次创建Algorithm执行的代码
}
