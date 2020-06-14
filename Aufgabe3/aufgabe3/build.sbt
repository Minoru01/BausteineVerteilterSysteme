name := "aufgabe3"

version := "0.1"

scalaVersion := "2.13.2"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.6"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.6.6"

// https://mvnrepository.com/artifact/org.iq80.leveldb/leveldb
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.12"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

//https://mvnrepository.com/artifact/com.h2database/h2
libraryDependencies += "com.h2database" % "h2" % "1.4.200"

//routing
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.6.6"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.6.6"