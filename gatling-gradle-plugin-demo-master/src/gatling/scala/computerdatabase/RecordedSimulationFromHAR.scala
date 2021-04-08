package computerdatabase
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class RecordedSimulationFromHAR extends Simulation {

	val httpProtocol = http
		.baseUrl("https://computer-database.gatling.io")
		.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
		.upgradeInsecureRequestsHeader("1")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")

/*
	val headers_0 = Map(
		"Cache-Control" -> "max-age=0",
		"Sec-Fetch-Dest" -> "document",
		"Sec-Fetch-Mode" -> "navigate",
		"Sec-Fetch-Site" -> "none",
		"Sec-Fetch-User" -> "?1",
		"sec-ch-ua" -> """Google Chrome";v="89", "Chromium";v="89", ";Not A Brand";v="99""",
		"sec-ch-ua-mobile" -> "?0")

	val headers_1 = Map(
		"Sec-Fetch-Dest" -> "document",
		"Sec-Fetch-Mode" -> "navigate",
		"Sec-Fetch-Site" -> "same-origin",
		"Sec-Fetch-User" -> "?1",
		"sec-ch-ua" -> """Google Chrome";v="89", "Chromium";v="89", ";Not A Brand";v="99""",
		"sec-ch-ua-mobile" -> "?0")

	val headers_2 = Map(
		"Cache-Control" -> "max-age=0",
		"Origin" -> "https://computer-database.gatling.io",
		"Sec-Fetch-Dest" -> "document",
		"Sec-Fetch-Mode" -> "navigate",
		"Sec-Fetch-Site" -> "same-origin",
		"Sec-Fetch-User" -> "?1",
		"sec-ch-ua" -> """Google Chrome";v="89", "Chromium";v="89", ";Not A Brand";v="99""",
		"sec-ch-ua-mobile" -> "?0")
*/
  /*
  1. We can create separate classes to move sets of HTTP requests for
  readability and maintainability.
  2. Rename the request_<N> to meaningful names that can be interpreted.
  3.
   */
  object ReqOne {
    val rq1 = exec(http("Search Mac")
              .get("/computers?f=Mac")
              .headers(headers_0)) //Can remove headers
              .pause(10)
  }

  object ReqTwo {
    val rq2 =	exec(http("Add new Computer")
              .get("/computers/new")
              .headers(headers_1)) //Can remove headers
              .pause(20)
  }

  object ReqThree {
    val rq3 = exec(http("Add NextGenmac3")
              .post("/computers")
              .headers(headers_2) //Can remove headers
              .formParam("name", "NextGenmac3")
              .formParam("introduced", "2021-04-07")
              .formParam("discontinued", "")
              .formParam("company", "1"))
  }
  // If there was 

	val scn = scenario("RecordedSimulationFromHAR").exec(ReqOne.rq1, ReqTwo.rq2, ReqThree.rq3);




	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
