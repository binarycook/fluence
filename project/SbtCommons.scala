import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headerLicense
import de.heikoseeberger.sbtheader.License
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{javaOptions, _}
import sbt.Scoped.AnyInitTask
import sbt.internal.util.ManagedLogger
import java.io.File
import sbt.{Def, addCompilerPlugin, taskKey, _}
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}

import scala.sys.process._

object SbtCommons {

  val scalaV = scalaVersion := "2.12.9"

  val kindProjector = Seq(
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )

  val commons = Seq(
    scalaV,
    version                              := "0.3.0",
    fork in Test                         := true,
    parallelExecution in Test            := false,
    fork in IntegrationTest              := true,
    parallelExecution in IntegrationTest := false,
    organizationName                     := "Fluence Labs Limited",
    organizationHomepage                 := Some(new URL("https://fluence.network")),
    startYear                            := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    headerLicense := Some(License.ALv2("2018", organizationName.value)),
    resolvers += Resolver.bintrayRepo("fluencelabs", "releases"),
    scalafmtOnCompile := true,
    // see good explanation https://gist.github.com/djspiewak/7a81a395c461fd3a09a6941d4cd040f2
    scalacOptions ++= Seq("-Ypartial-unification", "-deprecation"),
    javaOptions in Test ++= Seq(
      "-XX:MaxMetaspaceSize=4G",
      "-Xms4G",
      "-Xmx4G",
      "-Xss6M"
    ),
    javaOptions in IntegrationTest ++= Seq(
      "-XX:MaxMetaspaceSize=4G",
      "-Xms4G",
      "-Xmx4G",
      "-Xss6M"
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
  ) ++ kindProjector

  class LinuxNativeMergeStrategy extends MergeStrategy{
    override def name: String = "Rename native/linux_x86_64 to native/x86_64-linux"

    override def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
      Right(files.map(_ -> "native/x86_64-linux/libfrank.so"))
    }
  }

  val mergeStrategy = Def.setting[String => MergeStrategy]({
    // a module definition fails compilation for java 8, just skip it
    case PathList("module-info.class", xs @ _*)  => MergeStrategy.first
    case "META-INF/io.netty.versions.properties" => MergeStrategy.first
    case PathList("native", "linux_x86_64", "libfrank.so") => new LinuxNativeMergeStrategy()
    case x =>
      import sbtassembly.AssemblyPlugin.autoImport.assembly
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }: String => MergeStrategy)

  val docker = taskKey[Unit]("Build docker image")

  private val buildContract = Def.task {
    val log = streams.value.log
    log.info(s"Generating java wrapper for smart contract")

    def run(cmd: String) = {
      val projectRoot = file("").getAbsolutePath
      val bootstrapFolder = file(s"$projectRoot/bootstrap")
      log.info(s"running $cmd in $bootstrapFolder")

      assert(
        Process(cmd, cwd = bootstrapFolder).! == 0,
        "Generating java wrapper or contract compilation failed"
      )
    }

    run("npm install")
    run("npm run generate-all")
  }

  def buildContractBeforeDocker(): Seq[Def.Setting[_]] =
    Seq(
      docker         := docker.dependsOn(buildContract).value,
      docker in Test := (docker in Test).dependsOn(buildContract).value
    )

  // Useful – unlike cmd.!! redirects both stdout & stderr to console
  def runCmd(cmd: String): Unit = {
    import scala.sys.process._

    val code = cmd.!
    if (code != 0) {
      throw new RuntimeException(s"Command $cmd exited: $code")
    }
  }

  def itDepends[T](task: TaskKey[T])(on: AnyInitTask*)(configs: Configuration*): Seq[Def.Setting[Task[T]]] =
    configs.map(c ⇒ (task in c) := (task in c).dependsOn(on: _*).value)

  def itDepends[T](task: InputKey[T])(on: AnyInitTask*)(configs: Configuration*): Seq[Def.Setting[InputTask[T]]] =
    configs.map(c ⇒ (task in c) := (task in c).dependsOn(on: _*).evaluated)

  /* Common deps */

  val catsVersion = "2.0.0"
  val cats = "org.typelevel" %% "cats-core" % catsVersion
  val catsEffectVersion = "2.0.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  val fs2Version = "1.0.4"
  val fs2 = "co.fs2"   %% "fs2-core"             % fs2Version
  val fs2rx = "co.fs2" %% "fs2-reactive-streams" % fs2Version
  val fs2io = "co.fs2" %% "fs2-io"               % fs2Version

  // functional wrapper around 'lightbend/config'
  val ficus = "com.iheart" %% "ficus" % "1.4.5"

  val cryptoVersion = "0.0.9"
  val cryptoHashsign = "one.fluence" %% "crypto-hashsign" % cryptoVersion

  val codecVersion = "0.0.5"
  val codecCore = "one.fluence" %% "codec-core" % codecVersion

  val frank = "frank" %% "frank" % "0.1.1"

  val sttpVersion = "1.6.3"
  val sttp = "com.softwaremill.sttp"            %% "core"                           % sttpVersion
  val sttpCirce = "com.softwaremill.sttp"       %% "circe"                          % sttpVersion
  val sttpFs2Backend = "com.softwaremill.sttp"  %% "async-http-client-backend-fs2"  % sttpVersion
  val sttpCatsBackend = "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpVersion

  val http4sVersion = "0.20.10"
  val http4sDsl = "org.http4s"    %% "http4s-dsl"          % http4sVersion
  val http4sServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  val http4sCirce = "org.http4s"  %% "http4s-circe"        % http4sVersion

  val circeVersion = "0.12.1"
  val circeCore = "io.circe"          %% "circe-core"           % circeVersion
  val circeGeneric = "io.circe"       %% "circe-generic"        % circeVersion
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
  val circeParser = "io.circe"        %% "circe-parser"         % circeVersion
  val circeFs2 = "io.circe"           %% "circe-fs2"            % "0.11.0"

  val scodecBits = "org.scodec" %% "scodec-bits" % "1.1.9"
  val scodecCore = "org.scodec" %% "scodec-core" % "1.11.3"

  val web3jVersion = "4.5.5"
  val web3jCrypto = "org.web3j" % "crypto" % web3jVersion
  val web3jCore = "org.web3j"   % "core"   % web3jVersion

  val toml = "com.electronwill.night-config" % "toml" % "3.4.2"

  val rocksDb = "org.rocksdb"      % "rocksdbjni" % "5.17.2"
  val levelDb = "org.iq80.leveldb" % "leveldb"    % "0.12"

  val protobuf = "io.github.scalapb-json"  %% "scalapb-circe"     % "0.4.3"
  val protobufUtil = "com.google.protobuf" % "protobuf-java-util" % "3.7.1"

  val bouncyCastle = "org.bouncycastle" % "bcprov-jdk15on" % "1.61"

  val asyncHttpClient = "org.asynchttpclient" % "async-http-client" % "2.8.1"

  /* Test deps*/
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.8"     % Test
  val catsTestkit = "org.typelevel"                      %% "cats-testkit"              % catsVersion % Test
  val disciplineScalaTest = "org.typelevel"              %% "discipline-scalatest"      % "1.0.0-M1"  % Test

  val scalaTestCompile = "org.scalatest"     %% "scalatest" % "3.0.8"
  val scalaTest = scalaTestCompile           % Test
  val scalaIntegrationTest = "org.scalatest" %% "scalatest" % "3.0.8" % IntegrationTest
  val mockito = "org.mockito"                % "mockito-core" % "2.21.0" % Test
}
