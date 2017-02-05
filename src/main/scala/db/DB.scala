package db

import framework.Logging
import main.Settings

import org.squeryl._
import org.squeryl.adapters.SQLiteAdapter
import scala.util.Try
import java.text.SimpleDateFormat
import java.util.Date


class Track(var id: Long = 0,
            var trackname: Option[String],
            var trackfechaini: Option[Long], // start unix time
            var trackfolder: Option[String]
           ) extends KeyedEntity[Long] with Logging {
  def getTimeString: String = {
    val sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
    Try( sdf.format(new Date(trackfechaini.getOrElse(0L))) ).getOrElse("date error")
  }
  override def toString: String = s"[$id][${trackfolder.getOrElse("null")}] ${trackname.getOrElse("null")} [$getTimeString]"
}

class TrackPoint(var id: Long = 0,
                 var trkptlat: Option[Double],
                 var trkptlon: Option[Double],
                 var trkptalt: Option[Double],
                 var trkpttime: Option[Long],
                 var trkptseg: Option[Long]
                ) extends KeyedEntity[Long] {
}

class Segment(var id: Long = 0,
              var segname: Option[String],
              var segdescr: Option[String],
              var segfechaini: Option[Long],
              var segfechafin: Option[Long],
              var segtimeup: Option[Long],
              var segtimedown: Option[Long],
              var segmaxalt: Option[Double],
              var segminalt: Option[Double],
              var segavgspeed: Option[Double],
              var segupalt: Option[Double],
              var segdownalt: Option[Double],
              var segdist: Option[Double],
              var segtimemov: Option[Long],
              var segtrack: Option[Long],
              var segmaxspeed: Option[Double],
              var segcolor: Option[Long],
              var segstroke: Option[Double],
              var segfill: Option[Int],
              var segfillColor: Option[Int]
             ) extends KeyedEntity[Long] {
}

object SquerylEntrypointForMyApp extends PrimitiveTypeMode {
  // http://squeryl.org/0.9.6.html
  // use then:
  // import SquerylEntrypointForMyApp._

//  implicit object Track extends KeyedEntityDef[Track, Int] {
//    def getId(a: Track) = a.id
//    def isPersisted(a: Track) = a.id > 0
//    def idPropertyName = "_id"
//  }
}
import db.SquerylEntrypointForMyApp._

object DB extends Schema with Logging {

  val lastschemaversion = 3

  info("Initializing oruxmaps DB...")

  val tracks: Table[Track] = table[Track]("tracks")
  val trackPoints: Table[TrackPoint] = table[TrackPoint]("trackpoints")
  val segments: Table[Segment] = table[Segment]("segments")

  // there are issues in squeryl with renaming of columns ("named"). if a foreign key does not work, use uppercase!
  on(tracks)(t => declare(
    t.id.is(named("_id"))
  ))
  on(trackPoints)(t => declare(
    t.id.is(named("_id"))
  ))
  on(segments)(t => declare(
    t.id.is(named("_id"))
  ))

  def initialize() {
    terminate()
    val dbp = Settings.prefs.get(Settings.DBPATH, "")
    info("Loading database at " + dbp + " ...")
    val dbs = s"jdbc:sqlite:$dbp"

    Class.forName("org.sqlite.JDBC")
    SessionFactory.concreteFactory = Some(() => Session.create(java.sql.DriverManager.getConnection(dbs), new SQLiteAdapter()))

    transaction {
      DB.printDdl
      debug("track count: " + tracks.Count.toLong)
    }
    info("Database loaded!")
  }

  def terminate(): Unit = {
    Session.currentSessionOption.foreach(s => s.close)
  }

}
