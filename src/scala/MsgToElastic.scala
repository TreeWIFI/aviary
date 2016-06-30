package Birdcage

/**
  * Created by Gabor on 28/06/16.
  */
import java.util.Date
import com.sksamuel.elastic4s.ElasticDsl._
import akka.actor.Actor
import com.sksamuel.elastic4s.{ElasticClient, IndexAndType, IndexAndTypes, IndexResult}
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.paho.client.mqttv3.MqttMessage
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.Future
import scala.util.{Failure, Success}

object MsgToElastic {
  case class MsgMqtt(topic: String, message: MqttMessage, esClient: ElasticClient, indexTypeEs: Tuple2[String, String])
}

class MsgToElastic extends Actor with LazyLogging {

  import MsgToElastic._

  def receive = {
    case MsgMqtt(topic, message, esClient, indexTypeEs) => sentToElastic(topic, message, esClient, indexTypeEs)
    case _ => logger.debug("MsgActor received unsupported message")
  }

  def sentToElastic(topic: String, message: MqttMessage, esClient: ElasticClient, indexTypeEs: Tuple2[String, String]) = {

    val indexType = new IndexAndType(indexTypeEs._1.toLowerCase, indexTypeEs._2.toLowerCase())
    val messageVal = s"%s".format(message)
    val timestamp = new Date().toString

    val resp: Future[IndexResult] = {
      esClient.execute {
        index into IndexAndTypes.apply(indexType)  fields(
          "postcode"   -> topic.takeWhile(_ != '/').toLowerCase(),
          "huisnummer" -> topic.dropWhile(_ != '/').drop(1).takeWhile(_ != '/'),
          "timestamp"  -> timestamp,
          "subject"    -> topic.reverse.takeWhile(_ != '/').reverse,
          "value"      -> messageVal
          )
      }
    }
    resp.onComplete
    {
      case Success(res) => {
        val bufferString = "Msg to Es : %s, %s, %s\n %s\n".format(timestamp, topic, messageVal, resp)
        logger.info(bufferString)
      }
      case Failure(e) => logger.info("An error has occured while sending to Elastic")
    }
  }
}
