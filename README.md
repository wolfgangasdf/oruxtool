# Oruxtool

Is a tool to access the [oruxmaps](http://oruxmaps.com) database. It can't do very much currently, I made it public due to the lack of any program like this. Current functionalities:

* Runs on Mac, Windows, Linux.
* Browse by oruxmaps track-folder (or show all)
* Load a [geotiff](http://trac.osgeo.org/geotiff/) background map (see below)
* Draw a track (or all) on the map

For now, it can't do any database manipulation tasks, but this would not be hard to implement, e.g., mass folder management.

Import / export possibilities:

* Export selected/all tracks to a GPX file.

### How to use ###

* [Download the zip](https://github.com/wolfgangasdf/oruxtool/releases) for Mac or (Windows, Linux), extract it somewhere and double-click the app (Mac) or
  jar file (Windows, Linux).
* copy the oruxmaps database (usually `internal sdcard / oruxmaps / tracklogs / oruxmapstracks.db`) to your computer and open it in oruxtool.

Everything should be self-explanatory (watch out for tooltips).

### How to develop, compile & package ###

* Get Java 13 from https://jdk.java.net
* Clone the repository
* I use the free community version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) with the scala 
plugin for development, just import the project to get started. 

Packaging:

* Download JDKs for the other platforms (and/or adapt `cPlatforms` in `build.gradle.kts`), extract them and set the environment variables to it:
  * `export JDK_MAC_HOME=...`, `export JDK_WIN_HOME=...`, `export JDK_LINUX_HOME=...`
* Package for all platforms: `./gradlew clean dist`. The resulting files are in `build/crosspackage`

### Suggestions, bug reports, pull requests, contact ###
Please use the provided tools for bug reports and contributed code. Anything is welcome!

### Background map images ###

* [Landsat 8](http://landsatlook.usgs.gov/viewer.html) made very nice images. Use an `image mosaic`, export geotiff.
Format `geographic (wgs84)` is important to get lat/lon, otherwise it's in UTM.

### Used technologies ###

* [Scala](http://www.scala-lang.org) and [Scala Build Tool](http://www.scala-sbt.org)
* [Scalafx](http://scalafx.org) as wrapper for [JavaFX](http://docs.oracle.com/javafx) for the graphical user interface
* [Squeryl](http://squeryl.org) as database ORM & DSL, using [SQLite](https://www.sqlite.org) embedded as backend
* [sbt-javafx](https://github.com/kavedaa/sbt-javafx) to make the runnable Reftool jar file
* [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) to access build information
* [sbt-appbundle](https://github.com/Sciss/sbt-appbundle) to make the mac app bundle
* a modified version of [universalJavaApplicationStub](https://github.com/tofi86/universalJavaApplicationStub) to launch on Mac
* [JPX](https://github.com/jenetics/jpx) for GPX export

### License ###
[GPL](https://www.gnu.org/licenses/gpl.html)