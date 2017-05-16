package com.dfsx.newsclustering

/**
  * Created by ifpelset on 4/17/17.
  */

case class ClusterMessage(model: CorpusModel, newsArray: Array[News]) extends Serializable
case class ModifyMessage(id: Long, category: String) extends Serializable

case class MQMessage(
  msgType: Int,
  clusterMessage: Option[ClusterMessage],
  modifyMessage: Option[ModifyMessage]
) extends Serializable