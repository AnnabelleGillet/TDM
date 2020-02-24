import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.2.0"
ThisBuild / organization     := "tdm"
ThisBuild / organizationName := "tdm"

val sparkVersion = "2.4.4"

lazy val root = (project in file("."))
  .settings(
    name := "TDM",
    libraryDependencies += scalaTest % Test
  )
  
test in assembly := {}  

// Shapeless
resolvers ++= Seq(
	Resolver.sonatypeRepo("releases"),
	Resolver.sonatypeRepo("snapshots")
)

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

// Postgres embedded
libraryDependencies += "ru.yandex.qatools.embed" % "postgresql-embedded" % "2.10" % Test
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5" % Test

// Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"

// Coveralls

import org.scoverage.coveralls.Imports.CoverallsKeys._
coverallsToken := Some(sys.env.get("COVERALLS_TOKEN").getOrElse(""))

