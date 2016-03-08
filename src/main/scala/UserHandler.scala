import java.time.ZonedDateTime

import scala.runtime.ScalaRunTime

import Publication._
import akka.actor._
import akka.persistence._

object UserHandler {
  def props(id: Long) = Props(new UserHandler(id))

  sealed trait Command
  case object Get extends Command
  case class Subscribe(time: ZonedDateTime, publication: Publication) extends Command
  case class Unsubscribe(time: ZonedDateTime, publication: Publication) extends Command

  sealed trait Event
  sealed trait SubscriptionEvent extends Event {
    def time: ZonedDateTime
    def publication: Publication
  }
  case class Subscribed(time: ZonedDateTime, publication: Publication) extends SubscriptionEvent
  case class Unsubscribed(time: ZonedDateTime, publication: Publication) extends SubscriptionEvent
}

class UserHandler(userId: Long) extends PersistentActor with ActorLogging {
  import UserHandler._

  override val persistenceId = s"user-roles-$userId"

  private var state = UserRoleState()

  def receiveCommand: Receive = {
    case Get =>
      log.info(s"received a Get, sending to $sender")
      sender ! state
    case Subscribe(time, publication) =>
      persist(Subscribed(time, publication)) { evt =>
        updateState(evt)
        self.forward(Get)
      }
    case Unsubscribe(time, publication) =>
      persist(Unsubscribed(time, publication)) { evt =>
        updateState(evt)
        self.forward(Get)
      }
    case _ =>
      log.info("received unknown message")
  }

  def receiveRecover = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: UserRoleState) => state = snapshot
  }

  private def updateState(event: Event) = state = state.updated(event)
}
