import akka.actor._
import akka.persistence._

object Main extends App {
  import UsersManager._

  val system = ActorSystem("rolesmanager-system")
  val usersManager = system.actorOf(UsersManager.props, "users-manager")
  usersManager ! Subscribe(1)
  usersManager ! Unsubscribe(1)
  usersManager ! Status(1)
  usersManager ! Status(2)

  system.awaitTermination()
}

object UsersManager {
  def props = Props[UsersManager]

  case class Subscribe(id: Long)
  case class Unsubscribe(id: Long)
  case class Status(id: Long)
  case object Status
  case object Empty
}

class UsersManager extends Actor with ActorLogging {
  import UsersManager._

  override def receive = {
    case Status =>
      log.info(s"Get all events")
    case Status(id) =>
      log.info(s"Get user $id events sent to ${handler(id)}")
      handlerOpt(id) match {
        case Some(h) => h ! UserHandler.Get
        case None => sender ! Empty
      }
    case Subscribe(id) =>
      log.info(s"User $id subscribed sent to ${handler(id)}")
      handler(id) ! UserHandler.Subscribe
    case Unsubscribe(id) =>
      log.info(s"User $id unsubscribed sent to ${handler(id)}")
      handler(id) ! UserHandler.Unsubscribe
  }

  def handler(id: Long) = handlerOpt(id) getOrElse context.actorOf(UserHandler.props(id), id.toString)
  def handlerOpt(id: Long): Option[ActorRef] = context.child(id.toString)
}

object UserHandler {
  def props(id: Long) = Props(new UserHandler(id))

  sealed trait Command
  case object Get extends Command
  case object Subscribe extends Command
  case object Unsubscribe extends Command

  sealed trait Event
  case object Subscribed extends Event
  case object Unsubscribed extends Event
}
class UserHandler(userId: Long) extends PersistentActor with ActorLogging {
  import UserHandler._

  override val persistenceId = s"user-roles-$userId"

  private var state = UserRoleState(Nil)

  def receiveCommand: Receive = {
    case Get =>
      sender ! state
    case Subscribe =>
      persist(Subscribed){evt =>
        updateState(evt)
        self.forward(Get)
      }
    case Unsubscribe =>
      persist(Unsubscribed){evt =>
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

import UserHandler._
case class UserRoleState(events: List[Event] = Nil) {
  def updated(evt: Event): UserRoleState = copy(evt :: events)
  override def toString: String = events.reverse.toString
}
