package net.chloe.wow

import java.awt.image._
import java.awt.image.BufferedImage

import scala.collection.mutable.ListBuffer

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import com.sun.jna.platform.win32.WinUser._
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import net.chloe.models.classes._
import net.chloe._
import net.chloe.Configuration
import net.chloe.models._
import net.chloe.models.Color
import net.chloe.win32._
import play.api.Logger
import scala.util.Random
import scala.concurrent.duration._

object Wow {
  
  def pressAndReleaseKeystrokes(
    keys: List[Int],
    duration: Duration = 50.millis
  )(
    implicit player: WowClass
  ) = player.name.synchronized {
    keys.foreach { key =>
      User32.SendMessage(player.hWindow, 0x100, key, 0)
      Thread.sleep(duration.toMillis)
    }
    
    keys.reverse.foreach { key =>
      User32.SendMessage(player.hWindow, 0x101, key, 0)
      Thread.sleep(duration.toMillis)
    }    
  }
  
  def rightClick(
    implicit player: WowClass
  ) = player.name.synchronized {
    //User32.SendMessage(player.hWindow, 0x0204, key, 0)
    //Thread.sleep(50)
    //User32.SendMessage(player.hWindow, 0x0205, key, 0)
  }
  
  def pressAndReleaseKeystroke(key: Int, duration: Duration = 50.millis)(implicit player: WowClass) = {
    pressAndReleaseKeystrokes(List(key), duration)   
  }
  
  def getWindows(pattern: String): List[HWND] = {
    val windows = ListBuffer[HWND]()
    
    User32.EnumWindows(
      new WNDENUMPROC() {
        def callback(hWnd: HWND, userData: Pointer): Boolean = {
          val windowTitle = getWindowTitleImpl(hWnd)
          val isVisible = User32.IsWindowVisible(hWnd)
  
          if (windowTitle.contains(pattern)) {
          	Logger.debug(s"Found window `$windowTitle` - isVisible: $isVisible")
          	windows += hWnd
          }
          
          return true
        }
      },
      null
    )
      
    windows.toList
  }
  
  def getWindowTitle(implicit player: WowClass) = {
    getWindowTitleImpl(player.hWindow)
  }
  
  def getWindowTitleImpl(hWindow: HWND) = {
    val windowTitleAsBytes = new Array[Byte](512)
    User32.GetWindowTextA(hWindow, windowTitleAsBytes, 512)
    Native.toString(windowTitleAsBytes)
  }
  
  def getWindowSize(implicit player: WowClass) = {
    val targetWindowBounds = new RECT()
    User32.GetClientRect(player.hWindow, targetWindowBounds)

    val windowWidth = targetWindowBounds.right - targetWindowBounds.left
    val windowHeight = targetWindowBounds.bottom - targetWindowBounds.top
    
    (windowWidth, windowHeight)
  }
  
  def captureColor(
    column: Int,
    row: Int
  )(
    implicit player: WowClass
  ) = {
    val pixel = Wow.capture(
      widthOpt = Some(1),
      heightOpt = Some(1),
      columnOpt = Some(column),
      rowOpt = Some(row)
    )

    Color(pixel.getRGB(0, 0))
  }
  
  // hTargetWindow -> Window Handle
  // hdc -> Device Context Handle
  def capture(
    widthOpt: Option[Int] = None, 
    heightOpt: Option[Int] = None,
    columnOpt: Option[Int] = None,
    rowOpt: Option[Int] = None
  )(
    implicit player: WowClass
  ): BufferedImage = {
      val hdcTargetWindow = User32.GetDC(player.hWindow)
      val hdcCopy = GDI32.CreateCompatibleDC(hdcTargetWindow)
      
      val (width, height) = (widthOpt, heightOpt) match {
        case (Some(width), Some(height)) =>
          (width, height)
        case _ =>
          val (windowWidth, windowHeight) = getWindowSize
          
          Logger.debug(s"Width: $windowWidth")
          Logger.debug(s"Height: $windowHeight")
          
          (windowWidth, windowHeight)
      }
      
      val (column, row) = (columnOpt, rowOpt) match {
        case (Some(column), Some(row)) => 
          val halfRatio = Configuration.ScalingRatio / 2
          (
            (column * Configuration.ScalingRatio) - halfRatio,
            (row * Configuration.ScalingRatio) - halfRatio
          )
        case _ => (0, 0)
      }
      
      if (width == 0 || height == 0) {
        throw NoDimensionException
      }
      
      // Bitmap that matches the target window dc.
      val hBitmap = GDI32.CreateCompatibleBitmap(hdcTargetWindow, width, height)
      // We assign the bitmap to the dc.
      val hOriginalDcOjbect = GDI32.SelectObject(hdcCopy, hBitmap)
      // We get the pixels from the target dc to the copy.
      GDI32.BitBlt(hdcCopy, 0, 0, width, height, hdcTargetWindow, column, row, WinGDIExtra.SRCCOPY)
      // We clean up by assigning the old object
      // to the dc copy, then we delete it.
      GDI32.SelectObject(hdcCopy, hOriginalDcOjbect)
      GDI32.DeleteDC(hdcCopy)
      
      val bmi = new BITMAPINFO()
      bmi.bmiHeader.biWidth = width
      bmi.bmiHeader.biHeight = -height
      bmi.bmiHeader.biPlanes = 1
      bmi.bmiHeader.biBitCount = 32
      bmi.bmiHeader.biCompression = WinGDI.BI_RGB
      
      // We create an empty buffer.
      val buffer = new Memory(width * height * 4)
      // Fill it up with the bits from the bitmap handle
      // and assign the bitmap info to it.
      GDI32.GetDIBits(hdcTargetWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);
      
      // We convert the buffer into a buffered image.
      val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width)
      
      // Cleanup
      GDI32.DeleteObject(hBitmap)
      User32.ReleaseDC(player.hWindow, hdcTargetWindow)

      return image
  }

  def nextScalingAction(implicit player: WowClass) = {
    val (currentWindowWidth, _) = getWindowSize
    
    if (foundSentinel) {
      NoScaling
    } else {
      if (currentWindowWidth == Configuration.SlaveWindowWith) {
        ScaleUp
      } else if (currentWindowWidth == Configuration.PrimaryWindowWith) {
        ScaleDown
      } else {
        throw new Exception(s"Unexpected window width: $currentWindowWidth")
      }
    }
  }
  
  def isSlave(implicit player: WowClass) = {
    val (currentWindowWidth, _) = getWindowSize
    
    if (!foundSentinel) {
      false
    } else {
      if (currentWindowWidth == Configuration.SlaveWindowWith) {
        true
      } else {
        false
      }
    }
  }
  
  def foundSentinel(implicit player: WowClass) = {
    val pixel = Wow.capture(
      widthOpt = Some(1),
      heightOpt = Some(1),
      columnOpt = Some(1),
      rowOpt = Some(13)
    )

    val color = pixel.getRGB(0, 0)
    val red = (color & 0xff0000) / 65536
    val green = (color & 0xff00) / 256
    val blue = (color & 0xff)

    (red == 255 && green == 0 && blue == 255)
  }
  
  def removeTrailingZerosFromSpell(spellId: Int): Int = {
    require(spellId > 0)
    
    val spellIdDividedByTen = spellId / 10
    
    if (spellIdDividedByTen % 10 == 0) {
      removeTrailingZerosFromSpell(spellIdDividedByTen)
    } else {
      spellIdDividedByTen
    }
  }
  
  def buildSpellIdOpt(
    leftColumn: Int, 
    leftRow: Int,
    rightColumn: Int, 
    rightRow: Int
  )(
    implicit player: WowClass
  ) = {
    val leftColor = Wow.captureColor(leftColumn, leftRow)
    val rightColor = Wow.captureColor(rightColumn, rightRow)
    
    val leftRed = leftColor.getRedAsPercentage / 10
    val leftGreen = leftColor.getGreenAsPercentage / 10
    val leftBlue = leftColor.getBlueAsPercentage / 10
    
    val rightRed = rightColor.getRedAsPercentage / 10
    val rightGreen = rightColor.getGreenAsPercentage / 10
    val rightBlue = rightColor.getBlueAsPercentage / 10
    
    require(leftRed < 10)
    require(leftGreen < 10)
    require(leftBlue < 10)
    require(rightRed < 10)
    require(rightGreen < 10)
    require(rightBlue < 10)
    
    val spellIdToClean = s"$leftRed$leftGreen$leftBlue$rightRed$rightGreen$rightBlue".toInt
    
    if (spellIdToClean == 0) {
      None
    } else {
      Some(Wow.removeTrailingZerosFromSpell(spellIdToClean))
    }
  }
  
}