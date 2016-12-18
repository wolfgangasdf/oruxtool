# Oruxtool

is a tool to access the [oruxmaps](http://oruxmaps.com) database. It has currently the following functionalities:

* Uses Java 8 and runs on Mac, Windows, Linux.
* select a track and display with background map TODO
* select many tracks and visualize nicely TODO
* browse by oxusmaps folders TODO
* move tracks around folders

Import / export possibilities:

* mass GPX/KML/KMZ export TODO

### How to use ###

* Get Java JRE >= 8u101
* [Download the zip](https://bitbucket.org/wolfgang/oruxtool/downloads) for Mac or (Windows, Linux), extract it somewhere and double-click the app (Mac) or
  jar file (Windows, Linux).
* copy the oruxmaps database (usually `internal sdcard / oruxmaps / tracklogs / oruxmapstracks.db`) to your computer and open it in oruxtool.

Everything should be self-explanatory (watch out for tooltips).

### How to develop, compile & package ###

* Get Java JDK >= 8u101
* check out the code (`hg clone ...` or download a zip) 
* I use the free community version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) with the scala 
plugin for development, just import the project to get started. 

Run Reftool from terminal and package it:

* Install the [Scala Build Tool](http://www.scala-sbt.org/)
* Compile and run manually: `sbt run`
* Package for all platforms: `sbt dist`. The resulting files are in `target/`

### Suggestions, bug reports, pull requests, contact ###
Please use the bitbucket-provided tools for bug reports and contributed code. Anything is welcome!

contact: wolfgang.loeffler@gmail.com

### Used technologies ###

* [Scala](http://www.scala-lang.org) and [Scala Build Tool](http://www.scala-sbt.org)
* [Scalafx](http://scalafx.org) as wrapper for [JavaFX](http://docs.oracle.com/javafx) for the graphical user interface
* [Squeryl](http://squeryl.org) as database ORM & DSL, using [Apache Derby](http://db.apache.org/derby) embedded as backend
* [sbt-javafx](https://github.com/kavedaa/sbt-javafx) to make the runnable Reftool jar file
* [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) to access build information
* [sbt-appbundle](https://github.com/Sciss/sbt-appbundle) to make the mac app bundle
* a modified version of [universalJavaApplicationStub](https://github.com/tofi86/universalJavaApplicationStub) to launch on Mac

### License ###
[GPL](https://www.gnu.org/licenses/gpl.html)