import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorRef}

import scala.io.Source
import scala.language.postfixOps

class FileReaderActor(lineReaderActor : ActorRef) extends Actor {
  def receive : Receive = {
    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      context.stop(self)

    /**
     * gets a filepath as string. Converts it, reads the file line by line and sends it to lineReader
     */
    case FilePath(path) =>
      if (Files.exists(Paths.get(path))) {
        val bufferedSource = Source.fromFile(path)
        for (line <- bufferedSource.getLines) {
          lineReaderActor ! Line(line)
        }
        bufferedSource.close
        println("File: " + path + " processed")

      }
      else
        println("File '" + path + "' was not found")

    case c : Count =>
      lineReaderActor ! c

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }
}
