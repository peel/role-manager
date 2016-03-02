import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.persistence.PersistentActor

object Main extends App {
  import UsersManager._

  val system = ActorSystem("rolesmanager-system")
  val usersManager = system.actorOf(UsersManager.props, "users-manager")
  usersManager ! Subscribe(1)
  usersManager ! Subscribe(2)
  usersManager ! Subscribe(1)
  usersManager ! Status(1)
  usersManager ! Status(2)

  system.awaitTermination()
}

object UsersManager {
  def props = Props[UsersManager]
  case class Subscribe(id: Long)
  case object Subscribe
  case class Unsubscribe(id: Long)
  case object Unsubscribe
  case class Status(id: Long)
  case object Status
}
class UsersManager extends Actor with ActorLogging {
  import UsersManager._

  override def receive = {
    case Status =>
      log.info(s"Get all events")
    case Status(id) =>
      log.info(s"Get user $id events")
      handler(id) ! Status
    case Subscribe(id) =>
      log.info(s"User $id subscribed")
      handler(id) ! Subscribe(id)
    case Unsubscribe(id) =>
      log.info(s"User $id unsubscribed")
      handler(id) ! Unsubscribe(id)
  }

  def handler(id: Long): ActorRef = context.child(id.toString).getOrElse(context.actorOf(UserHandler.props(id), id.toString))
}

object UserHandler {
  def props(id: Long) = Props(new UserHandler(id))

  sealed trait Command
  case object Get extends Command
  case class Subscribe(id: Long) extends Command
  case class Unsubscribe(id: Long) extends Command

  sealed trait Event
  case class Subscribed(id: Long) extends Event
  case class Unsubscribed(id: Long) extends Event

}
class UserHandler(userId: Long) extends PersistentActor with ActorLogging {
  import UserHandler._

  override val persistenceId = s"user-roles-$userId"

  var state = RoleState()

  def receiveCommand = {
    case Get =>
      sender ! state
    case Subscribe(id) =>
      persist(Subscribed(id)){evt =>
        updateState(evt)
        self.forward(Get)
      }
    case Unsubscribe(id) =>
      persist(Subscribed(id)){evt =>
        updateState(evt)
        self.forward(Get)
      }
  }

  def receiveRecover = {
    case evt: Event => updateState(evt)
  }

  def updateState(event: Event) = state.updated(event)
}

import UserHandler._
case class RoleState(events: List[Event] = Nil) {

  def updated(evt: Event): RoleState = copy(evt :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}
