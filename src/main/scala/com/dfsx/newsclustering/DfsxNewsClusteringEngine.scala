package com.dfsx.newsclustering

import org.apache.predictionio.controller.{Engine, EngineFactory}

/**
  * Created by ifpelset on 3/23/17.
  */
object DfsxNewsClusteringEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("SinglePass" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
