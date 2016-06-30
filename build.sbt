import sbt.Keys._

val org = "com.sksamuel.elastic4s"

val ScalaVersion = "2.11.7"
val JacksonVersion = "2.6.1"
val ElasticsearchVersion = "2.3.0"
val Slf4jVersion = "1.7.12"

lazy val commonSettings = Seq(
  organization := "SpronQ",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.11.7", "2.10.5")
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "ConnectMQTT",
      libraryDependencies ++= Seq(
      "org.eclipse.paho" % "mqtt-client" % "0.4.0",
      "org.json4s" %% "json4s-jackson" % "3.4.0",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
      "com.sksamuel.elastic4s"  %% "elastic4s-core" % ElasticsearchVersion,
      "com.sksamuel.elastic4s"  %% "elastic4s-jackson" % ElasticsearchVersion,
        "org.slf4j" % "slf4j-api" % Slf4jVersion,
        "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
        "ch.qos.logback" % "logback-classic" % "1.1.2",
        "com.typesafe.akka" % "akka-actor_2.11" % "2.4.7",
        "com.typesafe" % "config" % "1.3.0"

      )
  )


resolvers += "MQTT Repository" at "https://repo.eclipse.org/content/repositories/paho-releases/"

