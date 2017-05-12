package com.dfsx.newsclustering.singlepass

import com.dfsx.newsclustering.ClusterInfo
import grizzled.slf4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ifpelset on 3/9/17.
  */
class SinglePass(threshold: Double, maxClusterElementsCount: Option[Int]) extends Serializable with Event {
  private var spClusterList: ListBuffer[Cluster] = ListBuffer()
  private val spThreshold = threshold
  private val spMaxClusterElementsCount = maxClusterElementsCount.getOrElse(1000)

//  private val spElementQueue: mutable.Queue[Element] = mutable.Queue()
// 因为队列不能进行删除指定元素，故使用ListBuffer模拟
  private val spElementQueue: mutable.Buffer[Element] = mutable.ListBuffer()
  private val spElementClusterMap: mutable.Map[Element, Cluster] = mutable.HashMap()

  // 聚类和元素，相似度集合的映射的缓存
  // 在Cluster中，当centroid改变的时候（改变或者添加第一个元素），回调SinglePass修改该映射的缓存重新计算所有其他聚类文章与该聚类中心的相似度
  // FIXME: 可以免掉该聚类中心属于的聚类中的文章与该聚类中心相似度的计算
  // 元素 -> ((聚类 -> 相似度),...)
  private val spElementClusterSimilarityMap: mutable.Map[Element, mutable.Map[Cluster, Double]] = mutable.HashMap()
  // 因为Cluster是可变类型，所以如果要将HashMap的key存为Cluster类型，那么Cluster的hashCode必须被重写
  private var spId = 0

  @transient lazy val logger = Logger[this.type]

  // getter
  def clusterList: ListBuffer[Cluster] = spClusterList
  def elementQueue: mutable.Buffer[Element] = spElementQueue
  def elementClusterMap: mutable.Map[Element, Cluster] = spElementClusterMap

  def clustering(element: Element): Unit = {
    if (spClusterList.isEmpty) {
      val newCluster = Cluster(spId, this)
      spClusterList.append(newCluster)
      spClusterList.head.addElement(element)

      spElementClusterMap.put(element, newCluster)
    } else {
      // 对元素个数的限制
      if (spElementQueue.size >= spMaxClusterElementsCount) {
        // 对时间的限制
        // 遍历spElementQueue，检查每个元素的时间，当前时间-时间>一周，删除该元素
        val removeElements = spElementQueue.filter(e => {
          val news = e.asInstanceOf[News]
          if ((System.currentTimeMillis() / 1000) - news.timestamp > 604800) {
            true
          } else {
            false
          }
        })

        removeElements.foreach(e => {
          remove(e)
        })
        //        val earlyElement = spElementQueue.remove(0)
        //        remove(earlyElement)
      }

      val features = element.features
      val tfVector = element.tfVector

      val firstCluster = spClusterList.head

      var maxCosineSimilarity = Similarity.calculateSimilarity(features, firstCluster.centroid)
      var maxOverlapWeight = Similarity.calculateOverlap(tfVector, firstCluster.tfVector)
      var maxSimilarity = maxCosineSimilarity + maxOverlapWeight// / (1 + maxOverlapWeight)//maxCosineSimilarity + maxOverlapWeight
      var maxSimilarityIndex = 0

      // 新来的element与老的所有的聚类的聚类中心求相似度
      updateElementClusterSimilarity(element, firstCluster, maxSimilarity)

      for ((cluster, clusterIndex) <- spClusterList.slice(1, spClusterList.length).zipWithIndex) {
        val cosineSimilarity = Similarity.calculateSimilarity(features, cluster.centroid)
        val overlapWeight = Similarity.calculateOverlap(tfVector, cluster.tfVector)
        val similarity =  cosineSimilarity + overlapWeight// / (1 + overlapWeight)//cosineSimilarity + overlapWeight

        // 新来的element与老的所有的聚类的聚类中心求相似度
        updateElementClusterSimilarity(element, cluster, similarity)

        if (similarity > maxSimilarity) {
          maxSimilarity = similarity
          maxSimilarityIndex = clusterIndex + 1

          maxCosineSimilarity = cosineSimilarity
          maxOverlapWeight = overlapWeight
        }
      }

      // for debug
//      println(s"sim: ${maxSimilarity}, cos: ${maxCosineSimilarity}, overlap: ${maxOverlapWeight}, title: ${element.asInstanceOf[News].title}")
      if (maxSimilarity > spThreshold) {
        // 注意这里是将新文档添加到老的聚类
        // 存在新文档和老聚类中文档重复的情况
        // 只有本聚类不是新的聚类的时候才会有这种情况考虑
        // 若 重复 直接跳过添加
        val destCluster = spClusterList(maxSimilarityIndex)
        if (destCluster.isDuplicate(element)) {
          // 重复也添加元素到spElementQueue和spElementClusterMap中，但是不添加该元素到目标聚类中去
          // 原因：1. 重复的元素也应当纳入元素个数统计范围
          //     2. 在获取相似推荐的时候，要确保spElementClusterMap中有该文章的聚类的信息
          spElementQueue += element
          spElementClusterMap.put(element, destCluster)
          return
        }

        destCluster.addElement(element)
        spElementClusterMap.put(element, destCluster)
      } else {
        val newCluster = Cluster(spId, this)
        newCluster.addElement(element)
        spClusterList.append(newCluster)
        spElementClusterMap.put(element, newCluster)
      }
    }

    // 更新用来计算Cluster hashCode的id值
    spId += 1
    // 修正为最后再添加元素到元素队列中
    // ???队列是不是应该优先保留所有的元素？？
    // Cluster中的clusterList才应该只保存不重复的元素？？
    spElementQueue += element
  }

  // 从该模型中删除所有数据结构中的指定元素
  def remove(element: Element): Unit = {
//    println(s"remove ${element.asInstanceOf[News].title}")
    if (spElementQueue.contains(element)) {
      spElementQueue -= element
    }

    val destCluster = spElementClusterMap(element)
    destCluster.deleteElement(element)
    spElementClusterMap.remove(element)
    if (destCluster.elementList.isEmpty) {
      spClusterList -= destCluster

      // 1.先清理我们新加的spElementClusterSimilarityMap中的某个cluster对应的相似度映射（清理cluster）
      //   应该为删除所有元素与destCluster的相似度的映射
      spElementClusterSimilarityMap.foreach(m => {
        m._2.remove(destCluster)
      })
    }

    // 2.再清理我们新加的spElementClusterSimilarityMap中的某个element与其他所有cluster相似度的映射（清理element）
    spElementClusterSimilarityMap.remove(element)
  }

  def updateElementClusterSimilarity(element: Element, cluster: Cluster, similarity: Double): Unit = {
    val clusterSimilarityMap = spElementClusterSimilarityMap.get(element)
    if (clusterSimilarityMap.isEmpty) {
      spElementClusterSimilarityMap.put(element, mutable.HashMap(cluster -> similarity))
    } else {
      spElementClusterSimilarityMap(element)(cluster) = similarity
    }
  }

  // by Cluster called
  override def onCentroidChanged(cluster: Cluster): Unit = {
    spElementQueue.foreach(element => {
      val similarity = Similarity.calculateSimilarity(element.features, cluster.centroid)
      val overlap = Similarity.calculateOverlap(element.tfVector, cluster.tfVector)
      val result = similarity + overlap

      updateElementClusterSimilarity(element, cluster, result)
    })
  }

  def getClusterCount: Int = spClusterList.size

  def getSummaries(minItemsCount: Int, maxClustersCount: Int) : ListBuffer[ClusterInfo] = {
    val summaryBuffer = ListBuffer[ClusterInfo]()
    val sortedClusterList = spClusterList.sortWith((c1, c2) => c1.elementList.size > c2.elementList.size)
    val sortedFilteredElementList = sortedClusterList.filter(c => c.elementList.size >= minItemsCount)

    for ((cluster, clusterIndex) <- sortedFilteredElementList.zipWithIndex if clusterIndex < maxClustersCount) {
      val elementList = cluster.elementList

      var titleComb = elementList.map(e => {
        e.asInstanceOf[News].title
      }).mkString("。")

      val contentComb = elementList.map(e => {
        e.asInstanceOf[News].content
      }).mkString("。")

      val realTitleArray = titleComb.split("[，,。:：？?！!；;丨|——~　\\s]").filter(s => s.length() > 5)
      titleComb = realTitleArray.mkString("。")

      val titleCount = realTitleArray.size

      val titleContentComb = Array(titleComb, contentComb).mkString("。")

      val summary = JavaUtils.getSummary(titleContentComb, titleCount)
      summaryBuffer.append(ClusterInfo(summary, elementList.map(_.asInstanceOf[News].id)))
    }

    summaryBuffer
  }

  def getTopNSimilarities(findId: Long, topN: Int): ListBuffer[Long] = {
    var idListBuffer =  ListBuffer[Long]()

    //首先查看要查的id新闻所属的聚类的新闻数量够不够topN
    // spElementQueue 保存了重复与不重复的所有元素
    val findElementResult = spElementQueue.find(e => {
      e.asInstanceOf[News].id == findId
    })

    if (findElementResult.isEmpty) {
      logger.warn(s"findId: ${findId} is not exists in spElementQueue")
      return idListBuffer
    }

    val findElement = findElementResult.get

    val findClusterResult = spElementClusterMap.get(findElement)

    if (findClusterResult.isEmpty) {
      logger.warn(s"findId: ${findId}, findElement is not exists in spElementClusterMap")
      return idListBuffer
    }

//    println(s"spElementClusterMap.size: ${spElementClusterMap.size}, spElementQueue.size: ${spElementQueue.size}")

    val findCluster = findClusterResult.get//spElementClusterMap(findElement)
    val elementList = findCluster.elementList
    var tempListBuffer: mutable.ListBuffer[Long] = null

    // 1. 判断要找的推荐的元素是否在elementList里，是的话只需要过滤掉该元素 则diffElementCount += 1
    // 2. 若不在elementList里，则判断是否在该cluster的重复元素队列里。若在队列里，则找到该map的值为该队列的key，只需要从elementList过滤掉该key，并且过滤掉自身，则diffElementCount += 2
    // 3. 若不在队列里，则说明他不属于重复元素，也只需要简单的过滤掉该元素即可 diffElementCount += 1
    // elementList包含findElement表示：1. 该元素存在重复的且该元素为map的key; 2. 该元素不存在重复。
    if (elementList.contains(findElement)) {
      tempListBuffer = elementList.filter(_ != findElement).map(_.asInstanceOf[News].id)
    } else {
      val findDuplicateResult = findCluster.elementQueueMap.find(m => {
        m._2.contains(findElement)
      })

      // findElement在重复队列里
      if (findDuplicateResult.nonEmpty) {
        val filterKey = findDuplicateResult.get._1
        // 过滤掉重复的以及过滤掉自己
        tempListBuffer = elementList.filter(_ != findElement).filter(_ != filterKey).map(_.asInstanceOf[News].id)
      }
    }

    val diffElementCount = topN - tempListBuffer.size

    if (diffElementCount <= 0) {
      idListBuffer = tempListBuffer.take(topN)
    } else {
      // 不够再查询spElementClusterSimilarityMap得到不那么相似的聚类中心依次凑够
      val clusterSimilarityMapResult = spElementClusterSimilarityMap.get(findElement)
      if (clusterSimilarityMapResult.isEmpty) {
        logger.warn(s"findId: ${findId}, findElement is not exists in clusterSimilarityMap")
        return idListBuffer
      }
      val clusterSimilarityMap = clusterSimilarityMapResult.get//spElementClusterSimilarityMap(findElement)

      // 除掉一个跟自己聚类的相似度计算的映射
      val sortedList = clusterSimilarityMap.filter(m => m._1 != findCluster).toList.sortWith((p1, p2) => p1._2 > p2._2)

      idListBuffer = tempListBuffer
      // 考虑极端的情况下 每个聚类只有一个元素的时候
      idListBuffer ++= sortedList.take(diffElementCount).flatMap(m => m._1.elementList).take(diffElementCount).map(_.asInstanceOf[News].id)
    }

    idListBuffer
  }

  // for debug
  def showClusterInfo(): Unit = {
    for ((cluster, clusterIndex) <- spClusterList.zipWithIndex) {
      println("--------------------------------------------------------------------------")
      println(s"cluster ${clusterIndex + 1}, element count: ${cluster.elementList.length}")
      for (element <- cluster.elementList) {
        val news = element.asInstanceOf[News]
        println(s"[${news.id} ${news.category}] ${news.title}")
      }
    }
  }

  // for debug
  def showTopTopics(n: Int): Unit = {
    // {title: [title1, title2,..., content1, content2, ...]}
    val sortedClusterList = spClusterList.sortWith((c1, c2) => c1.elementList.size > c2.elementList.size)

    println(s"--------------------------------Top ${n}----------------------------------------")
    for ((cluster, clusterIndex) <- sortedClusterList.zipWithIndex if clusterIndex < n) {
      val elementList = cluster.elementList

      var titleComb = elementList.map(e => {
        e.asInstanceOf[News].title
      }).mkString("。")

      val contentComb = elementList.map(e => {
        e.asInstanceOf[News].content
      }).mkString("。")

      val realTitleArray = titleComb.split("[，,。:：？?！!；;丨|——~　\\s]").filter(s => s.length() > 5)
      titleComb = realTitleArray.mkString("。")

      val titleCount = realTitleArray.size

      val titleContentComb = Array(titleComb, contentComb).mkString("。")

      val summary = JavaUtils.getSummary(titleContentComb, titleCount)

      println(s"${clusterIndex + 1}. ${summary} titleCount: ${titleCount}")
    }
  }

  def clear() : Unit ={
    spClusterList.foreach(cluster => {
      cluster.clear()
    })

    spClusterList.clear()
  }
}

object SinglePass {
  def apply(threshold: Double, maxClusterElementsCount: Option[Int]): SinglePass = {
    new SinglePass(threshold, maxClusterElementsCount)
  }
}
