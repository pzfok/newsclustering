package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.SinglePass
import grizzled.slf4j.Logger
import org.apache.spark.mllib.linalg.{SparseVector, Vectors}

import scala.collection.mutable

/**
  * Created by ifpelset on 5/16/17.
  */
class ClusterReceiver(val _algorithmParams: AlgorithmParams) {
  @transient lazy val logger = Logger[this.type]

  private def putCategoryToModelMapIfNotExist(category: String, categoryToModelMap: mutable.Map[String, SinglePass]): Unit = {
    if (!categoryToModelMap.contains(category)) {
      logger.debug(s"create ${category}'s SinglePass Model")
      categoryToModelMap.put(category, SinglePass(_algorithmParams.threshold, _algorithmParams.maxClusterElementsCount))
    }
  }

  def calculateWeightByNamedEntity(tfidfVector: SparseVector, namedEntity: Set[Int]): Array[Double] = {
    val valuesArray: Array[Double] = tfidfVector.values
    tfidfVector.indices.foreach(index => {
      if (namedEntity.contains(index)) {
        val valueIndex = tfidfVector.indices.indexOf(index)
        valuesArray(valueIndex) *= 4
      }
    })

    valuesArray
  }

  def cluster(model: CorpusModel, categoryToModelMap: mutable.Map[String, SinglePass], news: News): Unit = {
    val category = news.category.getOrElse("")
    val content = news.content

    val title = news.title.getOrElse(content)

    val realContent = if (title == content) content else title + content
    // attention: client must be set Content-type: charset=UTF-8
    val tfVector = model.tfIdf.hasher.hashTF(realContent)
    val tfidfVector: SparseVector = model.tfIdf.idf.transform(tfVector).asInstanceOf[SparseVector]

    // weight tfidf vector
    val feature = Vectors.sparse(
      tfidfVector.size,
      tfidfVector.indices,
      calculateWeightByNamedEntity(tfidfVector, model.namedEntity)
    )

    putCategoryToModelMapIfNotExist(category, categoryToModelMap)

    categoryToModelMap(category)
      .clustering(
        singlepass.News(
          news.id, news.timestamp, category, title,
          content, feature, tfVector
        )
      )
  }

  def cluster(model: CorpusModel, categoryToModelMap: mutable.Map[String, SinglePass], newsArray: Array[News]): Unit = {
    newsArray.foreach(news => {
      cluster(model, categoryToModelMap, news)
    })
  }
}
