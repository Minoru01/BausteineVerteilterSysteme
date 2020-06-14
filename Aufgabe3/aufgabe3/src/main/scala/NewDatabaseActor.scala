import akka.persistence.PersistentActor

import scala.collection.mutable


class NewDatabaseActor extends PersistentActor{
  val wordMap: mutable.Map[String, Int] = mutable.HashMap()
  println(" the DatabaseActor: " + self.path.name + " was created")

  override def receiveRecover: Receive = {
    case w : Word => processText(w)

    case unhandled => println(self.path.name + ": Recovered unhandled message: " + unhandled)
  }

  override def receiveCommand: Receive = {
    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      context.stop(self)

    case Count(word) =>
      val wordCount = wordMap.getOrElse(word, 0)
      sender ! CountPart(wordCount)

    case w : Word =>
      persistAsync(w)(processText)

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }

  def processText(w: Word) : Unit = {
    val wordCount = wordMap.getOrElse(w.word, 0)
    //println(w.word + " : " + (wordCount + 1))
    wordMap.update(w.word,wordCount + 1)
  }

  override def persistenceId: String = self.path.toString
}
