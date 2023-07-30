ThisBuild / scalaVersion     := "2.12.16"
ThisBuild / version          := "0.5.0"
ThisBuild / organization     := "tdm"
ThisBuild / organizationName := "tdm"

val sparkVersion = "3.4.1"

lazy val root = (project in file("."))
  .settings(
    name := "TDM",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
  )
  
test in assembly := {}
assemblyExcludedJars in assembly := {
	val cp = (fullClasspath in assembly).value
	cp filter {_.data.getName != "MuLOT-distributed-assembly-0.5.jar"}
}

// Shapeless
resolvers ++= Seq(
	Resolver.sonatypeRepo("releases"),
	Resolver.sonatypeRepo("snapshots")
)

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

// Postgres embedded
libraryDependencies += "ru.yandex.qatools.embed" % "postgresql-embedded" % "2.10" % Test
libraryDependencies += "org.postgresql" % "postgresql" % "42.6.0" % Test

// Breeze
libraryDependencies += "org.scalanlp" %% "breeze" % "1.1"
libraryDependencies += "org.scalanlp" %% "breeze-natives" % "1.1"

// Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"

// Coveralls

import org.scoverage.coveralls.Imports.CoverallsKeys._
coverallsToken := Some(sys.env.get("COVERALLS_TOKEN").getOrElse(""))

