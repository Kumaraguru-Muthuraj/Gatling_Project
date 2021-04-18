package gatlingdemostore

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class DemostoreSimulation extends Simulation {

  val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://" + domain)

  val categoryFeeder = csv("data/categoryDetails.csv").random
  val jsonFeederProducts = jsonFile("data/productDetails.json").random
  val loginFeeder = csv("data/loginDetails.csv").circular

  val rnd = new Random()
  def randomString(length: Int): String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10")toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  def getProperty(propertyName: String, defaultValue: String): String = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(session => session.set("cartTotal", 0.00))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
    // Comment out the print line during actual load
    //.exec{ session => println(session); session}


    // Tiding up the project
    //.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		//.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		//.acceptEncodingHeader("gzip, deflate")
		//.acceptLanguageHeader("en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
		//.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
  object CmsPages {
      def homePage = {
        exec(http("Load Home Page")
          .get("/")
          .check(status.is(200))
          .check(regex("<title>Gatling Demo-Store</title>").exists)
          .check(css("#_csrf", "content").saveAs("csrfValue")))
      }
      def aboutUs = {
        exec(http("Load About Us Page")
          .get("/about-us")
          .check(status.is(200)))
      }
  }

  object Catalog {
    object Category {
      def view = {
        feed(categoryFeeder)
        .exec(http("Load Category Page - ${categoryName}")
          .get("/category/${categorySlug}")
          .check(status.is(200))
          .check(css("#CategoryName").is("${categoryName}"))
        )
      }
    }
    object Product {
      def view = {
        feed(jsonFeederProducts)
          .exec(http("Load Product Page - ${name}")
            .get("/product/${slug}")
            .check(status.is(200))
            .check(css("#ProductDescription").is("${description}"))
          )
      }

      //Demonstrates how one can call other and chain.
      def add = {
        exec(view)
          .exec(http("Add Product to Cart")
            .get("/cart/add/${id}")
            .check(status.is(200))
            .check(substring("items in your cart"))
          )
          .exec(session => {
            val currentCartTotal = session("cartTotal").as[Double]
            val itemPrice = session("price").as[Double]
            session.set("cartTotal", currentCartTotal + itemPrice)
          })
      }
    }
  }

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

  object Customer {
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

  //Removed headers for tidying up
	val scn = scenario("DemostoreSimulation")
    .exec(initSession)
    .exec(CmsPages.homePage)
		.pause(2)
    .exec(CmsPages.aboutUs)
    .pause(2)
    .exec(Catalog.Category.view)
		.pause(2)
    // We moved view to Catalog.Product.add
    //.exec(Catalog.Product.view)
		//.pause(2)
    .exec(Catalog.Product.add)
		.pause(2)
    .exec(Checkout.viewCart)
		.pause(2)
    // Not required. Since View cart is doing login for you
    //.exec(Customer.login)
		//.pause(2)
    .exec(Checkout.completeCheckout)

  //MULTIPLE USER JOURNEYS
  object UserJourneys {
    def minPause = 100.milliseconds
    def maxPause = 500 milliseconds

    def browseStore = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(CmsPages.aboutUs)
        .pause(minPause, maxPause)
        .repeat(5) {
          exec(Catalog.Category.view)
            .pause(minPause, maxPause)
            .exec(Catalog.Product.view)
        }
    }

    def abandonCart = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
      .exec(Catalog.Category.view)
        .pause(minPause, maxPause) // Pause randomly between minPause and maxPause times
        .exec(Catalog.Product.add)
    }

    def completePurchase = {
      exec(initSession)
        .exec(CmsPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
        .pause(minPause, maxPause)
        .exec(Checkout.viewCart)
        .pause(minPause, maxPause)
        .exec(Checkout.completeCheckout)
    }

  }

  object Scenarios {
    def default = scenario ("Default Load Test")
      .during(60 seconds) {
        randomSwitch(
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
      }
    def highPurchase = scenario("High Purchase Load")
      .during(60 seconds) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
      }
  }

  setUp(Scenarios.default
    .inject(rampUsers(userCount) during (rampDuration seconds))
    .protocols(httpProtocol)
  )


  // By default we run only for 1 user - Uncomment this if we are just writing the code rahter than laod testing
	 //setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

  //ACTUAL LOAD TESTS
  //OPEN MODEL - Refer https://gatling.io/docs/current/general/simulation_setup/
  /*setUp(
    scn.inject(atOnceUsers(3),
      nothingFor(5.seconds),
      rampUsers(10) during(20.seconds),
      nothingFor(10.seconds),
      constantUsersPerSec(1) during(20.seconds)
    ).protocols(httpProtocol)
  )*/

  //CLOSED MODEL - Refer https://gatling.io/docs/current/general/simulation_setup/
  /*setUp(
    scn.inject(
      constantConcurrentUsers(10).during(20.seconds), // 1
      rampConcurrentUsers(10).to(20).during(20.seconds) // 2
    ).protocols(httpProtocol)
  )*/

  //THROTTLING MODEL - Throttles only to the upper RPS limit
  //Throttled traffic goes into a queue
  //Not balanced by request type
  /*setUp(scn.inject(constantUsersPerSec(1).during(3.minutes))).protocols(httpProtocol)
    .throttle(
    reachRps(10).in(30.seconds),
    holdFor(1.minute),
    jumpToRps(20),
    holdFor(1.minute)
  ).maxDuration(3.minutes)*/

  //MULTIPLE USER JOURNEYS

}
