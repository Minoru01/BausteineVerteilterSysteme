import akka.actor.{Actor, Props}

class RouteeActor extends Actor{
  override def receive: Receive = {

    case unhandled => println(self.path.name + ": Recovered unhandled message: " + unhandled)

  }
}


object Routee {

  def main(args: Array[String]): Unit = {

    Utils.createSystem("routee.conf", "routee")
      .actorOf(Props[RouteeActor], name = "routee-actor")




  }
}