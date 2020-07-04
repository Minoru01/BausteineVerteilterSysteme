import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/** ReadSherlockActor
 *
 * Creates the lineReader and fileReader Actor in the ReadSherlock System
 */
class ReadSherlockActor extends Actor{
  private val pathPrefix="akka://loadBalancerServer@127.0.0.1:2565/user/"

  private val loadBalancerActor1Path=pathPrefix+"loadBalancerActor"
  private val remoteLoadBalancerActor1 = context.actorSelection(loadBalancerActor1Path)

  private val system = context.system

  private val lineReaderActorName = "lineReaderActor1"
  private val lineReaderActor1 = system.actorOf(Props(classOf[LineReaderActor], remoteLoadBalancerActor1), name = lineReaderActorName)
  println(lineReaderActorName + " was created")

  private val fileReaderActorName = "fileReaderActor1"
  private val fileReaderActor1 = system.actorOf(Props(classOf[FileReaderActor], lineReaderActor1), name = fileReaderActorName)
  println(fileReaderActorName + " was created")

  private val startTime = System.currentTimeMillis()
  fileReaderActor1 ! FilePath("sherlock.txt")
  println("sherlock.txt got delivered to fileReaderActor")

  fileReaderActor1 ! Count("Sherlock")

  override def receive: Receive = {
    case OutputCount( count) =>
      println("the selected word is " + count + " times in the text")
      val endTime = System.currentTimeMillis()
      println("Runtime: " + (endTime - startTime)/1000 + "s")

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}

/** ReadSherlock
 *
 * Creates the actor system to input and process the sherlock.txt
 */
object ReadSherlock {
  def main(args: Array[String]): Unit = {
    ActorSystem("client",ConfigFactory.load("client.conf")).actorOf(Props[ReadSherlockActor], name = "client-actor")
  }
}



