import java.sql.DriverManager

import akka.actor.{Actor, ReceiveTimeout}

import scala.concurrent.duration.Duration

class DatabaseActor extends Actor {
  val conn = DriverManager.getConnection("jdbc:h2:./dtb/sherlockDB", "", "")

  def receive : Receive = {

    case ("command", "bye") =>
      println("DatabaseActor received ''bye''")
      conn.close()
      context.stop(self)

    /**
     * if the DatabaseActor receives a tupel of the String "count"
     * and a String with the word wanted to get its appearance in the database
     * the Actor sends PrintActor the frequency as String
     */
    case ("count", pureText : String) =>
      val statement = conn.createStatement()
      val tableWord = statement.executeQuery("select * from ALLWORDS where WORD = '"+ pureText + "'")
      val text = tableWord.getString("WORD")
      val count = tableWord.getInt("COUNT")

      sender ! "text: " + text + ", count: " + count

      context.setReceiveTimeout(Duration.create("2 second"))

    /**
     * if the DatabaseActor receives a String the Actor checks if this String is already in the database
     * if there no entry the String gets inserted.
     * if not then the count in the entry gets increased by one
     */
    case text: String =>
      try {
        val statement = conn.createStatement()
        val tableWord = statement.executeQuery("select * from ALLWORDS where WORD = '"+ text+ "'")
        if (tableWord.first){
          statement.executeUpdate("update ALLWORDS set COUNT = COUNT + 1 where WORD = '"+ text+ "'")
          //println("word count increased for: " + text)
        }
        else{
          statement.executeUpdate("insert into ALLWORDS values('"+ text +"', 1)")
          //println("new word added")
        }
      }
      catch {
        case e:Exception => e.printStackTrace()
      }
      context.setReceiveTimeout(Duration.create("2 second"))

    case ReceiveTimeout =>
      conn.close()
      context.stop(self)
      println(self.path.name + " has nothing more to process and terminates")

    case _ =>
      println(self.path.name + " received some unknown argument: ")
  }
}
