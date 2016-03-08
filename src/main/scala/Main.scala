import scala.concurrent.duration._

import Publication._
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

trait Config {
  protected val config = ConfigFactory.load()
  protected val interface = config.getString("http.interface")
  protected val port = config.getInt("http.port")
}

object Main extends App with JsonProtocols with Config {
  implicit val system = ActorSystem("rolesmanager-system")
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val usersManager = system.actorOf(UsersManager.props, "users-manager")

  Http().bindAndHandle(interface = interface, port = port, handler = {
    pathPrefix("users" / IntNumber) { userId =>
      (patch & pathEndOrSingleSlash) {
        entity(as[UserRoleChange]) { msg =>
          complete {
            msg.isSubscribed match {
              case true =>
                usersManager ! UsersManager.Subscribe(userId, msg.publication)
                OK
              case false =>
                usersManager ! UsersManager.Unsubscribe(userId, msg.publication)
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
