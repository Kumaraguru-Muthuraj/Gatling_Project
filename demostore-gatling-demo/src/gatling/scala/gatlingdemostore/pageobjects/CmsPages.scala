package gatlingdemostore.pageobjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

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
