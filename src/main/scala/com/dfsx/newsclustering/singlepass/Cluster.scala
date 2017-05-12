package com.dfsx.newsclustering.singlepass

import org.apache.spark.mllib.linalg.{Vector, Vectors}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ifpelset on 3/9/17.
  */
class Cluster(val id: Int, val event: Event) extends Serializable {
  // clusterElementList中的所有元素都是不重复的
  private var clusterElementList: ListBuffer[Element] = ListBuffer()
  private var clusterCentroid: Vector = Vectors.zeros(1)
  private var clusterTFVector: Vector = Vectors.zeros(1)

  // 重复元素队列中的首元素->（重复的所有元素的队列）
//  private val clusterElementQueueMap: mutable.Map[Element, mutable.Queue[Element]] = mutable.HashMap()
  private val clusterElementQueueMap: mutable.Map[Element, mutable.ListBuffer[Element]] = mutable.HashMap()

  // getter elementList
  def elementList: ListBuffer[Element] = clusterElementList

  // getter centroid
  def centroid: Vector = clusterCentroid
  def tfVector: Vector = clusterTFVector
  def elementQueueMap: mutable.Map[Element, mutable.ListBuffer[Element]] = clusterElementQueueMap

  def addElement(element: Element) : Unit = {
    if (clusterElementList.isEmpty) {
      clusterCentroid = element.features
      clusterTFVector = element.tfVector

      // call onCentroidChanged
      event.onCentroidChanged(this)
    } else {
      val newElementSum = calculateElementSimilaritySum(element.features, element.tfVector)
      val oldCentroidSum = calculateElementSimilaritySum(clusterCentroid, clusterTFVector)

      if (newElementSum > oldCentroidSum) {
        clusterCentroid = element.features
        clusterTFVector = element.tfVector

        // call onCentroidChanged
        event.onCentroidChanged(this)
      }
    }
    clusterElementList.append(element)
  }

  def deleteElement(element: Element) : Unit = {
    require(clusterElementList.nonEmpty)

    // 存在再删除 像clearClusterElementQueueMap中是key才删除
    clusterElementList -= element
    // 进行重复元素数据结构有关的清理操作
    clearClusterElementQueueMap(element)

    // re-select centroid
    if (element.features == clusterCentroid && element.tfVector == clusterTFVector) {
      val newCentroidElement = if (clusterElementList.size < 2) element else  clusterElementList.reduce((e1, e2) => {
        val s1 = calculateElementSimilaritySum(e1.features, e1.tfVector)
        val s2 = calculateElementSimilaritySum(e2.features, e2.tfVector)

        if (s1 > s2) {
          e1
        } else {
          e2
        }
      })

      clusterCentroid = newCentroidElement.features
      clusterTFVector = newCentroidElement.tfVector

      // call onCentroidChanged
      event.onCentroidChanged(this)
    }
  }

  // 1.若element为clusterElementQueueMap的key
  //  取出该key对应的ElementQueue的第二个元素作为新的key，同时将该key加入elementList中（若ElementQueue size < 2就不用管了，没有重复的了）
  // 2.若element不为clusterElementQueueMap的key
  //  从ElementQueue中删除该元素（若ElementQueue size < 2删除该key对应的映射）
  def clearClusterElementQueueMap(element: Element) : Unit = {
    val findResult = clusterElementQueueMap.find(m => {
      if (m._1 == element || m._2.contains(element))
        true
      else
        false
    })

    // 要存在该元素重复才进行如下操作
    if (findResult.nonEmpty) {
      val findResultPair = findResult.get
      val queue = findResultPair._2

      // key是要删除的元素
      if (findResultPair._1 == element) {
        val firstElement = queue.remove(0)
        require(firstElement == element) // 队列的队首元素为map的key

        if (queue.nonEmpty) {
          val newKey = queue.head
          // 若队列中还有至少两个元素，则创建一个新的映射
          if (queue.size > 1) {
            clusterElementQueueMap.put(newKey, queue)
          }

          // 将新的重复的元素添加到聚类中的元素列表中
          clusterElementList.append(newKey)
        }

        clusterElementQueueMap.remove(element)
      } else { // 要删除的是队列中的元素
        queue -= element
        // 当重复队列元素少于2的时候，该映射将没必要保存
        if (queue.size < 2) {
          clusterElementQueueMap.remove(findResultPair._1)
        }
      }
    }
  }

  def isDuplicate(element: Element) : Boolean = {
    val vector = element.features

    for (item <- clusterElementList) {
      val cosineSimilarity = Similarity.calculateSimilarity(vector, item.features)

      if (cosineSimilarity >= 0.98) {
//        println(s"the same element, id: (${item.asInstanceOf[News].id}, ${element.asInstanceOf[News].id})")
        val destMapOpt = clusterElementQueueMap.get(item)
        if (destMapOpt.isEmpty) {
          clusterElementQueueMap.put(item, mutable.ListBuffer(item, element))
        } else {
          clusterElementQueueMap(item).append(element)
        }
        return true
      }
    }

    false
  }

  def calculateElementSimilaritySum(vector: Vector, tfVec: Vector) : Double = {
    var similaritySum = 0.0

    for (item <- elementList) {
      val cosineSimilarity = Similarity.calculateSimilarity(vector, item.features)
      val overlapWeight = Similarity.calculateOverlap(tfVec, item.tfVector)
      val similarity = cosineSimilarity + overlapWeight /// (1 + overlapWeight)

      similaritySum += similarity
    }

    similaritySum // / clusterElementList.length
  }

  override def hashCode(): Int = id

  def clear() : Unit = {
    clusterElementList.clear()
    clusterCentroid = Vectors.zeros(1)
    clusterTFVector = Vectors.zeros(1)
  }
}

object Cluster {
  def apply(id: Int, event: Event): Cluster = {
    new Cluster(id, event)
  }
}
