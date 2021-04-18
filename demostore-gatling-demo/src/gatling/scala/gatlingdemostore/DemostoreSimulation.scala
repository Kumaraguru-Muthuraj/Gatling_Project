package gatlingdemostore

import gatlingdemostore.pageobjects.{Catalog, Checkout, CmsPages}

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class DemostoreSimulation extends Simulation {

  val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://" + domain)

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

  before {
    println(s"Running test with ${userCount} users")
    println(s"Running users over ${rampDuration} seconds")
    println(s"Total test duration is ${testDuration} seconds")
  }
  after {
    println("Load tests completed")
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
      .during(testDuration) {
        randomSwitch(
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
      }
    def highPurchase = scenario("High Purchase Load")
      .during(testDuration) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
      }
  }

  //RUN THE default and high purchase scenarios in PARALLEL
  setUp(
    Scenarios.default.inject(rampUsers(userCount) during (rampDuration seconds)).protocols(httpProtocol),
    Scenarios.highPurchase.inject(rampUsers(5) during (60 seconds)).protocols(httpProtocol)
  )

  // RUN THE default and high purchase scenarios in SEQUENCE
  /*setUp(
    Scenarios.default
    .inject(rampUsers(userCount) during (rampDuration seconds)).protocols(httpProtocol)
      .andThen(
        Scenarios.highPurchase
          .inject(rampUsers(5) during (10 seconds)).protocols(httpProtocol)
      )
  )*/

  /*RUNNING DEFAULT SCENARIO ALONE
  setUp(Scenarios.default
    .inject(rampUsers(userCount) during (rampDuration seconds))
    .protocols(httpProtocol)
  )*/

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
