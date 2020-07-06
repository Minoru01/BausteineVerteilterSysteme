import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

object WebServer {
  // needed to run the route
  implicit val system = ActorSystem()

  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  def main(args: Array[String]) {

    val route: Route =
      concat(
        get {
          path("get") {
            parameter("name") {
              case "" =>
                complete(StatusCodes.NotFound)
              case name : String =>

                complete(Item(name,12))
            }
          }
        },
        post {
          path("put") {
            parameters("name"){
              case "" =>
                complete(StatusCodes.BadRequest)
              case name : String =>
                
                complete(Item(name, 13))
            }
          }
        }
      )

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

    StdIn.readLine()

  }
}
