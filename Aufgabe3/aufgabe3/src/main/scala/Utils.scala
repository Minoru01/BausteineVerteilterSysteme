import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object Utils {
  def createSystem(fileName: String, systemName : String) : ActorSystem = {
    val configFile = getClass
      .getClassLoader
      .getResource(fileName)
      .getFile
    val config = ConfigFactory.parseFile(new File(configFile))
    val result = ActorSystem(systemName,config)
    return result
  }
}
