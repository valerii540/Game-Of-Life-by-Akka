name := "game-of-life"

version := "0.1"

scalaVersion := "2.13.5"

fork := true

libraryDependencies ++= {
  val akkaVersion = "2.6.13"

  Seq(
    "ch.qos.logback"     % "logback-classic"  % "1.2.3",
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"      % akkaVersion,
    "com.typesafe.akka" %% "akka-http"        % "10.2.4"
  )
}

scalacOptions ++= Seq("-deprecation", "-feature")
