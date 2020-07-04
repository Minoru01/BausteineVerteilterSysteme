import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory

/** LoadBalancerServer
 *
 * For excercise 3:
 * Creates the actor systems for the loadBalancer with the pool of DataBase Actors
 *
 */
object LoadBalancerServer {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("loadBalancerServer",ConfigFactory.load("server.conf"))
    val loadBalancerActorName = "loadBalancerActor"
    val numberOfRoutees = 5
    val routerActor = system.actorOf(RoundRobinPool(numberOfRoutees).props(Props[DataBaseActor]), "router")

    system.actorOf(Props(classOf[LoadBalancerActor],routerActor, numberOfRoutees), name = loadBalancerActorName)
    println(loadBalancerActorName + " was created")
  }
}