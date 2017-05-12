package com.dfsx.newsclustering.singlepass

import org.apache.spark.mllib.linalg.Vector

/**
  * Created by ifpelset on 3/9/17.
  */
case class News(
   id: Long,
   timestamp: Long,
   var category: String, // 新闻类别可修改
   title: String,
   content: String,
   features: Vector,
   tfVector: Vector
) extends Element {
  override def hashCode(): Int = id.toInt
}
