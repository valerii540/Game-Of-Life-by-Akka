name := "game-of-life"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic"   % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-typed"  % "2.6.13",
  "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.13"
)

scalacOptions ++= Seq("-deprecation", "-feature")
