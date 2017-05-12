package com.dfsx.newsclustering

import org.apache.predictionio.controller.Params

/**
  * Created by ifpelset on 3/23/17.
  */
case class AlgorithmParams(
  threshold: Double,
  maxClusterElementsCount: Option[Int]
) extends Params
