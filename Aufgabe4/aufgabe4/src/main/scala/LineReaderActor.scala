import akka.actor.{Actor, ActorSelection}

/** LineReaderActor
 *
 * receives lines from the file from processed by the FileReaderActor
 * and uses regex to extract the words from the line.
 * The words are sent to the load balancer Actor
 */
class LineReaderActor(loadBalancer : ActorSelection) extends Actor {
  def receive : Receive = {
    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      context.stop(self)

    case Line(line) =>
      val regex = "(\\w)+".r
      val allWords = regex.findAllMatchIn(line)

      for (word <- allWords) {
        loadBalancer ! Word(word.toString())
      }

    case c : Count =>
      loadBalancer.forward(c)

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}


