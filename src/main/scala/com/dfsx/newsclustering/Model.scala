package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.SinglePass
import org.apache.spark.mllib.linalg.{SparseVector, Vectors}
import org.json4s.MappingException

import scala.collection.mutable

/**
  * Created by ifpelset on 3/23/17.
  */
class Model(
 val tfIdf: TFIDFModel,
 val namedEntitySet: Set[Int],
 val ap: AlgorithmParams
) extends Serializable
{
  def cluster(categoryModelMapArg: mutable.Map[String, SinglePass], newsArray: Array[News]): Unit = {
    newsArray.foreach(news => {
      cluster(categoryModelMapArg, news)
    })

    // 返回此时所有类别下的聚类的数目
//    PredictedResult(Some(categoryModelMap.map(pair => pair._2.getClusterCount).sum), None, None, None)
  }

  def cluster(categoryModelMapArg: mutable.Map[String, SinglePass], news: News): Unit = {
    val tfHasher = tfIdf.hasher
    val idf = tfIdf.idf
    val category = news.category.getOrElse("")
    val id = news.id
    val timestamp = news.timestamp
    val content = news.content

    val title = news.title.getOrElse(content)

    val realContent = if (title == content) content else title + content
    // attention: client must be set Content-type: charset=UTF-8
    val tfVector = tfHasher.hashTF(realContent)
    val tfidfVector: SparseVector = idf.transform(tfVector).asInstanceOf[SparseVector]

    val valuesArray = tfidfVector.values
    tfidfVector.indices.foreach(index => {
      if (namedEntitySet.contains(index)) {
        val valueIndex = tfidfVector.indices.indexOf(index)
        valuesArray(valueIndex) *= 4
      }
    })

    // weight tfidf vector
    val feature = Vectors.sparse(tfidfVector.size, tfidfVector.indices, valuesArray)

    if (!categoryModelMapArg.contains(category)) {
      categoryModelMapArg.put(category, SinglePass(ap.threshold, ap.maxClusterElementsCount))
    }

    val singlePassModel = categoryModelMapArg(category)
    singlePassModel.clustering(singlepass.News(id, timestamp, category, title, content, feature, tfVector))
  }
}