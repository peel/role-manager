import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import Publication.Publication
import UserHandler._
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit object DateJsonFormat extends JsonFormat[ZonedDateTime] {
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    override def write(i: ZonedDateTime): JsValue = JsString(formatter.format(i))
    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(s) => ZonedDateTime.parse(s, formatter);
      case _ => throw new DeserializationException("Invalid or not ISO date")
    }
  }
  implicit object EventFormat extends JsonFormat[Event] {
    private val JsonDate = "date"
    private val JsonIsActive = "isActive"
    private val JsonPublication = "publication"

    override def write(evt: Event): JsValue = evt match {
      case e: Subscribed => writeJson(e.time, true, e.publication)
      case e: Unsubscribed => writeJson(e.time, false, e.publication)
    }
    private def writeJson(time: ZonedDateTime, status: Boolean, publication: Publication): JsValue = JsObject(
      (JsonDate, JsString(time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))),
      (JsonIsActive, JsBoolean(status)),
      (JsonPublication, publication.toJson)
    )

    override def read(json: JsValue): Event = json.asJsObject.getFields(JsonDate, JsonIsActive, JsonPublication) match {
      case Seq(JsString(date), JsBoolean(isActive), pub) if isActive =>
        Subscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME), pub.convertTo[Publication.Publication])
      case Seq(JsString(date), JsBoolean(isActive), pub) if !isActive =>
        Unsubscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME), pub.convertTo[Publication.Publication])
      case _ =>
        deserializationError(s"Event expected but not found")
    }
  }
  implicit object PublicationFormat extends JsonFormat[Publication] {
    override def read(json: JsValue): Publication = json.asJsObject.getFields("name") match {
      case Seq(JsString(name)) => Publication.find(name).getOrElse(deserializationError("Unknown publication"))
      case _ => deserializationError("Cannot parse persistent publication data")
    }
    override def write(p: Publication): JsValue = JsObject("name" -> JsString(p.toString))
  }
  implicit val roleStateFormat = jsonFormat1(UserRoleState.apply)
  implicit val roleChangeFormat = jsonFormat2(UserRoleChange.apply)
}
