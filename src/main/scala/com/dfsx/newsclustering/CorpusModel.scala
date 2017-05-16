package com.dfsx.newsclustering

/**
  * Created by ifpelset on 3/23/17.
  */
class CorpusModel(
 private val tfIdfModel: TFIDFModel,
 private val namedEntitySet: Set[Int]
) extends Serializable {
  def tfIdf: TFIDFModel = tfIdfModel

  def namedEntity: Set[Int] = namedEntitySet
}