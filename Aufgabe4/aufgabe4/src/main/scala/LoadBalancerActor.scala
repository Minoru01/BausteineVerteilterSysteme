import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.routing.Broadcast
import org.apache.zookeeper.{CreateMode, ZooDefs, ZooKeeper}

/** LoadBalancerActor
 *
 * Sends the incomming words to all connected Actors with the given routerActor.
 * If a Count is received the router broadcasts the Count to all connected Actors.
 * The Actor response with a CountPart message with their actual count for the String in Count.
 *
 */
class LoadBalancerActor(routerActor: ActorRef, routeeCount: Int) extends Actor with ActorLogging {

  private val zk = new ZooKeeper("localhost:2181", 5000, null)
  private val address = context.system.extension(RemoteAddressExtension).address
  zk.create(
    "/node/router",
    (address + "/user/" + self.path.name).getBytes,
    ZooDefs.Ids.OPEN_ACL_UNSAFE,
    CreateMode.EPHEMERAL_SEQUENTIAL)

  /**
   * Closes the Zookeeper connection to delete its ephemeral node.
   */
  override def postStop(): Unit = {
    zk.close()
    super.postStop
  }

  log.info("wurde erstellt.")
  var preservedSender: ActorRef = _

  private var valueParts = Seq[Int]()

  def receive: Receive = {

    case Word("throwError") =>
      log.info("an test-error occured")
      throw new IllegalStateException()

    case w: Word =>
      routerActor ! w

    case c: Count =>
      println("sends broadcast to DatabaseActor...")
      preservedSender = sender()
      routerActor ! Broadcast(c)

    case CountPart(value) =>
      valueParts = valueParts :+ value
      if (valueParts.size == routeeCount) {
        preservedSender ! OutputCount(valueParts.sum)
        println("Ergebnis gesendet: " + valueParts.sum)
        valueParts = Seq[Int]()
      }

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}