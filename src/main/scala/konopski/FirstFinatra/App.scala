package konopski.FirstFinatra

import java.util.Random

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Response, Request}
import com.twitter.finatra._
import com.twitter.finatra.ContentType

import scala.annotation.tailrec
import scala.util.parsing.json.JSON

object App extends FinatraServer {


  type ProductName = String
  type Cash = BigDecimal
  type Quantity = Int

  object ListOrderService {

    case class ListOrderResponse(products: List[(ProductName, Cash, Quantity)],
                            totalPrice: Cash)

    def calcTotal(prods: List[(ProductName, Cash, Quantity)]) = {
      @tailrec
      def calcTotal0(prods: List[(ProductName, Cash, Quantity)], acc: Cash): Cash = {
        def price = prods.head._2 * prods.head._3

        if (prods.isEmpty) acc
        else calcTotal0(prods.tail, acc + price)
      }
      calcTotal0(prods, 0)
    }

    def prepareListOrderResp(user: String): ListOrderResponse = {
      val prods = DataBase.getUsersProductsWithQuantityAndPrice(user)
      ListOrderResponse(prods, calcTotal(prods))
    }
  }

  object DataBase {
    val random = new Random()

    val prices = new java.util.HashMap[String, Cash]

    val data = new java.util.HashMap[String, List[(ProductName, Cash, Quantity)]]
    data.put("lk", List(("prod1", BigDecimal(12), 1 )))
    data.put("rw", List(("prod2", BigDecimal(20), 2 )))

    def deleteAll(user: String) ={
      data.put(user, List())
    }

    def getProductPrice(prod: ProductName): Cash = {
      def randCash: Cash = random.nextInt(50)
      if(null == prices.get(prod)) prices.put(prod, randCash)
      prices.get(prod)
    }

    def getUsersProductsWithQuantityAndPrice(user: String):
          List[(ProductName, Cash, Quantity)] = {
      val result = data.get(user)
      if (null==result) List()
      else result
    }
    
    def has(user: String, prod: ProductName) = {
      val order = getUsersProductsWithQuantityAndPrice(user)
      order.exists( (tup) => tup._1 == prod )
    }

    def updateQuantity(user: String, prod: ProductName, q: Quantity) = {
      val order = getUsersProductsWithQuantityAndPrice(user)
      val updated = order.find((tup) => tup._1 == prod) map (
        x => (x._1, x._2, q)
      )
      val newData = updated.get :: order.filterNot( (tup) => tup._1 == prod )
      data.put(user, newData)
    }

    def delete(user: String, prod: ProductName) = {
      val order = getUsersProductsWithQuantityAndPrice(user)
      val newData = order.filterNot( (tup) => tup._1 == prod )
      data.put(user, newData)
    }

    def addToOrder(user: String, prod: ProductName, q: Quantity) = {
      val order = getUsersProductsWithQuantityAndPrice(user)
      val newData = (prod, getProductPrice(prod), q) :: order
      data.put(user, newData)
    }
  }

  object UpdateService {
    def delete(user: String, product: ProductName) = {
      DataBase.delete(user, product)
    }

    def deleteAll(user: String) = {
      DataBase.deleteAll(user)
    }

    def put(user: String, prod: ProductName, q: Quantity) = {
      if(DataBase.has(user, prod)) {
        if(q>0) DataBase.updateQuantity(user, prod, q)
        else if(q==0) DataBase.delete(user, prod)
        else throw new IllegalArgumentException
      }
      else DataBase.addToOrder(user, prod, q)
    }
  }

  class ShopCartController extends Controller {

    put("/order") { request =>
      val user = request.headers().get(USER_HEADER)
      respondTo(request) {
        case _: ContentType.Json =>  {
          val input = JSON.parseFull(request.contentString)
          input match {
            case Some(map: Map[String, Any]) => {
              val product = map("product").asInstanceOf[ProductName]
              val quantity = map("quantity").asInstanceOf[Double].toInt
              UpdateService.put(user, product, quantity)
              render.plain("SUCCESS").toFuture
            }
            case _ => log.error("unmatched json payload") ;throw new BadRequest
          }
        }
        case _ => log.error("unmatched content type") ;throw new BadRequest
      }
    }

    delete("/order") { request =>
      val user = request.headers().get(USER_HEADER)
      log.info("content type:" + request.accepts )
      respondTo(request) {
        case _: ContentType.Json =>  {
          val input = JSON.parseFull(request.contentString)
          input match {
            case Some(map: Map[String, Any]) => {
              val product = map("product").asInstanceOf[ProductName]
              UpdateService.delete(user, product)
              render.status(200).toFuture
            }
            case _ => log.error("unmatched json payload") ;throw new BadRequest
          }
        }
        case _ => log.error("unmatched content type") ;throw new BadRequest
      }

    }

    delete("/order/all") { request =>
      val user = request.headers().get(USER_HEADER)
      UpdateService.deleteAll(user)
      render.status(200).toFuture
    }

    get("/order") { request =>
      val user = request.headers().get(USER_HEADER)
      render.json(ListOrderService.prepareListOrderResp(user)).toFuture
    }

    get("/whoami") { request =>
      render.plain("U R " + request.headers().get(USER_HEADER)).toFuture
    }

    get("/unauthorized") { request =>
      throw new Unauthorized
    }

    error { request =>
      log.error("see an error", request.error)
      request.error match {
        case Some(e:ArithmeticException) =>
          render.status(500).plain("whoops, go back to math class!").toFuture
        case Some(e:Unauthorized) =>
          render.status(401).plain("Not Authorized!").toFuture
        case Some(e:BadRequest) =>
          render.status(400).plain("Wrong format of data!").toFuture
        case _ =>
          render.status(500).plain("Something went wrong!").toFuture
      }
    }
  }

  val USER_HEADER = "X-User"

  class SimpleAuthFilter
    extends SimpleFilter[Request , Response] with App {

    def apply(request: Request, service: Service[Request, Response]) = {
      request.headerMap.get(USER_HEADER) match {
        case Some(user) => {
          log.info("logged: " + user)
          service(request)
        }
        case None => {
          log.error("unauthorized " + request.remoteSocketAddress.getHostString)
          request.uri="/unauthorized"
          service(request)
        }
      }

    }
  }

  class Unauthorized extends Exception
  class BadRequest extends Exception

  addFilter(new SimpleAuthFilter())
  register(new ShopCartController())
}
