package com.dfsx.newsclustering

import org.apache.spark.rdd.RDD

/**
  * Created by ifpelset on 3/23/17.
  */
class TrainingData(
  val data: RDD[Observation]
) extends Serializable
