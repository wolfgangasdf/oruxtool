
import java.time.ZonedDateTime

name := "oruxtool"
organization := "com.oruxtool"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "UTF-8")

resolvers ++= Seq(
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"
)
val geotoolsversion = "13.2"
libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "org.squeryl" %% "squeryl" % "0.9.6-RC4" withSources() withJavadoc(),
  "org.scalaj" %% "scalaj-http" % "1.1.6",
  "org.xerial" % "sqlite-jdbc" % "3.8.7",
  "org.geotools" % "gt-shapefile" % geotoolsversion,
  "org.geotools" % "gt-image" % geotoolsversion,
  "org.geotools" % "gt-shapefile" % geotoolsversion,
  "org.geotools" % "gt-geotiff" % geotoolsversion
  //"org.geotools" % "gt-epsg-hsql" % geotoolsversion
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion,
      BuildInfoKey.action("buildTime") { ZonedDateTime.now.toString }
    ),
    buildInfoPackage := "buildinfo",
    buildInfoUsePackageAsPath := true
  )

////////////////// sbt-javafx for packaging
jfxSettings
JFX.verbose := true
JFX.mainClass := Some("main.Main")
JFX.devKit := JFX.jdk(System.getenv("JAVA_HOME"))
JFX.pkgResourcesDir := baseDirectory.value + "/src/deploy"
JFX.artifactBaseNameValue := "oruxtool"

/////////////// mac app bundle via sbt-appbundle
Seq(appbundle.settings: _*)
appbundle.name := "Oruxtool"
appbundle.javaVersion := "1.8*"
appbundle.icon := Some(file("src/deploy/macosx/icon.icns"))
appbundle.mainClass := JFX.mainClass.value
appbundle.executable := file("src/deploy/macosx/universalJavaApplicationStub")


/////////////// task to zip the jar for win,linux
lazy val tzip = TaskKey[Unit]("zip")
tzip := {
  println("packaging...")
  JFX.packageJavaFx.value
  println("zipping jar & libs...")
  val s = target.value + "/" + JFX.artifactBaseNameValue.value + "-win-linux.zip"
  IO.zip(
    Path.allSubpaths(new File(crossTarget.value + "/" + JFX.artifactBaseNameValue.value)).
      filterNot(_._2.endsWith(".html")).filterNot(_._2.endsWith(".jnlp")), new File(s))
  println("==> created windows & linux zip: " + s)
}

/////////////// task to zip the mac app bundle
lazy val tzipmac = TaskKey[Unit]("zipmac")
tzipmac := {
  println("making app bundle...")
  appbundle.appbundle.value
  println("zipping mac app bundle...")
  val zf = new File(target.value + "/" + appbundle.name.value + "-mac.zip")
  val bd = new File(target.value + "/" + appbundle.name.value + ".app")
  def entries(f: File):List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries) else Nil)
  IO.zip(entries(bd).map(d => (d, d.getAbsolutePath.substring(bd.getParent.length))), zf)
  println("==> created mac app zip: " + zf)
}

/////////////// task to do all at once
lazy val tdist = TaskKey[Unit]("dist")
tdist := {
  tzipmac.value
  tzip.value
  println("Created Sgar distribution files!")
}
