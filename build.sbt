libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play" % "2.6.7",
  ws,
  "org.slf4j" % "log4j-over-slf4j" % "1.7.21",
  "net.java.dev.jna" % "jna" % "4.5.1",
  "net.java.dev.jna" % "jna-platform" % "4.5.1" 
)

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Maven Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases/",
  "GridGrain" at "http://www.gridgainsystems.com/nexus/content/repositories/external/",
  "Clojars" at "http://clojars.org/repo/" 
)

val main = Project("chloe", file("."))
  .enablePlugins(PlayScala)
  .settings(
    organization := "net.chloe",
    scalaVersion := "2.11.8",
    EclipseKeys.withJavadoc := false,
    EclipseKeys.withSource := true
  )