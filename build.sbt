name := "roles-manager"

version := "1.0"

scalaVersion := "2.11.8"

val akkaV = "2.4.2"
val eventstoreV = "2.2.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-persistence" % akkaV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-http-core" % akkaV,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
  "com.geteventstore" %% "akka-persistence-eventstore" % eventstoreV,
  "com.geteventstore" %% "eventstore-client" % eventstoreV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test")

fork in run := true
