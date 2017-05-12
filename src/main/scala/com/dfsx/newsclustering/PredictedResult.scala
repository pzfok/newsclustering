package com.dfsx.newsclustering

import scala.collection.mutable.ListBuffer

/**
  * Created by ifpelset on 3/23/17.
  */
case class ClusterInfo(
  tag_name: String,
  news_of_clusters: ListBuffer[Long]
) extends Serializable

case class PredictedResult(
  category_count: Option[Int],
  result: Option[ListBuffer[ClusterInfo]],
  id: Option[Long],
  recommendations: Option[ListBuffer[Long]]
) extends Serializable
