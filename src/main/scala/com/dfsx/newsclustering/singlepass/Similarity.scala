package com.dfsx.newsclustering.singlepass

import org.apache.spark.mllib.linalg.{LinalgShim, SparseVector, Vector, Vectors}

/**
  * Created by ifpelset on 3/9/17.
  */
object Similarity {
  def calculateOverlap(vector1: Vector, vector2: Vector) : Double = {
    val sparseVector1 = vector1.asInstanceOf[SparseVector]
    val sparseVector2 = vector2.asInstanceOf[SparseVector]

    val intersectIndicesVector = sparseVector1.indices.intersect(sparseVector2.indices)
    val intersectTFSum = intersectIndicesVector.map(index => {
      math.min(sparseVector1(index), sparseVector2(index))
    }).sum

    val minTFSum = math.min(sparseVector1.values.sum, sparseVector2.values.sum)

    intersectTFSum / minTFSum
  }

  def calculateSimilarity(vector1: Vector, vector2: Vector) : Double = {
    require(vector1.size == vector2.size, "vector size is not equal")

    val dotProduct = LinalgShim.dot(vector1, vector2)
    val norms = Vectors.norm(vector1, 2) * Vectors.norm(vector2, 2)
    math.abs(dotProduct) / norms
  }
}
