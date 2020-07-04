import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinGroup
import com.typesafe.config.ConfigFactory

/** DatabaseServer
 *
 * For excercise 4:
 * Creates the actor systems for the loadBalancer server and the database server
 * the routeeCount defines how much database actors for the database server should be created
 */
object DatabaseServer {
  def main(args: Array[String]): Unit = {
    val routeeCount = 5

    val routeeSystem = ActorSystem("routee", ConfigFactory.load("routee.conf"))

    for (routeeNumber <- 1 to routeeCount) {
      routeeSystem.actorOf(Props(classOf[DataBaseActor]), "routee" + routeeNumber)
    }

    val routerSystem = ActorSystem("loadBalancerServer", ConfigFactory.load("server.conf"))

    val routeeAddressPrefix = "akka://routee@127.0.0.1:2566/user/"

    val routeeAddress = Seq.tabulate(routeeCount) {
      i => routeeAddressPrefix + "routee" + (i + 1)
    }
    val routerActor = routerSystem.actorOf(RoundRobinGroup(routeeAddress).props(), "router")
    routerSystem.actorOf(Props(classOf[LoadBalancerActor], routerActor, routeeCount), "loadBalancerActor")
  }
}