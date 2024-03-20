val http4sVersion = "0.23.26"
val blazeVersion = "0.23.16"
val tapirVersion = "1.10.0"
val catsEffectVersion = "3.5.4"
val fs2Version = "3.10.0"
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
  scalaVersion := "2.13.13",
  scalacOptions ++= Seq("-release", "21"),
  javacOptions ++= Seq("-source", "21", "-target", "21"),
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
    "-Dcats.effect.tracing.mode=none",
    "-Dcats.effect.tracing.exceptions.enhanced=false",
    "-Dcats.effect.tracing.buffer.size=64",
    "-Djava.lang.Integer.IntegerCache.high=65536",
    "-Djava.net.preferIPv4Stack=true",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+TrustFinalNonStaticFields",
    "-Xms32g",
    "-Xmx32g",
    "-XX:+UseZGC",
    "-XX:+ZGenerational",
    "-XX:+AlwaysPreTouch",
    "-XX:TLABSize=1m",
    "-XX:-ResizeTLAB",
    "-XX:InitialCodeCacheSize=256m",
    "-XX:ReservedCodeCacheSize=256m",
    "-XX:NonNMethodCodeHeapSize=16m",
    "-XX:NonProfiledCodeHeapSize=120m",
    "-XX:ProfiledCodeHeapSize=120m",
    "-XX:+UseTransparentHugePages",
    "-XX:TimerSlack=5"
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
      "-J--add-opens",
      "-Jjava.base/sun.nio.ch=ALL-UNNAMED",
      "-J--add-opens",
      "-Jjava.base/java.util.zip=ALL-UNNAMED",
      "-J-Dcats.effect.tracing.mode=none",
      "-J-Dcats.effect.tracing.exceptions.enhanced=false",
      "-J-Dcats.effect.tracing.buffer.size=64",
      "-J-Djava.lang.Integer.IntegerCache.high=65536",
      "-J-Djava.net.preferIPv4Stack=true",
      "-J-XX:+UnlockExperimentalVMOptions",
      "-J-XX:+TrustFinalNonStaticFields",
      "-J-Xms32g",
      "-J-Xmx32g",
      "-J-XX:+UseZGC",
      "-J-XX:+ZGenerational",
      "-J-XX:+AlwaysPreTouch",
      "-J-XX:TLABSize=1m",
      "-J-XX:-ResizeTLAB",
      "-J-XX:InitialCodeCacheSize=256m",
      "-J-XX:ReservedCodeCacheSize=256m",
      "-J-XX:NonNMethodCodeHeapSize=16m",
      "-J-XX:NonProfiledCodeHeapSize=120m",
      "-J-XX:ProfiledCodeHeapSize=120m",
      "-J-XX:+UseTransparentHugePages",
      "-J-XX:TimerSlack=5"
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
      "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens", "java.base/java.util.zip=ALL-UNNAMED",
      "-Djava.lang.Integer.IntegerCache.high=65536",
      "-Djava.net.preferIPv4Stack=true",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+TrustFinalNonStaticFields",
      "-Xms32g",
      "-Xmx32g",
      "-XX:+UseZGC",
      "-XX:+ZGenerational",
      "-XX:+AlwaysPreTouch",
      "-XX:TLABSize=1m",
      "-XX:-ResizeTLAB",
      "-XX:InitialCodeCacheSize=256m",
      "-XX:ReservedCodeCacheSize=256m",
      "-XX:NonNMethodCodeHeapSize=16m",
      "-XX:NonProfiledCodeHeapSize=120m",
      "-XX:ProfiledCodeHeapSize=120m",
      "-XX:+UseTransparentHugePages",
      "-XX:TimerSlack=5"
    ),
  )

lazy val web_servers_benchmark = (project in file("."))
  .settings(name := "web-servers-benchmark")
  .disablePlugins(JavaAppPackaging)
  .aggregate(server)
  .aggregate(client)
