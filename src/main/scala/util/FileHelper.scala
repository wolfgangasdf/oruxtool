package util

import java.net.URI

import framework.{Helpers, Logging}


object FileHelper extends Logging {


  def openURL(url: String): Unit = {
    import java.awt.Desktop
    if (Desktop.isDesktopSupported && url != "") {
      val desktop = Desktop.getDesktop
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        desktop.browse(new URI(url))
      }
    }
  }

  def revealFile(file: java.io.File): Unit = {
    if (Helpers.isMac) {
      Runtime.getRuntime.exec(Array("open", "-R", file.getPath))
    } else if (Helpers.isWin) {
      Runtime.getRuntime.exec("explorer.exe /select,"+file.getPath)
    } else if (Helpers.isLinux) {
      error("not supported OS, tell me how to do it!")
    } else {
      error("not supported OS, tell me how to do it!")
    }
  }

}

