package com.dfsx.newsclustering

import org.apache.http.client.HttpClient
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.{Before, Ignore, Test}
import org.scalatest.junit.JUnitSuite

/**
  * Created by ifpelset on 5/15/17.
  */
class HttpClientTest extends JUnitSuite {
  var _client: HttpClient = _

  @Before def setUp(): Unit = {
    _client = HttpClients.createDefault()
  }

  @Ignore
  @Test def testHttpGet(): Unit = {
    val request: HttpGet = new HttpGet("http://www.baidu.com")
    val response: HttpResponse = _client.execute(request)
    val entity: HttpEntity = response.getEntity

    val result = EntityUtils.toString(entity)
    println(result)
  }

  @Ignore
  @Test def testHttpPost(): Unit = {
    val request: HttpPost = new HttpPost("http://www.baidu.com")
    val input = new StringEntity("{\"test\":1}")
    input.setContentType("application/json;charset=UTF-8")
    request.setEntity(input)
    val response = _client.execute(request)
    val entity = response.getEntity

    val result = EntityUtils.toString(entity)
    println(result)
  }
}
