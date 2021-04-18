package gatlingdemostore.pageobjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Checkout {
  def viewCart = {
    doIf(session => !session("customerLoggedIn").as[Boolean]) {
      exec(Customer.login)
    }
      .exec(http("Load cart page")
        .get("/cart/view")
        .check(status.is(200))
        //Get the grandTotal from the form and compare with cartTotal. We also want the Dollar Symbol to be compared
        .check(css("#grandTotal").is("$$${cartTotal}")))
  }

  def completeCheckout = {
    exec(http("Checkout Cart")
      .get("/cart/checkout")
      .check(status.is(200))
      .check(substring("Thanks for your order! See you soon!")))
  }
}
