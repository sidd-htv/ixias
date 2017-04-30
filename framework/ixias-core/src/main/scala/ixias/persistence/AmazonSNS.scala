/*
 * This file is part of the IxiaS services.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package ixias.persistence

import scala.util.{ Try, Success, Failure }
import scala.concurrent.{ Future, ExecutionContext }

import ixias.persistence.model.DataSourceName
import ixias.persistence.lifted.Aliases
import ixias.persistence.backend.AmazonSNSBackend
import ixias.persistence.dbio.Execution
import ixias.util.Logging

// Amazon SNS
//~~~~~~~~~~~~
trait AmazonSNS extends Aliases with Logging {

  // --[ Typedefs ]-------------------------------------------------------------
  type PublishResult = com.amazonaws.services.sns.model.PublishResult

  // --[ Alias ]----------------------------------------------------------------
  val DataSourceName = ixias.persistence.model.DataSourceName

  // --[ Properties ]-----------------------------------------------------------
  /** The data source name */
  implicit val dsn: DataSourceName

  /** The backend */
  protected lazy val backend = AmazonSNSBackend()

  /** The Execution Context */
  protected implicit val ctx: ExecutionContext = Execution.Implicits.trampoline

  // --[ Methods ]--------------------------------------------------------------
  /**
   * Sends a message to a topic's subscribed endpoints.
   */
  def publish(message: String): Future[Seq[PublishResult]] =
    backend.isSkip(dsn) match {
      case true  => {
        backend.getTopicARN(dsn) map { topic =>
          logger.info("AWS-SNS :: skip to publish a message. topic = %s, message = %s".format(topic, message))
        }
        Future.successful(Seq.empty)
      }
      case false => for {
        client    <- backend.getDatabase(dsn)
        topicSeq  <- Future.fromTry(backend.getTopicARN(dsn))
        resultSeq <- Future.sequence {
          topicSeq map { topic =>
            Future.fromTry {
              Try(client.publish(topic, message))
            } andThen {
              case Success(result) => logger.info(
                "AWS-SNS :: publish a message. topic = %s, message = %s, result = %s"
                  .format(topic, message, result.toString()))
              case Failure(ex)     => logger.error(
                "AWS-SNS :: failed to publish a message. topic = %s, message = %s"
                  .format(topic, message), ex)
            }
          }
        }
      } yield resultSeq
    }
}
