import akka.actor.{Actor, ActorRef}
import akka.routing.Broadcast

/** LoadBalancerActor
 *
 * Sends the incomming words to all connected Actors with the given routerActor.
 * If a Count is received the router broadcasts the Count to all connected Actors.
 * The Actor response with a CountPart message with their actual count for the String in Count.
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
      if (valueParts.size == routeeCount) {
        preservedSender ! OutputCount(valueParts.sum)
        println("Ergebnis gesendet: " + valueParts.sum)
        valueParts = Seq[Int]()
      }

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}