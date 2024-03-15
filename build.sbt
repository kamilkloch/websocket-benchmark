val http4sVersion = "0.23.22"
val blazeVersion = "0.23.15"
val tapirVersion = "1.6.0"
val catsEffectVersion = "3.5.1"
val fs2Version = "3.7.0"
val gatlingVersion = "3.10.4"
val logbackVersion = "1.5.3"

// compiler options explicitly disabled from https://github.com/DavidGregory084/sbt-tpolecat
val disabledScalacOptionsCompile = Set(
  "-Xfatal-warnings",
  "-Wunused:privates",
)

lazy val commonSettings = Def.settings(
  name := "web-servers-benchmark",
  version := "0.1.0-SNAPSHOT",
  fork := true,
  scalaVersion := "2.13.12",
  scalacOptions ++= Seq("-release", "17"),
  javacOptions ++= Seq("-source", "17", "-target", "17"),
  Compile / scalacOptions ~= ((options: Seq[String]) => options.filterNot(disabledScalacOptionsCompile)),
  Compile / scalacOptions ++= Seq(
    "-Wconf:any:warning-verbose", // print warnings with their category, site, and (for deprecations) origin and since-version
    "-Xsource:3", // disabled until IJ Scala plugin has stable support
    "-Vimplicits", // makes the compiler print implicit resolution chains when no implicit value can be found
    "-Vtype-diffs", // turns type error messages into colored diffs between the two types
    "-Wconf:cat=other-match-analysis:error", // report incomplete case match as error
    "-Wconf:cat=other-pure-statement:silent", // silence "unused value of type [???] (add `: Unit` to discard silently)"
    "-Wnonunit-statement",
  ),
  javaOptions := Seq(
    "-Xms32g",
    "-Xmx32g",
    "-XX:+UseZGC"
  ),
)

lazy val server = (project in file("server"))
  .settings(name := "server")
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % blazeVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    )
  )
  .settings(
    Universal / javaOptions := Seq(
      "-J-Xms32g",
      "-J-Xmx32g",
      "-J-XX:+UseZGC"
    )
  )

lazy val client = (project in file("client"))
  .settings(name := "client")
  .enablePlugins(GatlingPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test,
      "io.gatling" % "gatling-test-framework" % gatlingVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    ),
    Gatling / javaOptions := overrideDefaultJavaOptions(
      "-Xms32g",
      "-Xmx32g",
      "-XX:+UseZGC"
    ),
  )

lazy val web_servers_benchmark = (project in file("."))
  .settings(name := "web-servers-benchmark")
  .disablePlugins(JavaAppPackaging)
  .aggregate(server)
  .aggregate(client)
