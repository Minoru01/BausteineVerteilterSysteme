import akka.actor.{Actor, ActorSelection}


class LineReaderActor(loadBalancer : ActorSelection) extends Actor {

  def receive : Receive = {
    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      context.stop(self)

    /**
     * receives lines from the file from processed by the FileReaderActor
     * and uses regex to extract the words from the line.
     * The words are sent to the load balancer Actor
     */
    case Line(line) =>
      val regex = "(\\w)+".r
      val allWords = regex.findAllMatchIn(line)

      for (word <- allWords) {
        loadBalancer ! Word(word.toString())
      }

    case c : Count =>
      loadBalancer ! c

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)



  }
}


