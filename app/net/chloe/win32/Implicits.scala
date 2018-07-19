package net.chloe.win32

import com.sun.jna.Memory
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinUser._
import com.sun.jna.Native
import net.chloe.win32._
import net.chloe._
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import java.awt.image.BufferedImage
import com.sun.jna.platform.win32.WinDef._
import scala.collection.mutable.ListBuffer
import java.io._
import java.awt.image._
import javax.imageio._
import net.chloe.models._
import com.sun.jna.platform.win32.BaseTSD._
import scala.util._
import net.chloe.models.Color
import net.chloe.Configuration
import play.api.Logger
import net.chloe.models.classes._
import net.chloe.models.spells._
import net.chloe.models._
import net.chloe.models.auras._
import scala.concurrent.duration._

object Implicits {
  
  implicit def pointerToLong(pointer: Pointer): Long = {
    Pointer.nativeValue(pointer)
  }
  
}