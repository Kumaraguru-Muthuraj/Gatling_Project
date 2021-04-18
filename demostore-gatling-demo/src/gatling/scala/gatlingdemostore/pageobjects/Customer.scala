package gatlingdemostore.pageobjects

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

object Customer {
  val loginFeeder = csv("data/loginDetails.csv").circular
  def login = {
    feed(loginFeeder)
      .exec(http("Load Login page")
        .get("/login")
        .check(status.is(200))
        .check(substring("Username:")))
      //Debug to print user logged in status
      //.exec{ session => println(session); session}
      .exec(http("Customer Login Action")
        .post("/login")
        .formParam("_csrf", "${csrfValue}")
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .check(status.is(200)))
      .exec(session => session.set("customerLoggedIn", true))
    //Debug to print user logged in status
    //.exec{ session => println(session); session}
  }
}
