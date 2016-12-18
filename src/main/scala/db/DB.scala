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

  // there are issues in squeryl with renaming of columns ("named"). if a foreign key does not work, use uppercase!
  on(tracks)(t => declare(
    t.id.is(named("_id"))
  ))
  on(trackPoints)(t => declare(
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
