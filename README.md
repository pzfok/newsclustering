# news clustering 

## 介绍

> 基于修改版本的[PredictionIO](https://github.com/apache/incubator-predictionio)之上的热点新闻聚类推荐系统。

## 特点
1. *支持新闻聚类;*
2. *支持新闻聚类结果筛选查询;*
3. *支持修改指定新闻的类别;*
4. *支持对指定新闻做相关新闻推荐。*

## 依赖

1. *Scala-2.10.6*
2. *PredictionIO-0.10.0-incubating*
3. *Apache-Spark-1.6.3*
4. *hanlp-1.3.2.jar*

## 用法

1. 下载已修改的[PredictionIO]二进制版本并设置相关环境变量

   ```bash
   # 1. 解压PredictionIO-0.10.0-incubating.deploy.tar.gz
   [ifpelset@archlinux ~]$ tar zxvf PredictionIO-0.10.0-incubating.deploy.tar.gz
   # 2. 配置系统环境变量
   [ifpelset@archlinux ~]$ sudo vim /etc/profile # 添加如下内容
   	export JAVA_HOME=/usr/lib/jvm/java-8-jdk # 此项按照实际情况为准
   	export PIO_HOME=[您解压PIO的位置]/PredictionIO-0.10.0-incubating
   	export SPARK_HOME=$PIO_HOME/vendors/spark-1.6.3-bin-hadoop2.6
   	export PATH=$PATH:$JAVA_HOME/bin:$PIO_HOME/bin:$SPARK_HOME/bin:$SPARK_HOME/sbin
   # 3. 重启一次让系统环境变量生效（貌似通过source /etc/profile达不到效果，建议重启一次）
   [ifpelset@archlinux ~]$ reboot
   ```



2. 启动PredictionIO

   ```bash
   [ifpelset@archlinux ~]$ pio-start-all
   ```


3. 创建新应用

   ```bash
   [ifpelset@archlinux ~]$ pio app new <您的App名字>
   ```


4. 克隆新闻分类源码到本地
   ```bash
   [ifpelset@archlinux ~]$ git clone https://github.com/ifpelset/newsclustering.git
   ```

5. 导入事先准备的好的“中新网”十个分类的json文件到HBase中

   ```bash
   # 1. 切换目录到“数据”目录下
   [ifpelset@archlinux ~]$ cd newsclustering/data
   # 2. 导入中新网数据到HBase中
   [ifpelset@archlinux data]$ pio import --appid <您的AppId> --input ./中新网指定分类的新闻.json
   ```

6. ​ 构建源码

   ```bash
   # 1. 切换目录到源码根目录下
   [ifpelset@archlinux ~]$ cd newsclustering
   # 2. 执行构建
   [ifpelset@archlinux newsclustering]$ pio build --verbose
   ```

7.  下载分词数据包并放置到相应目录

   ```bash
   # 1. 创建/var/local/data目录并给予当前用户读写执行权限
   [ifpelset@archlinux ~]$ sudo mkdir /var/local/data
   [ifpelset@archlinux ~]$ sudo chmod o+rwx /var/local/data
   # 2. 解压到/var/local/data下
   [ifpelset@archlinux ~]$ tar zxvf HanLPData.tar.gz -C /var/local/data
   ```

8.  训练分类模型

   ```bash
   [ifpelset@archlinux newsclassification]$ pio train -- --driver-memory 2G --executor-memory 4G
   ```

9.  部署分类服务

   ```bash
   [ifpelset@archlinux newsclassification]$ pio deploy --port 8002 -- --driver-memory 3G --executor-memory 10M
   ```

10.  测试分类
  通过任意工具，设置Content-Type值为applicaiton/json;charset=UTF-8，然后向http://localhost:8002/queries.json发送Post请求即可。请参考newsclassification/data目录下的test json文件。

  ​