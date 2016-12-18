package db

import framework.Logging
import main.Settings

import org.squeryl._
import org.squeryl.adapters.SQLiteAdapter
import scala.util.Try
import java.text.SimpleDateFormat
import java.util.Date


class Track(var id: Int = 0,
            var trackname: String = "",
            var trackfechaini: Long = 0, // start unix time
            var trackfolder: String = ""
           ) extends KeyedEntity[Int] with Logging {
  def getTimeString: String = {
    val sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
    Try( sdf.format(new Date(trackfechaini)) ).getOrElse("date error")
  }
  override def toString: String = s"[$id][$trackfolder] $trackname [$getTimeString]"
}

class TrackPoint(var id: Int = 0,
                 var trkptlat: Double,
                 var trkptlon: Double,
                 var trkptalt: Double,
                 var trkpttime: Int,
                 var trkptseg: Int
                ) extends KeyedEntity[Int] {
}

class Segment(var id: Int = 0,
              var segname: String = "",
              var segdescr: String = "",
              var segfechaini: Int = 0,
              var segfechafin: Int = 0,
              var segtimeup: Int = 0,
              var segtimedown: Int = 0,
              var segmaxalt: Double = 0,
              var segminalt: Double = 0,
              var segavgspeed: Double = 0,
              var segupalt: Double = 0,
              var segdownalt: Double = 0,
              var segdist: Double = 0,
              var segtimemov: Int = 0,
              var segtrack: Int = 0,
              var segmaxspeed: Double = 0,
              var segcolor: Int = 0,
              var segstroke: Double = 0,
              var segfill: Int = 0,
              var segfillColor: Int = 0
             ) extends KeyedEntity[Int] {
}

//class Topic2Article(val TOPIC: Long, val ARTICLE: Long, var color: Int) extends KeyedEntity[CompositeKey2[Long, Long]] {
//   def id = compositeKey(TOPIC, ARTICLE)
//}

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

    info("Loading database at " + Settings.dbpath + " ...")
    val dbs = s"jdbc:sqlite:${Settings.dbpath}"

    Class.forName("org.sqlite.JDBC")

    SessionFactory.concreteFactory = Some(() => Session.create(java.sql.DriverManager.getConnection(dbs), new SQLiteAdapter()))

    transaction {
      DB.printDdl
      debug("track count: " + tracks.Count.toLong)
    }
    info("Database loaded!")
  }


}
