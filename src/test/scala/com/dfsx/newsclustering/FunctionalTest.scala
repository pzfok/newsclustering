package com.dfsx.newsclustering

import java.io.{BufferedReader, FileInputStream, InputStream, InputStreamReader}

import com.dfsx.newsclustering.common.Constants
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.{Before, Ignore, Test}
import org.junit.Assert.assertEquals
import org.scalatest.junit.JUnitSuite

/**
  * Created by ifpelset on 5/15/17.
  */
class FunctionalTest extends JUnitSuite {
  var _client: HttpClient = _

  def loadJsonFile(path: String): String = {
    val is: InputStream = new FileInputStream(path)
    val br: BufferedReader = new BufferedReader(new InputStreamReader(is))

    val json = br.readLine()
    br.close()
    is.close()

    json
  }

  def sendHttpPost(url: String, content: String): String = {
    val request: HttpPost = new HttpPost(url)
    val input = new StringEntity(content, "UTF-8")
    request.setHeader("Content-Type", "application/json;charset=UTF-8")
    request.setEntity(input)

    val response = _client.execute(request)
    val entity = response.getEntity

    EntityUtils.toString(entity)
  }

  @Ignore
  @Before def setUp(): Unit = {
    _client = HttpClients.createDefault()
  }

  @Ignore
  @Test def testCluster(): Unit = {
    val actualResult = sendHttpPost(
      Constants.REQUEST_URL,
      loadJsonFile(
        Constants.CLUSTER_INPUT_JSON_FILE_NAME
      )
    )

    val expectResult = loadJsonFile(Constants.CLUSTER_OUTPUT_JSON_FILE_NAME)

    assertEquals(expectResult, actualResult)
  }

  @Ignore
  @Test def testInfo(): Unit = {
    val actualResult = sendHttpPost(
      Constants.REQUEST_URL,
      loadJsonFile(
        Constants.INFO_INPUT_JSON_FILE_NAME
      )
    )

    val expectResult = loadJsonFile(Constants.INFO_OUTPUT_JSON_FILE_NAME)

    assertEquals(expectResult, actualResult)
  }

  @Ignore
  @Test def testModify(): Unit = {
    val actualResult = sendHttpPost(
      Constants.REQUEST_URL,
      loadJsonFile(
        Constants.MODIFY_INPUT_JSON_FILE_NAME
      )
    )

    val expectResult = loadJsonFile(Constants.MODIFY_OUTPUT_JSON_FILE_NAME)

    assertEquals(expectResult, actualResult)
  }

  // testModify之后的验证测试
  @Ignore
  @Test def testModifyInfo(): Unit = {
    val actualResult = sendHttpPost(
      Constants.REQUEST_URL,
      loadJsonFile(
        Constants.MODIFY_INFO_INPUT_JSON_FILE_NAME
      )
    )

    val expectResult = loadJsonFile(Constants.MODIFY_INFO_OUTPUT_JSON_FILE_NAME)

    assertEquals(expectResult, actualResult)
  }

  @Ignore
  @Test def testRecommendation(): Unit = {
    val actualResult = sendHttpPost(
      Constants.REQUEST_URL,
      loadJsonFile(
        Constants.RECOMMEND_INPUT_JSON_FILE_NAME
      )
    )

    val expectResult = loadJsonFile(Constants.RECOMMEND_OUTPUT_JSON_FILE_NAME)

    assertEquals(expectResult, actualResult)
  }
}
