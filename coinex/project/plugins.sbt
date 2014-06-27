
resolvers ++= Seq(
  // "Nexus Snapshots" at "http://192.168.0.105:8081/nexus/content/groups/public/",
  "JMParsons Releases" at "http://jmparsons.github.io/releases/",
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Classpaths.typesafeResolver
)


libraryDependencies ++= Seq(
  // "com.github.siasia" %% "xsbt-web-plugin" % "0.12.0-0.2.11.1",
  // "com.decodified" % "scala-ssh" % "0.6.2",
  "org.bouncycastle" % "bcprov-jdk16" % "1.46",
  "com.jcraft" % "jzlib" % "1.1.1"
)

// addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.0")

// addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.5.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")

// addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.13.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-atmos" % "0.3.2")

// addSbtPlugin("reaktor" % "sbt-scct" % "0.2-SNAPSHOT")

// addSbtPlugin("net.leifwarner" % "sbt-git-info" % "0.1-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.0-RC2")
