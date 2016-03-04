import akka.actor._
import akka.persistence._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._


import UserHandler._
case class UserRoleState(events: List[Event] = Nil) {
  def updated(evt: Event): UserRoleState = copy(evt :: events)
  override def toString: String = events.reverse.toString
}
case class UserRoleChange(isSubscribed: Boolean)

import spray.json.{DeserializationException, DefaultJsonProtocol,JsValue,JsString, JsonFormat}
trait JsonProtocols extends DefaultJsonProtocol {
  implicit object EventFormat extends JsonFormat[Event]{
    override def write(e: Event) = e match {
      case Subscribed(time: ZonedDateTime) => JsString("true")
      case Unsubscribed(time: ZonedDateTime) => JsString("false")
    }
    override def read(json: JsValue) = ???
  }
  implicit object DateJsonFormat extends JsonFormat[ZonedDateTime] {
    val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    override def write(i: ZonedDateTime) = JsString(formatter.format(i))
    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(s) => ZonedDateTime.parse(s, formatter);
      case _ => throw new DeserializationException("Invalid or not ISO date")
    }
  }
  implicit val roleStateFormat = jsonFormat1(UserRoleState.apply)
  implicit val roleChangeFormat = jsonFormat1(UserRoleChange.apply)
}

object Main extends App with JsonProtocols {
  import UsersManager._

  implicit val system = ActorSystem("rolesmanager-system")
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  implicit val timeout = Timeout(5 seconds)
  val usersManager = system.actorOf(UsersManager.props, "users-manager")

  Http().bindAndHandle(interface = "0.0.0.0", port = 8019, handler = {
    pathPrefix("users" / IntNumber) { userId =>
      (patch & pathEndOrSingleSlash) {
        entity(as[UserRoleChange]) { msg =>
          complete {
            msg.isSubscribed match {
              case true =>
                usersManager ! Subscribe(userId)
                OK
              case false =>
                usersManager ! Unsubscribe(userId)
                OK
            }
          }
        }
      } ~
      (get & pathEndOrSingleSlash) {
        onSuccess(usersManager ? Status(userId)) {
            case UserRoleState(evs) => complete(evs)
            case Empty => complete(NotFound)
          }
      }
    }
  })
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
