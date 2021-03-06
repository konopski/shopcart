package konopski.FirstFinatra

import org.jboss.netty.handler.codec.http.HttpMethod
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.twitter.finatra.test._
import com.twitter.finatra.{Request, FinatraServer}
import konopski.FirstFinatra._

class AppSpec extends FlatSpecHelper {

  val app = new App.ShopCartController
  override val server = new FinatraServer
  server.addFilter(new App.SimpleAuthFilter)
  server.register(app)


  "GET /unauthorized" should "respond 401" in {
    get("/unauthorized")
    response.body   should equal ("Not Authorized!")
    response.code   should equal (401)
  }

  "GET /whoami" should "respond 401 with no user" in {
    get("/whoami")
    response.body   should equal ("Not Authorized!")
    response.code   should equal (401)
  }

  "GET /whoami" should "respond 200" in {
    get("/whoami", headers=Map("X-USER" -> "lk"))
    response.body.contains("U R") should equal(true)
    response.code should equal(200)
  }

  "GET /notfound" should "respond 404" in {
    get("/notfound", headers=Map("X-USER" -> "lk"))
    response.body   should equal ("Not Found")
    response.code   should equal (404)
  }

  "GET /order" should "respond 200" in {
    get("/order", headers=Map("X-USER" -> "any"))
    response.body.contains("\"totalPrice\":") should equal(true)
    response.body.contains("\"products\":") should equal(true)
    response.code should equal(200)
  }

  "DELETE /order/all" should "respond 200" in {
    delete("/order/all", headers=Map("X-USER" -> "rw"))
    response.code should equal(200)
    get("/order", headers=Map("X-USER" -> "rw"))
    response.body.replaceAll("\\s", "").contains("\"totalPrice\":0") should equal(true)
    response.body.replaceAll("\\s", "").contains("\"products\":[]") should equal(true)

  }

  "DELETE /order" should "respond 200" in {
    val deleteRequest = Request("/order")
    deleteRequest.httpRequest.setMethod(HttpMethod.DELETE)
    deleteRequest.httpRequest.accept="application/json"
    deleteRequest.httpRequest.setContentString( "{\"product\":\"prod1\"}" )
    deleteRequest.httpRequest.headers().add("X-USER", "lk")
    send(deleteRequest)

    response.code should equal(200)
    get("/order", headers=Map("X-USER" -> "lk"))
    response.body.replaceAll("\\s", "").contains("\"totalPrice\":0") should equal(true)
    response.body.replaceAll("\\s", "").contains("\"products\":[]") should equal(true)

  }

}
