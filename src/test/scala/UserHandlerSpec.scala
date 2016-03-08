import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class UserHandlerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A UserHandler actor" must {
    "set event of subscription or unsubscription" in {
      val handler = system.actorOf(UserHandler.props(1))
      val time = java.time.ZonedDateTime.now
      handler ! UserHandler.Subscribe(time, Publication.Nwt)
      expectMsg(UserRoleState(List[UserHandler.Event](UserHandler.Subscribed(time, Publication.Nwt))))
    }
  }
}
