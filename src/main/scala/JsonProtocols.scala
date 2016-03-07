import spray.json._
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import UserHandler._
import UsersManager._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit object DateJsonFormat extends JsonFormat[ZonedDateTime] {
    val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    override def write(i: ZonedDateTime) = JsString(formatter.format(i))
    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(s) => ZonedDateTime.parse(s, formatter);
      case _ => throw new DeserializationException("Invalid or not ISO date")
    }
  }
  implicit object UnsubscribedFormat extends JsonFormat[Unsubscribed]{
    override def write(evt: Unsubscribed) = JsObject(
      ("date", JsString(evt.time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))),
      ("role", JsString(Role.Unsubscribed.toString)))
    override def read(json: JsValue) = ??? //not used
  }
  implicit object SubscribedFormat extends JsonFormat[Subscribed]{
    override def write(evt: Subscribed) = JsObject(
      ("date", JsString(evt.time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))),
      ("role", JsString(Role.Subscribed.toString)))
    override def read(json: JsValue) = ??? //not used
  }
  implicit object EventFormat extends JsonFormat[Event]{
    override def write(evt: Event) = evt match {
      case s: Subscribed => s.toJson
      case u: Unsubscribed => u.toJson
    }
    override def read(json: JsValue) = json.asJsObject.getFields("date", "role") match {
      case Seq(JsString(date),JsString(role)) if role == Role.Subscribed.toString => Subscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME))
      case Seq(JsString(date),JsString(role)) if role != Role.Unsubscribed.toString => Unsubscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME))
      case err => deserializationError(s"Event expected but found: $err")
    }
  }
  implicit val roleStateFormat = jsonFormat1(UserRoleState.apply)
  implicit val roleChangeFormat = jsonFormat1(UserRoleChange.apply)
}

object Role extends Enumeration{
  type Role = Value
  val Subscribed = Value("subscribed")
  val Unsubscribed = Value("unsubscribed")
}
