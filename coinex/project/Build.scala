import sbt._
import Keys._
import com.twitter.scrooge._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.SbtScalariform._
// import org.sbtidea.SbtIdeaPlugin._
import com.typesafe.sbt.SbtAtmos.{ Atmos, atmosSettings }
import sbtassembly.Plugin._
import AssemblyKeys._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

object CoinexBuild extends Build {
  val coinexVersion = "1.1.23-SNAPSHOT"

  val akkaVersion = "2.3.3"
  val bijectionVersion = "0.6.2"
  val sprayVersion = "1.3.1"
  val scroogeVersion = "3.13.0"

  val sharedSettings = Seq(
    organization := "com.coinport",
    version := coinexVersion,
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    initialCommands in console := """
      import com.coinport.coinex.Client
      import com.coinport.coinex.monitoring.MonitorTest
      import com.coinport.coinex.data._
      import com.coinport.coinex.data.Currency._
      import com.coinport.coinex.robot.sample._
      import akka.pattern.ask
      import akka.util.Timeout
      import scala.concurrent.duration._
      import com.redis.serialization.Parse.Implicits.parseByteArray
      import com.coinport.coinex.bitway._
      import com.coinport.coinex.serializers._
      import com.coinport.coinex.performance._
      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val timeout = Timeout(2 seconds)
      val se = new ThriftBinarySerializer()
      // BitwayClient.client.rpush("creq", se.toBinary(BitwayRequest(BitwayType.Transfer, 123, Currency.Btc, transferRequest = Some(TransferRequest("hoss", "chao", 12)))))
      // BitwayClient.client.rpush("creq", se.toBinary(BitwayRequest(BitwayType.GenerateWallet, 123, Currency.Btc, generateWalletRequest = Some(GenerateWalletRequest(Some("mima"))))))
    """,
    scalacOptions ++= Seq("-encoding", "utf8"),
    scalacOptions ++= Seq("-optimize"),
    scalacOptions += "-deprecation",
    publishArtifact in Test := false,
    publishMavenStyle := true,
    publishTo := Some("Sonatype Snapshots Nexus" at "http://192.168.0.105:8081/nexus/content/repositories/snapshots"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/groups/public",
      "Spray Repo" at "http://repo.spray.io"
      // "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"
      )) ++ assemblySettings ++ Seq(
      test in assembly := {},
      mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
        {
          case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
          case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
          // case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
          // case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
          case "application.conf" => MergeStrategy.concat
          case "unwanted.txt"     => MergeStrategy.discard
          case x => old(x)
        }
      }
    )

  lazy val root = Project(
    id = "coinex",
    base = file("."),
    settings = Project.defaultSettings ++ sharedSettings)
    .aggregate(client, backend)

  lazy val client = Project(
    id = "coinex-client",
    base = file("coinex-client"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      ScroogeSBT.newSettings ++
      scalariformSettings
    )
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
      "org.specs2" %% "specs2" % "2.3.8" % "test",
      "org.scalatest" %% "scalatest" % "2.0" % "test",
      "com.twitter" %% "bijection-scrooge" % bijectionVersion,
      "com.twitter" %% "bijection-json4s" % bijectionVersion,
      "com.twitter" %% "bijection-json" % bijectionVersion,
      "com.twitter" %% "scrooge-core" % scroogeVersion,
      "com.twitter" %% "scrooge-serializer" % scroogeVersion,
      "org.slf4s" %% "slf4s-api" % "1.7.6",
      "io.spray" %%  "spray-json" % "1.2.5",
      "org.json4s" %% "json4s-native" % "3.2.7",
      "org.json4s" %% "json4s-ext" % "3.2.7",
      "com.google.guava" % "guava" % "16.0.1",
      "org.mongodb" %% "casbah" % "2.6.5",
      "com.twitter" %% "util-eval" % "6.12.1",
      "org.apache.thrift" % "libthrift" % "0.8.0"),
      libraryDependencies += ("com.twitter" % "chill-akka_2.10" % "0.3.6")
        .exclude("com.esotericsoftware.minlog", "minlog")
        .exclude("org.ow2.asm", "asm")
    )

  lazy val backend = Project(
    id = "coinex-backend",
    base = file("coinex-backend"),
    settings = Project.defaultSettings ++
      SbtMultiJvm.multiJvmSettings ++
      sharedSettings ++
      ScroogeSBT.newSettings ++
      scalariformSettings
    )
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(packageArchetype.java_server:_*)
    .settings(packageDescription in Debian := "coinex")
    .settings(
      libraryDependencies += ("com.coinport" %% "akka-persistence-hbase" % "1.0.9-SNAPSHOT")
        .exclude("org.jboss.netty", "netty")
        .exclude("org.jruby", "jruby-complete")
        .exclude("javax.xml.stream", "stax-api")
        .exclude("javax.xml.stream", "stax-api")
        .exclude("commons-beanutils", "commons-beanutils")
        .exclude("commons-beanutils", "commons-beanutils-core")
        .exclude("tomcat", "jasper-runtime")
        .exclude("tomcat", "jasper-compiler")
        .exclude("org.slf4j", "slf4j-log4j12"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-remote" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
        "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
        "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.7",
        "com.github.scullxbones" % "akka-persistence-mongo-casbah_2.10" % "0.0.9",
        "org.specs2" %% "specs2" % "2.3.8" % "test",
        "org.scalatest" %% "scalatest" % "2.0" % "test",
        "org.apache.commons" % "commons-io" % "1.3.2",
        "ch.qos.logback" % "logback-classic" % "1.1.2",
        "ch.qos.logback" % "logback-core" % "1.1.2",
        "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.42" % "test",
        "io.spray" % "spray-can" % sprayVersion,
        "io.spray" % "spray-routing" % sprayVersion,
        "io.spray" % "spray-client" % sprayVersion,
        "io.spray" % "spray-http" % sprayVersion,
        "io.argonaut" %% "argonaut" % "6.0.4",
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
        "net.debasishg" % "redisclient_2.10" % "2.12"),
      compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
      parallelExecution in Test := false,
      executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
        case (testResults, multiNodeResults)  =>
          val overall =
            if (testResults.overall.id < multiNodeResults.overall.id)
              multiNodeResults.overall
            else
              testResults.overall
          Tests.Output(overall,
            testResults.events ++ multiNodeResults.events,
            testResults.summaries ++ multiNodeResults.summaries)
      }
    )
    .dependsOn(client)
    .configs(MultiJvm)
}
