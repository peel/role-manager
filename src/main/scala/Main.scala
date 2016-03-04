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
import scala.concurrent.duration._

case class UserRoleChange(isSubscribed: Boolean)

object Main extends App with JsonProtocols {
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
                usersManager ! UsersManager.Subscribe(userId)
                OK
              case false =>
                usersManager ! UsersManager.Unsubscribe(userId)
                OK
            }
          }
        }
      } ~
      (get & pathEndOrSingleSlash) {
        onSuccess(usersManager ? UsersManager.Status(userId)) {
            case UserRoleState(evs) => complete(evs)
            case UsersManager.Empty => complete(NotFound)
          }
      }
    }
  })
}
