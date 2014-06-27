package com.coinport.coinex.common

import de.flapdoodle.embed.mongo.{ Command, MongodStarter }
import de.flapdoodle.embed.mongo.config.{ ArtifactStoreBuilder, DownloadConfigBuilder, MongodConfigBuilder, Net, RuntimeConfigBuilder }
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.IRuntimeConfig
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.extract.UUIDTempNaming
import de.flapdoodle.embed.process.io.{ NullProcessor, Processors }
import de.flapdoodle.embed.process.io.directories.FixedPath
import de.flapdoodle.embed.process.runtime.Network
import com.mongodb.casbah.MongoConnection
import org.scalatest.{ Matchers, WordSpecLike, BeforeAndAfterAll, Suite }

trait EmbeddedMongoForTest {
  lazy val host = "localhost"
  lazy val port = 52345 // DO NOT CHANGE THIS PORT
  lazy val localHostIPV6 = Network.localhostIsIPv6()

  val artifactStorePath = new FixedPath("./mongodb")
  val executableNaming = new UUIDTempNaming()
  val command = Command.MongoD
  val version = Version.Main.PRODUCTION

  // Used to filter out console output messages.
  val processOutput = new ProcessOutput(
    Processors.named("[mongod>]", new NullProcessor),
    Processors.named("[MONGOD>]", new NullProcessor),
    Processors.named("[console>]", new NullProcessor))

  val runtimeConfig: IRuntimeConfig =
    new RuntimeConfigBuilder()
      .defaults(command)
      .processOutput(processOutput)
      .artifactStore(new ArtifactStoreBuilder()
        .defaults(command)
        .download(new DownloadConfigBuilder()
          .defaultsForCommand(command)
          .artifactStorePath(artifactStorePath))
        .executableNaming(executableNaming))
      .build()

  val mongodConfig =
    new MongodConfigBuilder()
      .version(version)
      .net(new Net(port, localHostIPV6))
      .build()

  lazy val mongodStarter = MongodStarter.getInstance(runtimeConfig)
  lazy val mongod = mongodStarter.prepare(mongodConfig)
  lazy val mongodExe = mongod.start()
  lazy val connection = MongoConnection(host, port)
  lazy val database = connection("coinex_test")

  def embeddedMongoStartup() {
    mongodExe
  }

  def embeddedMongoShutdown() {
    try {
      //  connection.close()
      mongod.stop()
      mongodExe.stop()
    } catch {
      case e: Throwable =>
    }
  }
}

trait EmbeddedMongoForTestWithBF extends EmbeddedMongoForTest with WordSpecLike with Matchers with BeforeAndAfterAll with Suite {
  override def beforeAll() {
    super.embeddedMongoStartup()
  }
  override def afterAll() {
    super.embeddedMongoShutdown()
  }
}

