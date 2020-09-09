import sbt._
import scala.sys.process._
import java.io.File

ThisBuild / organization := "datasketches"
ThisBuild / name := "sandbox"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / useSuperShell := false

libraryDependencies ++= Seq(
  Dependencies.catsCore.value,
  Dependencies.catsEffect.value,
  Dependencies.circe.value,
  Dependencies.dataSketches.value,
  Dependencies.mapRef.value,
  Dependencies.miniTest.value,
  Dependencies.miniTestLaws.value,
  Dependencies.http4sBlazeServer.value,
  Dependencies.http4sBlazeClient.value,
  Dependencies.http4sCirce.value,
  Dependencies.http4sDsl.value,
  Dependencies.logback.value,
  Dependencies.janino.value
)

testFrameworks += new TestFramework("minitest.runner.Framework")

lazy val nativeImageLocal =
  taskKey[File](
    "Build a standalone executable on this machine using GraalVM Native Image"
  )

nativeImageLocal := {
  import sbt.Keys.streams
  val assemblyFatJar = assembly.value
  val assemblyFatJarPath = assemblyFatJar.getAbsolutePath()
  val outputName = "ds-sandbox-server.local"
  val outputPath = (baseDirectory.value / "out" / outputName).getAbsolutePath()

  val cmd = s"""native-image
     | -jar ${assemblyFatJarPath}
     | ${outputPath}""".stripMargin.filter(_ != '\n')

  val log = streams.value.log
  log.info(s"Building local native image from ${assemblyFatJarPath}")
  log.debug(cmd)
  val result = (cmd.!(log))

  if (result == 0) file(s"${outputPath}")
  else {
    log.error(s"Local native image command failed:\n ${cmd}")
    throw new Exception("Local native image command failed")
  }
}

lazy val nativeImage =
  taskKey[File](
    "Build a standalone Linux executable using GraalVM Native Image"
  )

nativeImage := {
  import sbt.Keys.streams
  val assemblyFatJar = assembly.value
  val assemblyFatJarPath = assemblyFatJar.getParent()
  val assemblyFatJarName = assemblyFatJar.getName()
  val outputPath = (baseDirectory.value / "out").getAbsolutePath()
  val outputName = "ds-sandbox-server"
  val nativeImageDocker = "datasketches-sandbox/graalvm-native-image"

  val cmd = s"""docker run
     | --volume ${assemblyFatJarPath}:/opt/assembly
     | --volume ${outputPath}:/opt/native-image
     | ${nativeImageDocker}
     | --static
     | -jar /opt/assembly/${assemblyFatJarName}
     | ${outputName}""".stripMargin.filter(_ != '\n')

  val log = streams.value.log
  log.info(s"Building native image from ${assemblyFatJarName}")
  log.debug(cmd)
  val result = (cmd.!(log))

  if (result == 0) file(s"${outputPath}/${outputName}")
  else {
    log.error(s"Native image command failed:\n ${cmd}")
    throw new Exception("Native image command failed")
  }
}
