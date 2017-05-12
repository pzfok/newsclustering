package com.dfsx.newsclustering

/**
  * Created by ifpelset on 3/23/17.
  */

case class News(
  id: Long,
  timestamp: Long,
  category: Option[String], // 增量聚类训练需要的新闻分类参数
  title: Option[String],
  content: String
) extends Serializable

case class Query(
  action_type: Int, // 0-增量训练，1-聚类结果查询，2-新闻类别修改
  news: Option[Array[News]],
  id: Option[Long], // 新闻类别修改或者查询该id最相似的几条新闻时的id参数
  category: Option[String], // 查询和修改时需要传递的类别参数
  min_news_count: Option[Int],
  max_cluster_count: Option[Int],
  top: Option[Int]
) extends Serializable
