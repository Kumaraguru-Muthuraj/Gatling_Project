package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

  val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://" + domain)

    // Tiding up the project
    //.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		//.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		//.acceptEncodingHeader("gzip, deflate")
		//.acceptLanguageHeader("en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
		//.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")

  //Removed headers for tidying up
	val scn = scenario("DemostoreSimulation")
		.exec(http("Load Home Page")
			.get("/")
      .check(regex("<title>Gatling Demo-Store</title>").exists)
      .check(css("#_csrf", "content").saveAs("csrfValue")))
		.pause(2)
		.exec(http("Load About Us Page")
			.get("/about-us"))
    .pause(2)
		.exec(http("Load Categories Page")
			.get("/category/all"))
		.pause(2)
		.exec(http("Load Product Page")
			.get("/product/black-and-red-glasses"))
		.pause(2)
		.exec(http("Add Product to Cart")
			.get("/cart/add/19"))
		.pause(2)
		.exec(http("View Cart")
			.get("/cart/view"))
		.pause(2)
		.exec(http("Login User")
			.post("/login")
			.formParam("_csrf", "${csrfValue}")
			.formParam("username", "user1")
			.formParam("password", "pass"))
		.pause(2)
		.exec(http("Checkout")
			.get("/cart/checkout"))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
