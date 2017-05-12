package com.dfsx.newsclustering.singlepass

import org.apache.spark.mllib.linalg.Vector


/**
  * Created by ifpelset on 3/9/17.
  */
abstract class Element extends Serializable {
  val features: Vector
  val tfVector: Vector
}
