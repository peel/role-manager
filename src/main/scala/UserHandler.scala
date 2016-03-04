import akka.actor._
import akka.persistence._
import java.time.ZonedDateTime

object UserHandler {
  def props(id: Long) = Props(new UserHandler(id))

  sealed trait Command
  case object Get extends Command
  case class Subscribe(time: ZonedDateTime) extends Command
  case class Unsubscribe(time: ZonedDateTime) extends Command

  sealed trait Event
  case class Subscribed(time: ZonedDateTime) extends Event
  case class Unsubscribed(time: ZonedDateTime) extends Event
}

case class UserRoleState(events: List[UserHandler.Event] = Nil) {
  def updated(evt: UserHandler.Event): UserRoleState = copy(evt :: events)
  override def toString: String = events.reverse.toString
}

class UserHandler(userId: Long) extends PersistentActor with ActorLogging {
  import UserHandler._

  override val persistenceId = s"user-roles-$userId"

  private var state = UserRoleState(Nil)

  def receiveCommand: Receive = {
    case Get =>
      log.info(s"received a Get, sending to $sender")
      sender ! state
    case Subscribe(time) =>
      persist(Subscribed(time)) { evt =>
        updateState(evt)
        self.forward(Get)
      }
    case Unsubscribe(time) =>
      persist(Unsubscribed(time)) { evt =>
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
