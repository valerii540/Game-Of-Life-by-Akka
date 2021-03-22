name := "game-of-life"

version := "0.1"

scalaVersion := "2.13.5"

fork := true

libraryDependencies ++= {
  val akka  = "2.6.13"
  val kamon = "2.1.13"

  Seq(
    "ch.qos.logback"     % "logback-classic"    % "1.2.3",
    "com.typesafe.akka" %% "akka-actor-typed"   % akka,
    "com.typesafe.akka" %% "akka-stream"        % akka,
    "com.typesafe.akka" %% "akka-http"          % "10.2.4",
    "io.kamon"          %% "kamon-bundle"       % kamon,
    "io.kamon"          %% "kamon-apm-reporter" % kamon
  )
}

scalacOptions ++= Seq("-deprecation", "-feature")
