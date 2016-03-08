import Publication._
import akka.actor._
import akka.persistence._
import java.time.ZonedDateTime

import scala.runtime.ScalaRunTime

object UserHandler {
  def props(id: Long) = Props(new UserHandler(id))

  sealed trait Command
  case object Get extends Command
  case class Subscribe(time: ZonedDateTime, publication: Publication) extends Command
  case class Unsubscribe(time: ZonedDateTime, publication: Publication) extends Command

  sealed trait Event{
    def publication: Publication
  }
  case class Subscribed(time: ZonedDateTime, publication: Publication) extends Event
  case class Unsubscribed(time: ZonedDateTime, publication: Publication) extends Event
}

case class UserRoleState(events: List[UserHandler.Event] = Nil) {
  def updated(evt: UserHandler.Event): UserRoleState = evt match {
    case e: UserHandler.Unsubscribed => updated(e)
    case e: UserHandler.Subscribed => updated(e)
  }
  def updated(evt: UserHandler.Unsubscribed): UserRoleState = copy(filterNot(events, evt))
  def updated(evt: UserHandler.Subscribed): UserRoleState = copy(evt :: filterNot(events, evt))
  def filterNot(events: List[UserHandler.Event], event: UserHandler.Event) = events.filterNot(e => e match {
    case UserHandler.Subscribed(time, publication) if publication == event.publication => true
    case _ => false
  })

  override def toString: String = events.reverse.toString
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

  def updateState(event: Event) = state = state.updated(event)
}
