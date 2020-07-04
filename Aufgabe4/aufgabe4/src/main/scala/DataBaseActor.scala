import akka.persistence.PersistentActor

import scala.collection.mutable

/** DataBaseActor
 *
 * receives words to put into map together with their frequency
 * receiving a count request will give back the stored frequency of a word
 *
 * received words get persisted. when the DataBaseActor dies it can recover the persisted words
 *
 */
class DataBaseActor extends PersistentActor{
  private val wordMap: mutable.Map[String, Int] = mutable.HashMap()

  println("The DatabaseActor: " + self.path.name + " was created")

  override def receiveRecover: Receive = {
     case w : Word => processText(w)
  }

  override def receiveCommand: Receive = {
    case w : Word =>
      persistAsync(w)(processText)

    case message => deferAsync(message) (update)
  }

  private def update(message : Any) : Unit = message match {
    case Count(word) =>
      val wordCount = wordMap.getOrElse(word, 0)
      println(self.path.name + ": countpart sent")
      sender ! CountPart(wordCount)

    case w : Word =>
      processText(w)

    case unhandled => println(self.path.name + ": Received unhandled message: " + unhandled)
  }

  /** processText
   *
   * Checks if the String of the Word message is already in the map and adds the word or increases the wordCount   *
   * @param w a received Word from a dissassembled text
   */
  def processText(w: Word) : Unit = {
    val wordCount = wordMap.getOrElse(w.word, 0)
    wordMap.update(w.word,wordCount + 1)
  }

  override def persistenceId: String = self.path.toString
}
