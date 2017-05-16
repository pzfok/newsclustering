import AssemblyKeys._

assemblySettings

name := "newsclustering"

organization := "com.dfsx.newsclustering"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.10.0-incubating" % "provided",
  "org.apache.spark"        %% "spark-core"               % "1.3.0" % "provided",
  "org.apache.spark"        %% "spark-mllib"              % "1.3.0" % "provided",
//  "com.hankcs"              % "hanlp"                     % "portable-1.3.2",
  "org.apache.activemq"     %  "activemq-client"           % "5.14.4",
  ("com.github.karlhigley"  % "spark-neighbors_2.10"      % "0.2.2").exclude("com.chuusai", "shapeless_2.10.4"),
  "org.scalatest" % "scalatest_2.10" % "3.0.3" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.apache.httpcomponents" % "httpclient" % "4.5.3" % "test"
)