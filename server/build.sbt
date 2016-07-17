name := "MyScalaBlog"

version := "0.0"

scalaVersion  := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  val json4sV = "3.3.0"
  Seq(
    "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
    "io.spray"            %%   "spray-can"     % sprayV,
    "io.spray"            %%   "spray-routing" % sprayV,
    "io.spray"            %%   "spray-http"    % sprayV,
    "io.spray"            %%   "spray-util"    % sprayV,
    "io.spray"            %%   "spray-httpx"   % sprayV,
    "io.spray"            %%  "spray-json"    % "1.2.6",
    "io.spray"            %%  "spray-client"   % sprayV,
    "io.spray"            %%   "spray-testkit" % sprayV  % "test",
    "com.github.nscala-time" %% "nscala-time" % "1.4.0",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "org.json4s"          %% "json4s-native"  % json4sV,
    "org.json4s"          %% "json4s-jackson" % json4sV,
    "org.json4s"          %% "json4s-ext"     % json4sV
  )
}

lazy val `blog-server` = project.in(file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "server"
  )

buildInfoOptions += BuildInfoOption.BuildTime
buildInfoOptions += BuildInfoOption.ToJson

buildInfoKeys ++= Seq[BuildInfoKey] (
  BuildInfoKey.action("commitHash") {
    Process("git rev-parse HEAD").lines.head
  }
)

resolvers ++= Seq("spray" at "http://repo.spray.io/")

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

resolvers ++= Seq("Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

resolvers ++= Seq("Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/")

assemblyJarName in assembly := "my-scala-blog.jar"

Revolver.settings
