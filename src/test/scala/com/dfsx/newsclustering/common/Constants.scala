package com.dfsx.newsclustering.common

/**
  * Created by ifpelset on 5/15/17.
  */
object Constants {
  val REQUEST_URL = "http://localhost:8002/queries.json"

  val PROJECT_ROOT_DIR_NAME = "/home/ifpelset/root/dev/project/scala/newsclustering/"

  val CLUSTER_INPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_cluster_input.json"
  val CLUSTER_OUTPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_cluster_output.json"

  val INFO_INPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_info_input.json"
  val INFO_OUTPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_info_output.json"

  val MODIFY_INPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_modify_input.json"
  val MODIFY_OUTPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_modify_output.json"
  val MODIFY_INFO_INPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_modify_info_input.json"
  val MODIFY_INFO_OUTPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_modify_info_output.json"

  val RECOMMEND_INPUT_JSON_FILE_NAME: String = PROJECT_ROOT_DIR_NAME + "data/test_recommendation_input.json"
  val RECOMMEND_OUTPUT_JSON_FILE_NAME: String =  PROJECT_ROOT_DIR_NAME + "data/test_recommendation_output.json"
}
