import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameter, parameters, path, post, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.routing.RoundRobinGroup
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.zookeeper._

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/** DatabaseServer
 *
 * Creates the actor systems for the loadBalancer server and the database server.
 * The routeeCount defines how much database actors for the database server should be created.
 * Route defines the behavior of the restful Web-Service.
 */
object DatabaseServer extends Watcher {

  def createLoadBalancerServer(routeeAddressPrefix: String, routeeCount: Int, serverConf: String): ActorRef = {
    val routeeAddress = Seq.tabulate(routeeCount) {
      i => routeeAddressPrefix + "routee" + (i + 1)
    }
    val routerSystem = ActorSystem("loadBalancerServer", ConfigFactory.load(serverConf))
    routerSystem.registerExtension(RemoteAddressExtension)
    val routerActor = routerSystem.actorOf(RoundRobinGroup(routeeAddress).props(), "router")

    routerSystem.actorOf(Props(classOf[LoadBalancerActor], routerActor, routeeCount), "loadBalancerActor")
  }

  private val routeeAddressPrefix = "akka://routee@127.0.0.1:2566/user/"
  private val routeeCount = 5

  implicit val webSystem: ActorSystem = ActorSystem("webSystem", ConfigFactory.load("webSystem.conf"))
  implicit val executionContext: ExecutionContextExecutor = webSystem.dispatcher
  private val zooConnect = "127.0.0.1:2181"

  private val zk = new ZooKeeper(zooConnect, 5000, this)

  if (zk.exists("/node", false) == null) {
    zk.create("/node", new Array[Byte](0), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
  }

  Thread.sleep(1000)
  createLoadBalancerServer(routeeAddressPrefix, routeeCount, "server.conf")
  Thread.sleep(1000)
  createLoadBalancerServer(routeeAddressPrefix, routeeCount, "server2.conf")
  Thread.sleep(1000)

  private var address = new String(zk.getData("/node", this, null))

  private val nodeChildren = zk.getChildren("/node", this)
  val value = new String(zk.getData("/node/" + nodeChildren.get(0), this, null))
  zk.setData("/node", value.getBytes(), -1)

  private var router = webSystem.actorSelection(address)

  override def process(we: WatchedEvent): Unit = we.getType match {
    case Watcher.Event.EventType.NodeDataChanged =>
      address = new String(zk.getData("/node", this, null))
      router = webSystem.actorSelection(address)
      println("new Router address: " + address)
    case Watcher.Event.EventType.NodeDeleted =>
      val newNodeChildren = zk.getChildren("/node", this)

      val newValue = new String(zk.getData("/node/" + newNodeChildren.get(0), this, null))
      zk.setData("/node", newValue.getBytes(), -1)
  }

  def main(args: Array[String]): Unit = {

    println(s"Watching service at http://127.0.0.1:2181/\nPress RETURN to stop...")

    val routeeSystem = ActorSystem("routee", ConfigFactory.load("routee.conf"))
    // needed for the future map/flatmap in the end and future in fetchItem and saveOrder

    for (routeeNumber <- 1 to routeeCount) {
      routeeSystem.actorOf(Props(classOf[DataBaseActor]), "routee" + routeeNumber)
    }

    val route: Route =
      concat(
        get {
          path("select") {
            parameter("name") {
              case "" =>
                complete(StatusCodes.NotFound)
              case name: String =>
                implicit val timeout: Timeout = Timeout(5 seconds)

                val future = router ? Count(name)

                val result = Await.result(future, timeout.duration).asInstanceOf[OutputCount]
                if (result.count > 0) {
                  val responseBody = s"{ 'status' : 'found', record: {'name' : $name ,'frequency' : '${result.count}'} }"
                  complete(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, responseBody))
                }
                else {
                  val responseBody = s"{ 'status' : 'not found', record: {'name' : $name ,'frequency' : '${result.count}'} }"
                  complete(StatusCodes.NotFound, HttpEntity(ContentTypes.`application/json`, responseBody))
                }
            }
          }
        },
        post {
          path("insert") {
            parameters("name") {
              case "" =>
                complete(StatusCodes.BadRequest)
              case name: String =>
                router ! Word(name)
                complete(HttpEntity(ContentTypes.`application/json`, "{ 'status': 'inserted' }"))

              case unhandled =>
                println("Received unhandled message: " + unhandled)
                complete(StatusCodes.BadRequest)
            }
          }
        }
      )
    val bindingFuture = Http().bindAndHandle(route, "localhost", 2571)
    println(s"Server online at http://localhost:2571/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => routeeSystem.terminate()) // and shutdown when done
  }
}

