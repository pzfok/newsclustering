package com.dfsx.newsclustering

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{EmptyActualResult, EmptyEvaluationInfo, PDataSource}
import org.apache.predictionio.data.store.PEventStore
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
  * Created by ifpelset on 3/23/17.
  */
class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val observationRDD: RDD[Observation] = PEventStore.find(
      appName = dsp.appName,
      entityType = Some("content"), // specify data entity type
      eventNames = Some(List("news")) // specify data event name

      // Convert collected RDD of events to and RDD of Observation
      // objects.
    )(sc).map(x => {
      try {
        Observation(
          x.properties.get[String]("label"),
          x.properties.get[String]("title"),
          x.properties.get[String]("content")
        )
      } catch {
        case e: Exception => {
          logger.error(s"Failed to get properties ${x.properties} " +
            s" Exception: ${e}.")
          throw e
        }
      }
    }).cache()

    new TrainingData(observationRDD)
  }
}
