val http4sVersion = "0.23.22"
val blazeVersion = "0.23.15"
val tapirVersion = "1.6.0"

val catsEffectVersion = "3.5.1"
// val catsEffectVersion = "3.6-e9aeb8c"

val fs2Version = "3.7.0"
// val fs2Version = "3.8-1af22dd"

val zioHttpVersion = "3.0.0-RC2"
val zioVersion = "2.0.15"
val gatlingVersion = "3.9.5"
val logbackVersion = "1.4.7"

val bablVersion = "0.11.0"

// compiler options explicitly disabled from https://github.com/DavidGregory084/sbt-tpolecat
val disabledScalacOptionsCompile = Set(
  "-Xfatal-warnings",
  "-Wunused:privates",
)

lazy val commonSettings = Def.settings(
  name := "web-servers-benchmark",
  version := "0.1.0-SNAPSHOT",
  fork := true,
  scalaVersion := "2.13.11",
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
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.zip=ALL-UNNAMED",
    "-Djava.lang.Integer.IntegerCache.high=65536",
    "-Djava.net.preferIPv4Stack=true",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+TrustFinalNonStaticFields",
    // FIXME: It does't work for client/Gatling/test "-Xms2g",
    "-Xmx2g",
    "-XX:+AlwaysPreTouch",
    "-XX:+UseZGC",
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

      "com.aitusoftware" % "babl" % bablVersion,

      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,

      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,

      "ch.qos.logback" % "logback-classic" % logbackVersion,
    )
  )
  .settings(
    Universal / javaOptions := Seq(
      "-J--add-opens",
      "-Jjava.base/sun.nio.ch=ALL-UNNAMED",
      "-J--add-opens",
      "-Jjava.base/java.util.zip=ALL-UNNAMED",
      "-J-Djava.lang.Integer.IntegerCache.high=65536",
      "-J-Djava.net.preferIPv4Stack=true",
      "-J-XX:+UnlockExperimentalVMOptions",
      "-J-XX:+TrustFinalNonStaticFields",
      "-J-Xms2g",
      "-J-Xmx2g",
      "-J-XX:+AlwaysPreTouch",
      "-J-XX:+UseZGC",
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
    )
  )

lazy val web_servers_benchmark = (project in file("."))
  .settings(name := "web-servers-benchmark")
  .disablePlugins(JavaAppPackaging)
  .aggregate(server)
  .aggregate(client)
