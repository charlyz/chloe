package net.chloe.wow

import java.awt.image._
import java.awt.image.BufferedImage

import scala.collection.mutable.ListBuffer

import com.sun.jna.{Memory => JnaMemory}
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import com.sun.jna.platform.win32.WinUser._
import com.sun.jna.platform.win32.WinNT._
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
import org.joda.time.{ Duration => JodaDuration, _ }
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent._
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import net.chloe.win32.Memory
import net.chloe.Configuration.Offsets
import net.chloe.win32.Implicits._
import net.chloe.models.spells._
import java.util.concurrent.ForkJoinPool

object Wow {
  
  var lastLeftClick = DateTime.now
  val isCtmInProgress = new AtomicBoolean(false)
  
  val WM_RBUTTONDOWN = 0x0204
	val WM_RBUTTONUP = 0x0205
	val WM_MOUSEMOVE = 0x0200
	val WM_SETCURSOR = 0x0020
	val wordShift = 16;
  
  val writingForkjoinPool = ExecutionContext.fromExecutor(new ForkJoinPool())
    
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
    x: Int,
    y: Int
  )(
    implicit player: WowClass
  ) = {
    val rct = new RECT()

    if (!User32.GetWindowRect(player.hWindow, rct))
    {
      throw new Exception("Could not get window size.")
    }
        
    val newX = x - rct.left
    val newY = y - rct.top
    
    println(s"newX $newX - newY $newY")
    
    val lParam = new LPARAM(newX | (newY << wordShift))

    User32.PostMessage(player.hWindow, WM_MOUSEMOVE, new WPARAM(2), lParam)
    User32.PostMessage(player.hWindow, WM_RBUTTONDOWN, new WPARAM(1), lParam)
    User32.PostMessage(player.hWindow, WM_RBUTTONUP, new WPARAM(0), lParam)
  }
  
  
  def pressAndReleaseKeystroke(key: Int, duration: Duration = 50.millis)(implicit player: WowClass) = {
    pressAndReleaseKeystrokes(List(key), duration)   
  }
  
  def getPrimaryPlayer(team: Team) = {
    val (_, primaryCharacter) = team.players
      .find { case (_, player) =>
        Player.isPrimary(player)
      }
      .getOrElse {
        throw new Exception("Primary character not found.")
      }
      
    primaryCharacter
  }
  
  def getCameraMatrices(implicit player: WowClass) = {
    val base1 = Memory.readPointer(player.hProcess, player.baseAddress + Offsets.Camera.Base1)
    val base2 = Memory.readPointer(player.hProcess, base1 + Offsets.Camera.Base2)
    
    val xX = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixX)
    val xY = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixX + 0x04)
    val xZ = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixX + 0x08)
    
    val yX = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixY)
    val yY = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixY + 0x04)
    val yZ = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixY + 0x08)
    
    val zX = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixZ)
    val zY = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixZ + 0x04)
    val zZ = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.MatrixZ + 0x08)
    
    val originX = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.Origin)
    val originY = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.Origin + 0x04)
    val originZ = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.Origin + 0x08)
    
    val fov = Memory.readFloat(player.hProcess, base2 + Offsets.Camera.Fov)
    
    CameraMatrices(xX, xY, xZ, yX, yY, yZ, zX, zY, zZ, originX, originY, originZ, fov)
  }
  
  def getWorldToScreenCoordinates(targetX: Float, targetY: Float, targetZ: Float)(implicit player: WowClass) = {
    val (nameToPlayerEntity, _, _) = Player.getEntities()
    
    nameToPlayerEntity.get(player.name) match {
      case Some(playerLocation) =>
        val rec = new RECT()
        
        if (!User32.GetWindowRect(player.hWindow, rec)) {
          throw new Exception("Could not get the window size.")
        }
        
        val point = new POINT(0, 0)
        
        if (!User32.ClientToScreen(player.hWindow, point)) {
          throw new Exception("Could not execute ClientToScreen.")
        }
        
        rec.left = point.x
        rec.top = point.y

        val windowWidth = rec.right - rec.left
        val windowHeight = rec.bottom - rec.top
        
        println(s"windowWidth $windowWidth - windowHeight $windowHeight")

        val offsetWidth = rec.left
        val offsetHeight = rec.top
        
        println(s"offsetWidth $offsetWidth - offsetHeight $offsetHeight")

        val ar = windowWidth / windowHeight
        val fovX = 2 * Math.atan(0.27 * ar).toFloat
        val fovY = 0.5f
        
        val centerX = windowWidth * 0.5f
        val centerY = windowHeight * 0.5f
        
        val cameraMatrices = getCameraMatrices
        
        println(cameraMatrices)
        
        val positionX = targetX - cameraMatrices.originX
        val positionY = targetY - cameraMatrices.originY
        val positionZ = targetZ - cameraMatrices.originZ
        
        val transformX = positionX * cameraMatrices.yX + positionY * cameraMatrices.yY + positionZ * cameraMatrices.yZ
        val transformY = positionX * cameraMatrices.zX + positionY * cameraMatrices.zY + positionZ * cameraMatrices.zZ
        val transformZ = positionX * cameraMatrices.xX + positionY * cameraMatrices.xY + positionZ * cameraMatrices.xZ
        
        val (outX, outY) = if (transformZ > 0.1) {
          (
            (1f - (transformX / fovX / transformZ)) * centerX,
            (1f - (transformY / fovY / transformZ)) * centerY
          )
        } else {
          (-1f, -1f)
        }
        
        (
          (outX + offsetWidth).toInt,
          (outY + offsetHeight).toInt
        )
      case _ => throw new Exception("Player location not found.")
    }
  }
  
  def getOtherPlayerEntitiesToHide(
    team: Team, 
    playerName: String,  
    playerNameToPlayerEntity: Map[String, PlayerEntity],
    npcEntities: List[NPCEntity]
  ): List[EntityLocation] = {
   val playerEntities = team.players
      .flatMap { case (_, player) =>
        if (player.name == playerName) {
          None
        } else {
          playerNameToPlayerEntity.get(player.name) match {
            case Some(playerEntity) => Some(playerEntity)
            case _ => None
          }
        }
      }
      .toList
      
    playerEntities ++ npcEntities
  }
  
  def clickToMoveSlave(
    ctmX: Float, 
    ctmY: Float, 
    ctmZ: Float,
    team: Team
  )(
    implicit player: WowClass
  ): Unit = {
    //println("Target " + (ctmX, ctmY, ctmZ))
    Wow.pressAndReleaseKeystrokes(List(Keys.Alt, Keys.Add))(player)
    val (x, y) = getWorldToScreenCoordinates(ctmX, ctmY, ctmZ)(player)
    println(s"${player.name} is going there: $x - $y")
    
    val (nameToPlayerEntity, _, guidToEntityLocation) = Player.getEntities()
    nameToPlayerEntity.get(player.name) match {
      case Some(playerEntity) => 
        val otherEntitiesToHide = guidToEntityLocation.values.filter(_.guid != playerEntity.guid)
        
        @volatile var stopHiding = false
        val hidingOtherPlayersFuture = Future {
          blocking {
            while (!stopHiding) {
              otherEntitiesToHide.foreach { case otherEntityToHide =>
                Memory.writeFloat(player.hProcess, otherEntityToHide.xAddress, 0f)
              }
              Thread.sleep(5)
            }
          }
        }(writingForkjoinPool)
        
        rightClick(x, y)(player)
        Wow.pressAndReleaseKeystrokes(List(Keys.Alt, Keys.Add))(player)
        
        stopHiding = true
        Await.result(hidingOtherPlayersFuture, 10.seconds)
      case _ =>
    }
  }
  
  def clickToMoveSlaves(
    team: Team, 
    xOpt: Option[Float] = None, 
    yOpt: Option[Float] = None, 
    zOpt: Option[Float] = None
  ): Unit = {
    Future {
      blocking {
        if (isCtmInProgress.compareAndSet(false, true)) {
          val now = DateTime.now
          val shouldAutoFace = now.getMillis - lastLeftClick.getMillis < 200
          lastLeftClick = DateTime.now
          if (shouldAutoFace) {
            implicit val primaryPlayer = Wow.getPrimaryPlayer(team)
            val (ctmX, ctmY, ctmZ) = (xOpt, yOpt, zOpt) match {
              case (Some(x), Some(y), Some(z)) => (x, y, z)
              case _ => Player.getCursorCoordinates
            }
            println("Target " + (ctmX, ctmY, ctmZ))
            val ctmFuture = Future.traverse(team.players) { 
              case (_, player) if player != primaryPlayer =>
                Future {
                  blocking {
                    clickToMoveSlave(ctmX, ctmY, ctmZ, team)(player)
                  }
                }
              case _ => Future.successful(())
            }
            
            Await.result(ctmFuture, 10.seconds)
          }
          
          isCtmInProgress.set(false)

        } else {
          ()
        }
      }
    }
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
      val buffer = new JnaMemory(width * height * 4)
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