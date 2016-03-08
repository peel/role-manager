object Publication extends Enumeration {
  type Publication = Value
  val Mt = Value("mt")
  val Sla = Value("sla")
  val Nwt = Value("nwt")
  val Ep = Value("ep")

  def find(str: String): Option[Publication] = Publication.values.find(_.toString == str)
}

import UserHandler._
import Publication._
case class UserRoleState(events: List[Event] = Nil) {

  def updated(evt: Event): UserRoleState = evt match {
    case e: Unsubscribed => updated(e)
    case e: Subscribed => updated(e)
  }
  private def updated(evt: Unsubscribed): UserRoleState = copy(filterNot(events, evt))
  private def updated(evt: Subscribed): UserRoleState = copy(evt :: filterNot(events, evt))
  private def filterNot(events: List[Event], event: SubscriptionEvent) = events.filterNot(e => e match {
    case Subscribed(time, publication) if publication == event.publication => true
    case _ => false
  })

  override def toString: String = events.reverse.toString
}
case class UserRoleChange(isSubscribed: Boolean, publication: Publication)

