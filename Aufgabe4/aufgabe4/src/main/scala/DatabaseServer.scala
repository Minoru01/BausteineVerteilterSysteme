import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameter, parameters, path, post, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.routing.RoundRobinGroup
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2, _}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/** DatabaseServer
 *
 * Creates the actor systems for the loadBalancer server and the database server
 * the routeeCount defines how much database actors for the database server should be created
 */
object DatabaseServer {

  def main(args: Array[String]): Unit = {
    val routeeCount = 5

    implicit val routeeSystem = ActorSystem("routee", ConfigFactory.load("routee.conf"))
    // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
    implicit val executionContext = routeeSystem.dispatcher

    // formats for unmarshalling and marshalling
    implicit val itemFormat = jsonFormat2(Item)
    implicit val orderFormat = jsonFormat1(Order)

    for (routeeNumber <- 1 to routeeCount) {
      routeeSystem.actorOf(Props(classOf[DataBaseActor]), "routee" + routeeNumber)
    }

    val routerSystem = ActorSystem("loadBalancerServer", ConfigFactory.load("server.conf"))

    val routeeAddressPrefix = "akka://routee@127.0.0.1:2566/user/"

    val routeeAddress = Seq.tabulate(routeeCount) {
      i => routeeAddressPrefix + "routee" + (i + 1)
    }
    val routerActor = routerSystem.actorOf(RoundRobinGroup(routeeAddress).props(), "router")
    val loadBalancerActor = routerSystem.actorOf(Props(classOf[LoadBalancerActor], routerActor, routeeCount), "loadBalancerActor")

    val route: Route =
      concat(
        get {
          path("get") {
            parameter("name") {
              case "" =>
                complete(StatusCodes.NotFound)
              case name: String =>
                implicit val timeout = Timeout(10 seconds)
                val future = loadBalancerActor ? Count(name)
                val result = Await.result(future, timeout.duration).asInstanceOf[OutputCount]
                complete(Item(name, result.count))
            }
          }
        },
        post {
          path("put") {
            parameters("name") {
              case "" =>
                complete(StatusCodes.BadRequest)
              case name: String =>
                loadBalancerActor ! Word(name)
                println("eigentlich fertig")
                complete(StatusCodes.Accepted)

              case unhandled =>
                println("Received unhandled message: " + unhandled)
                complete(StatusCodes.BadRequest)
            }
          }
        }
      )

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => routeeSystem.terminate()) // and shutdown when done
  }




}

