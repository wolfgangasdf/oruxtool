package main

import java.awt.Taskbar
import io.jenetics.jpx

import java.io
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.prefs.Preferences
import javafx.concurrent.Task

import javax.imageio.ImageIO
import db.SquerylEntrypointForMyApp._
import db.{DB, Track, TrackPoint}
import framework.Helpers._
import framework.{Helpers, Logging, MyWorker}
import org.geotools.api.geometry.Bounds
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.geotools.geometry.Position2D
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.util.factory.Hints
import util._
import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.embed.swing.SwingFXUtils
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation, Point2D, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.Button._
import scalafx.scene.control.Label._
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.scene.input.{MouseEvent, ScrollEvent}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.stage.{FileChooser, WindowEvent}

import java.io.File


object Settings {
  val prefs: Preferences = Preferences.userRoot().node("oruxtool")
  val DBPATH = "lastdbpath"
  val IMGPATH = "lastimgpath"
  val EXPORTFILEPATH = "exportfilepath"
  val MINPOINTDIST = 3 // draw points not closer than this distance
  val MAXDISTANCE = 0.03 // max. distance between points to draw connected
}

class ScrollPanImageView extends ScrollPane {
  var img: Image = _ // this hosts the image
  var bgimage: WritableImage = _ // this writes onto img.

  val imageView: ImageView = new ImageView {
    preserveRatio = true
    tooltip = new Tooltip("zoom: shift+scroll")
  }

  def setImage(image: Image): Unit = {
    img = image
    bgimage = new WritableImage(img.pixelReader.get, img.width.value.toInt, img.height.value.toInt)
    imageView.image = bgimage
  }

  onMouseClicked = (me: MouseEvent) => {
    val posInImg = imageView.sceneToLocal(new Point2D(me.x, me.y)).multiply(1.0/zoomval)
    println(s"pos in img: $posInImg zoom=$zoomval posinimgz=${posInImg.multiply(zoomval)} me=${new Point2D(me.x, me.y)}")
    println(s"NEW: ${getPosInPic(me.x, me.y)} orig: ${getPosInPic(me.x, me.y).multiply(1.0/zoomval)}")
  }

  def getPosInPic(x: Double, y: Double): Point2D = { // imageView.sceneToLocal gives nonsense (sometimes)!
    var px = (imageView.getFitWidth - this.getWidth) * this.getHvalue + x
    var py = (imageView.getFitHeight - this.getHeight) * this.getVvalue + y
    if (px < 0) px = x // Hvalue is 1.0 if no scroll bar needed, bug IMO
    if (py < 0) py = y
    new Point2D(px, py)
  }

  var zoomval = 1.0
  filterEvent(ScrollEvent.Any) {
    se: ScrollEvent =>
      if (se.shiftDown) { // shift scroll for zoom around mouse cursor!
        // remember old position & zoom
        val posInImg = getPosInPic(se.x, se.y).multiply(1.0/zoomval)
        println(s"se: ${new Point2D(se.x, se.y)}")
        println(s"sce2loc1: $posInImg posinimgz=${posInImg.multiply(zoomval)}") // gives where 0,0 of image is!
        zoomval *= (if (se.deltaY > 0) 1.1 else 0.9091)
        imageView.fitWidth = img.width.value * zoomval
        imageView.fitHeight = img.height.value * zoomval
        // scroll to old position
        val newx = posInImg.getX * zoomval // new position where mouse should be in zoomed pic
        val scrollx = (newx - se.x) / (imageView.getFitWidth - this.getWidth)
        this.setHvalue(scrollx)
        val newy = posInImg.getY * zoomval
        val scrolly = (newy - se.y) / (imageView.getFitHeight - this.getHeight)
        this.setVvalue(scrolly)
      }
  }
  content = imageView
}


class MainScene extends Scene with Logging {

  val tracks = new ObservableBuffer[db.Track]()
  val folders = new ObservableBuffer[String]()
  var imgOrig: Image = _

  val ALLFOLDERS = "### all ###"
  val NOFOLDER = "### no folder ###" // --- or null, see below

  private def createMenuBar = new MenuBar {
    useSystemMenuBar = true
    menus = List(
      new Menu("Help") {
        items = List(
          new MenuItem("About") {
            onAction = (_: ActionEvent) => {
              new Alert(AlertType.Information, "", ButtonType.Close) {
                title = "About Oruxtool"
                headerText = "Oruxtool - oruxmaps track database tool"
                dialogPane.value.content = new VBox {
                  padding = Insets(15)
                  spacing = 15
                  children ++= Seq(
                    new Button("Open Oruxtool homepage") {
                      onAction = (_: ActionEvent) =>
                        FileHelper.openURL("https://github.com/wolfgangasdf/oruxtool")
                    }
                  )
                }
              }.showAndWait()
            }
          }
        )
      }
    )
  }

  val spiv = new ScrollPanImageView


  //////////////////////////

  def drawpoint(x0: Int, y0: Int): Unit = { // a smooth point
    val pw = spiv.bgimage.getPixelWriter
    val pr = spiv.bgimage.getPixelReader
    val rr = 15 // radius
    if (x0 > rr && y0 > rr && x0 < spiv.bgimage.width.value-rr && y0 < spiv.bgimage.height.value-rr) {
      for (x <- -rr to rr) {
        for (y <- -rr to rr) {
          val c = pr.getColor(x0+x, y0+y)
          val rn = math.sqrt(x*x+y*y)
          val op = 0.3*math.exp(-rn*rn/math.pow(0.4*rr, 2.0)) // adapt factor if mindist changed!
          val c1 = c.interpolate(Color.rgb(0, 255, 0), op)
          pw.setColor(x0+x, y0+y, c1)
        }
      }
    }
  }

  var lastdrawnpoint: (Int, Int) = (0, 0)
  def lineTo(x: Int, y: Int): Unit = {
    val dist = math.sqrt(math.pow(x-lastdrawnpoint._1, 2) + math.pow(y-lastdrawnpoint._2, 2)).toInt
    val ang = math.atan2(y-lastdrawnpoint._2, x-lastdrawnpoint._1)
    if (dist > Settings.MINPOINTDIST) {
      for (rr <- Settings.MINPOINTDIST to dist by Settings.MINPOINTDIST) {
        drawpoint((lastdrawnpoint._1 + rr * math.cos(ang)).toInt, (lastdrawnpoint._2 + rr * math.sin(ang)).toInt)
      }
      lastdrawnpoint = (x, y)
    }
  }

  def plotTrackPoints2(tps: List[TrackPoint]): Unit = {
    var olddp: Position2D = null
    tps.foreach( tp => {
      val dp = new Position2D(tp.trkptlon.getOrElse(0), tp.trkptlat.getOrElse(0))
      val dist = if (olddp != null) dp.distance(olddp) else 0
      val dpgrid = coverage.getGridGeometry.worldToGrid(dp)
      val (rx, ry) = (dpgrid.getCoordinateValue(0), dpgrid.getCoordinateValue(1))
      if (olddp == null || dist > Settings.MAXDISTANCE) {
        drawpoint(rx, ry)
        lastdrawnpoint = (rx, ry)
      } else {
        lineTo(rx, ry)
      }
      olddp = dp
    })
  }

  def loadTrack(tt: db.Track): Unit = {
    if (tt != null) {
      debug("Loading track: " + tt)
      transaction {
        val segids = from(DB.segments)(s => where(s.segtrack === tt.id) select s.id).toList
        val trkpts = from(DB.trackPoints)(tp => where( tp.trkptseg in segids ) select tp orderBy tp.trkpttime)
        debug("  loaded #trkps : " + trkpts.size)
        plotTrackPoints2(trkpts.toList)
      }
    }
  }

  val tracksList: ListView[Track] = new ListView[db.Track](tracks) {
    prefWidth = 50.0
    selectionModel().selectedItem.onChange { (_, _, tt) => loadTrack(tt) }
  }

  private val sph = new SplitPane {
    orientation = Orientation.Horizontal
    dividerPositions = 0.15
    items.addAll(tracksList, spiv)
  }

  private val statusBarLabel = new Label("") {
    hgrow = Priority.Always
  }
  private val statusbar = new VBox {
    children += statusBarLabel
  }

  val folderActionDisabled = new AtomicBoolean(false)
  private val cbFolders = new ChoiceBox[String] {
    items.setValue(folders)
    onAction = (_: ActionEvent) => {
      if (!folderActionDisabled.get) new MyWorker[Unit]("Load tracks db", loadTracksDB).runInBackground()
    }
  }
  val cbSelectedOnly = new CheckBox("selected tracks only:")
  val menuBar: MenuBar = createMenuBar
  private val statusBar = new HBox {
    alignment = Pos.CenterLeft
    children += new Button("Open track db") {
      onAction = (_: ActionEvent) => {
        val fc = new FileChooser() {
          title = "Open oruxmaps.db"
        }
        val inidir = new java.io.File(Settings.prefs.get(Settings.DBPATH, "/")).getParentFile
        if (inidir != null && inidir.isDirectory) fc.setInitialDirectory(inidir)
        val res: java.io.File = fc.showOpenDialog(scene.value.getWindow)
        val ff = new java.io.File(res.getPath)
        if (ff != null && ff.canRead) {
          Settings.prefs.put(Settings.DBPATH, res.toString)
          new MyWorker[Unit]("Load tracks db", loadTracksDB).runInBackground()
        }
      }
    }
    children += new Label("Folder:")
    children += cbFolders
    children += new Button("Open geotiff") {
      onAction = (_: ActionEvent) => {
        val fc = new FileChooser() {
          title = "Open geotiff background image"
        }
        val inidir = new java.io.File(Settings.prefs.get(Settings.IMGPATH, "/")).getParentFile
        if (inidir != null && inidir.isDirectory) fc.setInitialDirectory(inidir)
        val res: java.io.File = fc.showOpenDialog(scene.value.getWindow)
        val ff = new java.io.File(res.getPath)
        if (ff != null && ff.canRead) {
          Settings.prefs.put(Settings.IMGPATH, res.toString)
          new MyWorker[Unit]("Load image", loadImage).runInBackground()
        }
      }
    }
    children += new Button("Draw all tracks") {
      onAction = (_: ActionEvent) => {
        val task = new Task[Unit] {
          override def call(): Unit = {
            tracks.indices.foreach(iii => {
              updateProgress(iii, tracks.length)
              runUIwait {
                loadTrack(tracks(iii))
              }
              Thread.sleep(10)
              if (isCancelled) throw new InterruptedException("interrupted!")
            })
          }
        }
        new MyWorker("Draw all tracks", task).runInBackground()
      }
    }
    children += new Button("Save pic") {
      onAction = (_: ActionEvent) => {
        val format = "png"
        val file = new FileChooser {
          extensionFilters += new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png")
        }.showSaveDialog(scene.value.getWindow)
        if (file != null)
          ImageIO.write(SwingFXUtils.fromFXImage(spiv.bgimage, null), format, file)
      }
    }
    children += new Button("Reset picture") {
      onAction = (_: ActionEvent) => {
        spiv.setImage(imgOrig)
      }
    }
    children += new Button("Export tracks") {
      onAction = (_: ActionEvent) => {
        val task = new Task[Unit] {
          override def call(): Unit = {
            val gpx = jpx.GPX.builder()
            val indices: Seq[Int] = if (cbSelectedOnly.selected.value)
              tracksList.getSelectionModel.getSelectedIndices.map(_.toInt).toSeq
            else
              tracks.indices.toList
            indices.foreach(iii => {
              updateProgress(iii, tracks.length)
              val jt = jpx.Track.builder()
              val tt = tracks(iii)
              jt.name(tt.trackname.get)
              transaction {
                val segids = from(DB.segments)(s => where(s.segtrack === tt.id) select s.id).toList
                for (segid <- segids) {
                  val trkpts = from(DB.trackPoints)(tp => where( tp.trkptseg === segid) select tp orderBy tp.trkpttime)
                  val js = jpx.TrackSegment.builder()
                  for (trkpt <- trkpts) {
                    js.addPoint(jpx.WayPoint.builder().
                      lat(trkpt.trkptlat.get).lon(trkpt.trkptlon.get).ele(trkpt.trkptalt.get).
                      time(trkpt.trkpttime.getOrElse(0L)).build())
                  }
                  jt.addSegment(js.build())
                }
              }
              gpx.addTrack(jt.build())
              if (isCancelled) throw new InterruptedException("interrupted!")
            })
            jpx.GPX.write(gpx.build(), Paths.get(Settings.prefs.get(Settings.EXPORTFILEPATH, "")))
          }
        }

        val fc = new FileChooser() {
          title = "Select gpx export filename"
          extensionFilters += new FileChooser.ExtensionFilter("GPX files (*.gpx)", "*.gpx")
        }
        val inidir = new java.io.File(Settings.prefs.get(Settings.EXPORTFILEPATH, "/")).getParentFile
        if (inidir != null) fc.setInitialDirectory(inidir)
        val res: java.io.File = fc.showSaveDialog(scene.value.getWindow)
        val ff = new java.io.File(res.getPath)
        if (ff != null) {
          Settings.prefs.put(Settings.EXPORTFILEPATH, res.toString)
          new MyWorker("Export tracks", task).runInBackground()
        }
      }
    }
    children += cbSelectedOnly
  }
  private val maincontent = new BorderPane() {
    top = new VBox {
      children += menuBar
      children += statusBar
    }
    center = sph
    bottom = statusbar
  }

  content = maincontent
  maincontent.prefHeight <== this.height
  maincontent.prefWidth <== this.width


  def awt2jfxImage(image: java.awt.image.RenderedImage): Image = {
    val out = new java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(image, "png", out)
    out.flush()
    val in = new java.io.ByteArrayInputStream(out.toByteArray)
    new Image(in, image.getWidth, image.getHeight, true, false)
  }

  var crs: CoordinateReferenceSystem = _
  var env: Bounds = _
  var coverage: GridCoverage2D = _


  //noinspection ConvertExpressionToSAM
  def loadTracksDB: Task[Unit] = new Task[Unit]() {
    override def call(): Unit = {
      try {
        DB.initialize()

        updateMessage("Loading tracks from DB...")
        runUIwait {
          folderActionDisabled.set(true)
          val oldf = Option(cbFolders.value.value).getOrElse(NOFOLDER)
          tracks.clear()
          folders.clear()
          folders += ALLFOLDERS
          folders += NOFOLDER
          transaction {
            from(DB.tracks)(a => select(a)).foreach(aa => {
              debug(s"loading track: ${aa.id }: ${aa.toString }")
              val addit = if (oldf == NOFOLDER)
                aa.trackfolder.getOrElse("---") == "---"
              else if (oldf == ALLFOLDERS)
                true
              else
                aa.trackfolder.getOrElse("---") == oldf
              if (addit) tracks += aa
              aa.trackfolder.foreach(tf => if (!folders.contains(tf)) folders += tf)
            })
            cbFolders.setValue(oldf)
          }
          folderActionDisabled.set(false)
          info(s"folders: [${folders.mkString(",")}]")
        }
      } catch {
        case e: Exception => error("can't load database! " + e.getMessage)
          e.printStackTrace()
      }


    }
  }

  //noinspection ConvertExpressionToSAM
  def loadImage: Task[Unit] = new Task[Unit]() {
    override def call(): Unit = {
      updateMessage("Loading image...")
      // load geotiff, must be in lat/lon format!
      val gtf = new java.io.File(Settings.prefs.get(Settings.IMGPATH,""))

      val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, DefaultGeographicCRS.WGS84)
      val gtreader = new GeoTiffReader(gtf, hints)
      // this does not work... removed also epsg requirement
      // val format = GridFormatFinder.findFormat(gtf)
      // val gtreader = format.getReader(gtf)

      coverage = gtreader.read(null)
      crs = coverage.getCoordinateReferenceSystem
      env = coverage.getEnvelope
      val ggeom = coverage.getGridGeometry
      // ImageIO.write(image, "png", new java.io.File("/tmp/asdf.png"))
      debug(s"cov: $coverage")
      debug(s"format: ${gtreader.getFormat }")
      debug(s"crs: $crs")
      debug(s"env: $env")
      debug(s"ggeom: $ggeom")

      // for drawing: set vars
      val img1 = coverage.getRenderedImage
      imgOrig = awt2jfxImage(img1)
      updateMessage("Showing image...")
      runUIwait{
        spiv.setImage(imgOrig)
      }
    }
  }



  def afterShown(): Unit = {
    val t1 = loadTracksDB
    t1.onSucceeded = () => {
      if (folders.nonEmpty) {
        new MyWorker[Unit]("Load image", loadImage).runInBackground()
      }
    }
    new MyWorker[Unit]("Load tracks", t1).runInBackground()
  }
}

object Main extends JFXApp3 with Logging {

  var mainScene: MainScene = _
  val logfile: File = java.io.File.createTempFile("oruxtoollog", ".txt")

  // JFXApp3: UI init stuff must go into this!
  override def start(): Unit = {
    // redirect console output, must happen on top of this object!
    val oldOut = System.out
    val oldErr = System.err
    val logps: io.FileOutputStream = new io.FileOutputStream(logfile)
    System.setOut(new io.PrintStream(new MyConsole(false), true))
    System.setErr(new io.PrintStream(new MyConsole(true), true))

    Thread.currentThread().setUncaughtExceptionHandler((_: Thread, e: Throwable) => {
      error("Exception: " + e.getMessage)
      e.printStackTrace()
      if (stage.isShowing) Helpers.showExceptionAlert("", e)
    })

    class MyConsole(errchan: Boolean) extends io.OutputStream {
      override def write(b: Int): Unit = {
        if (logps != null) logps.write(b)
        (if (errchan) oldErr else oldOut).print(b.toChar.toString)
      }
    }

    def loadMainScene(): Unit = {
      stage = new PrimaryStage {
        title = "Oruxtool"
        width = 1200
        height = 800
        mainScene = tryit {
          new MainScene()
        }
        scene = mainScene
        debug("huhu")
        onShown = (_: WindowEvent) => { // works only if no stage shown before...
          tryit {
            info("log file: " + logfile)
            mainScene.afterShown()
          }
        }
        tryit {
          icons += new Image(getClass.getResource("/icons/icon_16x16.png").toExternalForm)
          icons += new Image(getClass.getResource("/icons/icon_32x32.png").toExternalForm)
          icons += new Image(getClass.getResource("/icons/icon_256x256.png").toExternalForm)
        }
      }
    }

    // Dock icon
    if (Helpers.isMac) Taskbar.getTaskbar.setIconImage(ImageIO.read(getClass.getResource("/icons/icon_256x256.png")))

    loadMainScene()
  }

  override def stopApp(): Unit = {
    info("*************** stop app")
    DB.terminate()
    sys.exit(0)
  }

}


