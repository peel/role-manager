import akka.actor._
import java.time.ZonedDateTime

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
        case Some(h) => h.tell(UserHandler.Get, sender)
        case None => sender ! Empty
      }
    case Subscribe(id) =>
      log.info(s"User $id subscribed sent to ${handler(id)}")
      handler(id) ! UserHandler.Subscribe(ZonedDateTime.now)
    case Unsubscribe(id) =>
      log.info(s"User $id unsubscribed sent to ${handler(id)}")
      handler(id) ! UserHandler.Unsubscribe(ZonedDateTime.now)
  }

  def handler(id: Long) = handlerOpt(id) getOrElse context.actorOf(UserHandler.props(id), id.toString)
  def handlerOpt(id: Long): Option[ActorRef] = context.child(id.toString)
}
