package com.dfsx.newsclustering.singlepass

/**
  * Created by ifpelset on 4/10/17.
  */
trait Event {
  def onCentroidChanged(cluster: Cluster): Unit
}
