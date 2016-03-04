import spray.json.{DeserializationException, DefaultJsonProtocol,JsValue,JsString, JsonFormat}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import UserHandler._
import UsersManager._

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
