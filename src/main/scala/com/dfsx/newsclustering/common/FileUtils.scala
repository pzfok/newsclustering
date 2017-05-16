package com.dfsx.newsclustering.common

import java.io.File

/**
  * Created by ifpelset on 5/15/17.
  */
object FileUtils {
  def createDirIfNotExist(dirName: String): Unit = {
    val dir = new File(dirName)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  def createFileIfNotExist(filename: String): Unit = {
    val file = new File(filename)
    if (!file.exists()) {
      file.createNewFile()
    }
  }
}
