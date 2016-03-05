name := "roles-manager"

version := "1.0"

scalaVersion := "2.11.7"

val akkaV = "2.4.2"
val akkaHttpV = "2.0.3"
val eventstoreV = "2.2.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-persistence" % akkaV,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpV,
  "com.geteventstore" %% "akka-persistence-eventstore" % eventstoreV,
  "com.geteventstore" %% "eventstore-client" % eventstoreV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test")

fork in run := true
