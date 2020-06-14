import akka.actor.{Actor, ActorSystem, Props}

class ReadSherlockActor extends Actor{
  val pathPrefix="akka://server@127.0.0.1:2565/user/"

  val loadBalancerActor1Path=pathPrefix+"loadBalancerActor1"
  val remoteLoadBalancerActor1 = context.actorSelection(loadBalancerActor1Path)

  val system = ActorSystem("system")

  val lineReaderActorName = "lineReaderActor1"
  val lineReaderActor1 = system.actorOf(Props(classOf[LineReaderActor], remoteLoadBalancerActor1), name = lineReaderActorName)
  println(lineReaderActorName + " was created")

  val fileReaderActorName = "fileReaderActor1"
  val fileReaderActor1 = system.actorOf(Props(classOf[FileReaderActor], lineReaderActor1), name = fileReaderActorName)
  println(fileReaderActorName + " was created")

  fileReaderActor1 ! FilePath(".\\res\\sherlock.txt")
  println("sherlock.txt got delivered to fileReaderActor")

  fileReaderActor1 ! Count("Sherlock")

  override def receive: Receive = {
    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}

object ReadSherlock {

  def main(args: Array[String]): Unit = {

    Utils.createSystem("client.conf", "client")
      .actorOf(Props[ReadSherlockActor], name = "client-actor")




  }
}