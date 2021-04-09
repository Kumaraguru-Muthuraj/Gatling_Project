package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("http://demostore.gatling.io")
		.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")

	val headers_0 = Map("Upgrade-Insecure-Requests" -> "1")

	val headers_4 = Map(
		"Accept" -> "*/*",
		"X-Requested-With" -> "XMLHttpRequest")

	val headers_6 = Map(
		"Cache-Control" -> "max-age=0",
		"Origin" -> "http://demostore.gatling.io",
		"Upgrade-Insecure-Requests" -> "1")



	val scn = scenario("DemostoreSimulation")
		.exec(http("request_0")
			.get("/")
      .check(css("#_csrf", "content").saveAs("csrfValue"))
			.headers(headers_0))
		.pause(9)
		.exec(http("request_1")
			.get("/about-us")
			.headers(headers_0))
		.pause(9)
		.exec(http("request_2")
			.get("/category/all")
			.headers(headers_0))
		.pause(10)
		.exec(http("request_3")
			.get("/product/black-and-red-glasses")
			.headers(headers_0))
		.pause(11)
		.exec(http("request_4")
			.get("/cart/add/19")
			.headers(headers_4))
		.pause(9)
		.exec(http("request_5")
			.get("/cart/view")
			.headers(headers_0))
		.pause(16)
		.exec(http("request_6")
			.post("/login")
			.headers(headers_6)
			.formParam("_csrf", "${csrfValue}")
			.formParam("username", "user1")
			.formParam("password", "pass"))
		.pause(16)
		.exec(http("request_7")
			.get("/cart/checkout")
			.headers(headers_0))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
