package Birdcage

import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import com.typesafe.scalalogging._
import akka.actor.{ActorSystem, PoisonPill, Props}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.ConfigFactory

/**
  * Created by Gabor on 23/06/16.
  */
object ConnectMQTT extends LazyLogging {

  val conf = ConfigFactory.load()

  def main(args: Array[String]) {

    import MsgToElastic._

    logger.info("Program start")

    // Fill values for brokerUrl and Topic either from execution arguments of Application.conf file
    val brokerUrl: String = getBroker(args)
    val topic: String = getTopic(args)

    // get ElasticSearch index and Type from application.conf
    val indexTypeEs = new Tuple2[String, String](getIndex, getType)
    if (!indexTypeEs.toString().filter(_.isUpper).isEmpty)
      logger.info("Index/Type of ElasticSearch cannot contain Uppercase Characters!")

    // Create Actor
    val system = ActorSystem("MsgSystem")
    val msgToElastic = system.actorOf(Props[MsgToElastic], name = "msgToElastic")

    // Connect to ElasticSearch with ElasticSearchClientUri fro Application.conf
    val uri = ElasticsearchClientUri(conf.getString("Elastic.ElasticSearchClientUri"))
    logger.debug("connect to ElasticSearch: " + uri)
    val esClient = ElasticClient.transport(uri)

    //Set up persistence for messages
    val persistence = new MemoryPersistence

    //Initializing Mqtt Client specifying brokerUrl, clientID and MqttClientPersistance
    val client = new MqttClient(brokerUrl, MqttClient.generateClientId, persistence)

    //Connect to MqttBroker
    logger.info("Connecting to... "+ brokerUrl)
    client.connect()

    //Subscribe to Mqtt topic
    client.subscribe(topic)

    //Callback automatically triggers as and when new message arrives on specified topic
    val callback = new MqttCallback {

      override def messageArrived(topic: String, message: MqttMessage): Unit = {
        logger.debug("Message arrived" + topic + message)

        msgToElastic ! MsgMqtt(topic, message, esClient, indexTypeEs)

      }

      override def connectionLost(cause: Throwable): Unit = {
        logger.info(cause.toString())
      }

      override def deliveryComplete(token: IMqttDeliveryToken): Unit = {

      }
    }

    //Set up callback for MqttClient
    logger.debug("Set Callback, wait for messages")
    client.setCallback(callback)

    sys.addShutdownHook {
      client.disconnect()
      logger.info("Program stopping..")
      msgToElastic ! PoisonPill
      system.terminate()
    }
  }

  def isBroker(argument: String): Boolean = {
    if (argument.take(6) == "tcp://" || argument.take(6) == "ssl://" && argument.contains(".")) true
    else false
  }

  def getTopic(args: Array[String]): String = {

    val topic = {
      for {
        argument <- args
        if !isBroker(argument)
      } yield argument
    }.mkString("")
    if (topic == "") {
      if (conf.hasPath("ConnectMQTT.topic"))
        return conf.getString("ConnectMQTT.topic")
      else
        logger.info("No topic specified in config file or as argument. Topic set to #")
        return "#"
    }
    else topic
  }

  def getBroker(args: Array[String]): String = {

    val brokerUrl: String = {
      for {
        argument <- args
        if isBroker(argument)
      } yield argument
    }.mkString("")

    if (brokerUrl == "") {
      val confBrokerUrl = conf.getString("ConnectMQTT.brokerUrl")
      if (confBrokerUrl == "" || !isBroker(confBrokerUrl)) {
        logger.info("brokerUrl is missing or invalid either specify as argument or in the config file")
        return "invalid BrokerUrl"
      }
      else confBrokerUrl
    }
    else brokerUrl
  }

  def getIndex: String = conf.getString("Elastic.Index")
  def getType: String = conf.getString("Elastic.Type")
}

