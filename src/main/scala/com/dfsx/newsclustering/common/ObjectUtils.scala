package com.dfsx.newsclustering.common

import java.io._

/**
  * Created by ifpelset on 5/15/17.
  */
object ObjectUtils {
  private def writeObjectToByteArrayOutputStream(refObject: AnyRef): ByteArrayOutputStream = {
    //写入字节流
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(refObject)
    objectOutputStream.close()

    byteArrayOutputStream
  }

  private def readObjectFromByteArrayOutputStream(byteArrayOutputStream: ByteArrayOutputStream): AnyRef = {
    val byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray)
    val objectInputStream = new ObjectInputStream(byteArrayInputStream) {
      // scala存在的问题 需要重写resolveClass方法  不然报告ClassNotFoundException
      override def resolveClass(desc: ObjectStreamClass): Class[_] = {
        try { Class.forName(desc.getName, false, getClass.getClassLoader) }
        catch { case _: ClassNotFoundException => super.resolveClass(desc) }
      }
    }
    //返回生成的新对象
    val cloneObject = objectInputStream.readObject()
    objectInputStream.close()

    cloneObject
  }

  def cloneObject(refObject: AnyRef): AnyRef = {
    readObjectFromByteArrayOutputStream(
      writeObjectToByteArrayOutputStream(
        refObject
      )
    )
  }

  def writeObjectToFile(refObject: AnyRef, filename: String): Unit = {
    val file = new File(filename)
    if (!file.exists()) {
      throw new FileNotFoundException("写入的目标文件不存在")
    }

    val obs = new ObjectOutputStream(
      new FileOutputStream(
        file
      )
    )

    obs.writeObject(refObject)
    obs.close()
  }

  def readObjectFromFile(filename: String): AnyRef = {
    val file = new File(filename)
    if (!file.exists()) {
      throw new FileNotFoundException("写入的目标文件不存在")
    }

    if (file.length() == 0) {
      return None
    }

    val objectInputStream = new ObjectInputStream(new FileInputStream(file)) {
      // scala存在的问题 需要重写resolveClass方法  不然报告ClassNotFoundException
      override def resolveClass(desc: ObjectStreamClass): Class[_] = {
        try {
          Class.forName(desc.getName, false, getClass.getClassLoader)
        }
        catch {
          case _: ClassNotFoundException => super.resolveClass(desc)
        }
      }
    }

    val cloneObject = objectInputStream.readObject()
    objectInputStream.close()

    cloneObject
  }
}
