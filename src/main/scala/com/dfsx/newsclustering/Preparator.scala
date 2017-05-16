package com.dfsx.newsclustering

import com.dfsx.newsclustering.singlepass.JavaUtils
import org.apache.predictionio.controller.PPreparator
//import org.apache.predictionio.data.storage.Event
import org.apache.spark.SparkContext
//import org.apache.spark.SparkContext._
import org.apache.spark.mllib.feature.{HashingTF, IDF, IDFModel}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._

class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val tfHasher = new TFHasher()
    val wordNaturePairBufferRDD = trainingData.data.map(observer => {
      JavaUtils.segmentAndNature(observer.title + observer.content).asScala.map(term =>{
        (term.word, term.nature.name())
      })
    })

    val wordBufferRDD = wordNaturePairBufferRDD.map(buffer => {
      buffer.map(_._1)
    })

    val idf: IDFModel = new IDF().fit(tfHasher.hashTF(wordBufferRDD))

    val namedEntitySet = wordNaturePairBufferRDD
      .flatMap(buffer => buffer)
      .filter(pair => pair._2.contains("ns") || pair._2.contains("nr") || pair._2.contains("nt") || pair._2.contains("nz") || pair._2.contains("nf"))
      .map(pair => tfHasher.indexOf(pair._1)).collect().toSet

    val tfIdfModel = new TFIDFModel(
      hasher = tfHasher,
      idf = idf
    )

    new PreparedData(tfIdfModel, namedEntitySet)
  }
}

class TFHasher extends Serializable {
  private val hasher = new HashingTF()

  def hashTF[D <: Iterable[_]](dataset: RDD[D]): RDD[Vector] = {
    hasher.transform(dataset)
  }

  def hashTF(content: String): Vector = {
    val words = JavaUtils.segment(content).asScala
    hasher.transform(words)
  }

  def indexOf(term: Any): Int = {
    hasher.indexOf(term)
  }
}

class TFIDFModel(
  val hasher: TFHasher,
  val idf: IDFModel
) extends Serializable {

  def transform(text: String): Vector = {
    // Map(n-gram -> document tf)
    idf.transform(hasher.hashTF(text))
  }

  def transform(doc: RDD[(Double, String)]): RDD[LabeledPoint] = {
    doc.map{ case (label, text) => LabeledPoint(label, transform(text)) }
  }
}

class PreparedData(
  val tfIdfModel: TFIDFModel,
  val namedEntitySet: Set[Int]
) extends Serializable
