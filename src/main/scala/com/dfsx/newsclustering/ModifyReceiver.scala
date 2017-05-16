package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.SinglePass
import grizzled.slf4j.Logger

import scala.collection.mutable

/**
  * Created by ifpelset on 5/16/17.
  */
class ModifyReceiver(val _algorithmParams: AlgorithmParams) {
  @transient private lazy val _logger = Logger[this.type]

  private def findNewsById(model: SinglePass, id: Long): Option[singlepass.News] = {
    val findResult = model.elementQueue.find(e => e.asInstanceOf[singlepass.News].id == id)

    if (findResult.nonEmpty) {
      Some(findResult.get.asInstanceOf[singlepass.News])
    } else {
      None
    }
  }

  private def putCategoryToModelMapIfNotExist(category: String, categoryToModelMap: mutable.Map[String, SinglePass]): Unit = {
    if (!categoryToModelMap.contains(category)) {
      _logger.debug(s"create $category's SinglePass Model")
      categoryToModelMap.put(category, SinglePass(_algorithmParams.threshold, _algorithmParams.maxClusterElementsCount))
    }
  }

  private def removeNewsFromOldModel(news: singlepass.News, model: SinglePass): Unit = {
    model.remove(news)
  }

  private def clusterNewsFromNewModel(news: singlepass.News, model: SinglePass): Unit = {
    model.clustering(news)
  }

  private def modifyNewsCategory(news: singlepass.News, category: String): Unit = {
    news.category = category
  }

  private def findCategoryToModelOptionPair(categoryToModelMap: mutable.Map[String, SinglePass], id: Long, category: String): Option[(String, SinglePass)] = {
    // 使用find找到一个就返回，不用考虑scala的不支持“正常的”break
    categoryToModelMap.find(pair => {
      val findResult = findNewsById(pair._2, id)

      if (findResult.nonEmpty) {
        putCategoryToModelMapIfNotExist(category, categoryToModelMap)

        val news = findResult.get

        // 先删除老的模型中的数据，再重新聚类，防止改变分类为自己原本的分类的问题
        removeNewsFromOldModel(news, pair._2)

        // 修改之后要对该新闻重新聚类
        clusterNewsFromNewModel(news, categoryToModelMap(category))

        modifyNewsCategory(news, category)
        true
      } else {
        false
      }
    })
  }

  private def clearModelMapIfModelClusterCountIsZero(categoryToModelMap: mutable.Map[String, SinglePass], optionPair: Option[(String, SinglePass)]) {
    if (optionPair.nonEmpty) {
      val pair = optionPair.get
      val category = pair._1
      val model = pair._2
      // 若model中不存在任何一个聚类了，清理map中该(category,model)项
      if (model.getClusterCount < 1) {
        _logger.debug(s"remove $category from singlePassModelMap")
        categoryToModelMap.remove(category)
      }
    }
  }

  def modifyNewsCategoryFromCategoryToModelMap(categoryToModelMap: mutable.Map[String, SinglePass], id: Long, category: String): Unit = {
    clearModelMapIfModelClusterCountIsZero(
      categoryToModelMap,
      findCategoryToModelOptionPair(categoryToModelMap, id, category)
    )
  }
}