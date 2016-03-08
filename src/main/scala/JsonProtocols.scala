import spray.json._
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import UserHandler._

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
      ("isActive", JsBoolean(false)),
      ("publication", evt.publication.toJson))
    override def read(json: JsValue) = ??? //not used
  }
  implicit object SubscribedFormat extends JsonFormat[Subscribed]{
    override def write(evt: Subscribed) = JsObject(
      ("date", JsString(evt.time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))),
      ("isActive", JsBoolean(true)),
      ("publication", evt.publication.toJson))
    override def read(json: JsValue) = ??? //not used
  }
  implicit object EventFormat extends JsonFormat[Event]{
    override def write(evt: Event): JsValue = evt match {
      case s: Subscribed => s.toJson
      case u: Unsubscribed => u.toJson
    }
    override def read(json: JsValue) = json.asJsObject.getFields("date", "isActive", "publication") match {
      case Seq(JsString(date),JsBoolean(isActive), pub) if isActive == true =>
        Subscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME),pub.convertTo[Publication.Publication])
      case Seq(JsString(date),JsBoolean(isActive), pub) if isActive == false =>
        Unsubscribed(ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME), pub.convertTo[Publication.Publication])
      case err =>
        deserializationError(s"Event expected but found: $err")
    }
  }
  implicit object PublicationFormat extends JsonFormat[Publication.Publication] {
    override def read(json: JsValue): Publication.Publication = json.asJsObject.getFields("name") match {
      case Seq(JsString(name)) => Publication.find(name).getOrElse(deserializationError("Unknown publication"))
    }
    override def write(p: Publication.Publication): JsValue = JsObject("name" -> JsString(p.toString))
  }
  implicit val roleStateFormat = jsonFormat1(UserRoleState.apply)
  implicit val roleChangeFormat = jsonFormat2(UserRoleChange.apply)
}

object Publication extends Enumeration{
  type Publication = Value
  val Mt = Value("mt")
  val Sla = Value("sla")
  val Nwt = Value("nwt")
  val Ep = Value("ep")

  def find(str: String): Option[Publication] = Publication.values.find(_.toString == str)
}
