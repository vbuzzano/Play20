package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.api.libs.json._

import views._

import models._

object Contacts extends Controller {

  /**
   * Contact Form definition.
   */
  val contactForm: Form[Contact] = Form(

    // Defines a mapping that will handle Contact values
    mapping(
      "firstname" -> nonEmptyText,
      "lastname" -> nonEmptyText,
      "company" -> optional(text),

      // Defines a repeated mapping
      "informations" -> seq(
        mapping(
          "label" -> nonEmptyText,
          "email" -> optional(email),
          "phones" -> seq(
            text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")
          )
        )(ContactInformation.apply)(ContactInformation.unapply)
      )

    )(Contact.apply)(Contact.unapply)
  )

  val contactValidation = {
    import play.api.data.validation2._
    import Mappings._
    import Constraints._
    import Validations._

    val __ = Path[JsValue]()

    val infoValidation =
      ((__ \ "label").validate(nonEmptyText) ~
      (__ \ "email").validate(optional(email)) ~
      (__ \ "phones").validate(seq(pattern("""[0-9.+]+""".r)))) (ContactInformation.apply _)

    ((__ \ "firstname").validate(nonEmptyText) ~
    (__ \ "lastname").validate(nonEmptyText) ~
    (__ \ "company").validate[Option[String]] ~
    (__ \ "informations").validate(seq(infoValidation))) (Contact.apply _)
  }


  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.contact.form(contactForm));
  }

  /**
   * Display a form pre-filled with an existing Contact.
   */
  def editForm = Action {
    val existingContact = Contact(
      "Fake", "Contact", Some("Fake company"), informations = List(
        ContactInformation(
          "Personal", Some("fakecontact@gmail.com"), List("01.23.45.67.89", "98.76.54.32.10")
        ),
        ContactInformation(
          "Professional", Some("fakecontact@company.com"), List("01.23.45.67.89")
        ),
        ContactInformation(
          "Previous", Some("fakecontact@oldcompany.com"), List()
        )
      )
    )
    Ok(html.contact.form(contactForm.fill(existingContact)))
  }

  private def negotiate: BodyParser[Either[Map[String,Seq[String]], JsValue]] = parse.using{ r =>
    r.contentType match {
      case Some("text/json" | "application/json") => parse.json.map(Right(_))
      case _ => parse.urlFormEncoded.map(Left(_))
    }
  }

  /**
   * Handle form submission.
   */
  // curl http://localhost:9000/contacts -XPOST -H "Content-Type: application/json" -d "{\"firstname\":\"Julien\",\"lastname\":\"Tournay\",\"age\":27,\"informations\":[{\"label\":\"Personal\",\"email\":\"fakecontact@gmail.com\",\"phones\":[\"01.23.45.67.89\",\"98.76.54.32.10\"]}]}" -i
  def submit = Action(negotiate) { implicit request =>
    request.body.fold(
      form => NotImplemented,
      json =>
        contactValidation.validate(json).fold(
          err => BadRequest(Json.toJson(err)),
          _ => Ok))
    //contactForm.bindFromRequest.fold(
    //  errors => BadRequest(html.contact.form(errors)),
    //  contact => Ok(html.contact.summary(contact))
    //)
  }

}