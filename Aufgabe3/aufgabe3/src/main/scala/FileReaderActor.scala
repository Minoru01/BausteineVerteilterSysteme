import akka.actor.{Actor, ActorRef}

import scala.io.Source
import scala.language.postfixOps

/** FileReaderActor
 *
 * gets a filepath as string. Converts it, reads the file line by line and sends it to lineReader
 */
class FileReaderActor(lineReaderActor : ActorRef) extends Actor {
  def receive : Receive = {
    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      context.stop(self)


    case FilePath(path) =>
        val bufferedSource = Source.fromResource(path)
        for (line <- bufferedSource.getLines) {
          lineReaderActor ! Line(line)
        }
        bufferedSource.close
        println("File: " + path + " processed")

    case c : Count =>
      println("fileread actor count")
      lineReaderActor.forward(c)

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}
