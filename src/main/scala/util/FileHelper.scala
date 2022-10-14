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

}

