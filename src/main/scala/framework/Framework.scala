package framework

import scalafx.Includes._
import scalafx.concurrent.{Service, WorkerStateEvent}
import scalafx.scene.control._
import scalafx.scene.layout.VBox

// https://github.com/scalafx/ProScalaFX/blob/master/src/proscalafx/ch06/ServiceExample.scala
class MyWorker[T](atitle: String, atask: javafx.concurrent.Task[T]) {
  object worker extends Service[T](new javafx.concurrent.Service[T]() {
    override def createTask(): javafx.concurrent.Task[T] = atask
  })
  val lab = new Label("")
  val progress = new ProgressBar { minWidth = 250 }
  val al = new Dialog[Unit] {
    initOwner(main.Main.stage)
    title = atitle
    dialogPane.value.content = new VBox { children ++= Seq(lab, progress) }
    dialogPane.value.getButtonTypes += ButtonType.Cancel
  }
  al.onCloseRequest = () => {
    atask.cancel()
  }
  def runInBackground(): Unit = {
    al.show()
    lab.text <== worker.message
    progress.progress <== worker.progress
    worker.onSucceeded = (_: WorkerStateEvent) => {
      al.close()
    }
    worker.onFailed = (_: WorkerStateEvent) => {
      println("onfailed: " + atask.getException.getMessage)
      atask.getException.printStackTrace()
      al.close()
    }
    worker.start()
  }
}
