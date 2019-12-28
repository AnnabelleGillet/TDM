import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "tdm"
ThisBuild / organizationName := "tdm"

lazy val root = (project in file("."))
  .settings(
    name := "TDM",
    libraryDependencies += scalaTest % Test
  )

// Shapeless
resolvers ++= Seq(
	Resolver.sonatypeRepo("releases"),
	Resolver.sonatypeRepo("snapshots")
)

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"// % Test

// Postgres embedded
libraryDependencies += "ru.yandex.qatools.embed" % "postgresql-embedded" % "2.10" % Test
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5" % Test

// Coveralls

import org.scoverage.coveralls.Imports.CoverallsKeys._
coverallsToken := Some(sys.env("COVERALLS_TOKEN"))