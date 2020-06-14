
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.{Broadcast, RoundRobinPool}
import com.typesafe.config.ConfigFactory


class LoadBalancerActor extends Actor{

  private var valueParts = Seq[Int]()
  private val routeeCount = 5
  val router: ActorRef = context.actorOf(RoundRobinPool(routeeCount).props(Props[NewDatabaseActor]), "router")

  def receive : Receive = {
    case Word(word) =>
      router ! Word(word)

    case Count(word) =>
      router ! Broadcast(Count(word))

    case CountPart(value) =>
      valueParts = valueParts:+ value
      if (valueParts.size == routeeCount)
        println("The selected word is " + valueParts.sum + " times in the text")
      else
        println(valueParts.size + " parts of Count makes: " + valueParts.sum + " times right now")

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}

object Server {
  def main(args: Array[String]): Unit = {

val system = ActorSystem("server",ConfigFactory.load("server.conf"))
    val loadBalancerActorName = "loadBalancerActor1"
    val loadBalancer1 = system.actorOf(Props(classOf[LoadBalancerActor]), name = loadBalancerActorName)
    println(loadBalancerActorName + " was created")
  }
}

