
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.{Broadcast, RoundRobinPool}
import com.typesafe.config.ConfigFactory

/** LoadBalancerActor
 *
 * Sends the incomming words to all connected Actors with the given routerActor balanced
 * if a Count is received the router broadcasts the Count to all connected Actors
 * the Actor response with a CountPart message with their actual count for the String in Count
 *
 */
class LoadBalancerActor(routerActor : ActorRef, routeeCount : Int) extends Actor{
  var preservedSender : ActorRef = _

  private var valueParts = Seq[Int]()

  def receive : Receive = {
    case w : Word =>
      routerActor ! w

    case c : Count =>
      println("sends broadcast to DatabaseActor...")
      preservedSender = sender()
      routerActor ! Broadcast(c)


    case CountPart(value) =>
      valueParts = valueParts:+ value
      if (valueParts.size == routeeCount)
        preservedSender ! OutputCount(valueParts.sum)
      else
        println(valueParts.size + " parts of Count makes: " + valueParts.sum + " times right now")

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}

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

