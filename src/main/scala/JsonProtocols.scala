import spray.json.{ DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, JsBoolean, JsonFormat }
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import UserHandler._
import UsersManager._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit object EventFormat extends JsonFormat[Event]{
    override def write(evt: Event) = evt match {
      case Subscribed(time: ZonedDateTime) => toJson(time, "subscribed")
      case Unsubscribed(time: ZonedDateTime) => toJson(time, "unsubscribed")
    }
    override def read(json: JsValue) = ??? //not used
    private def toJson(time: ZonedDateTime, role: String) = JsObject(
      ("date", JsString(time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))),
      ("role", JsString(role)))
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
