package net.chloe.wow

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

object Pet {
  
  def getHealthPercentage(implicit player: WowClass) = {
    if (!Player.hasPet) {
      0
    } else {
      Wow
        .captureColor(column = 13, row = 1)
        .getRedAsPercentage
    }
  }

  def geBufftStacksCount(buff: Buff)(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = buff.buffIndexInAddon, row = 10)
    
    if (color.red == 255) {
      0
    } else {
      color.getRedAsPercentage
    }
  }
  
  def getBuffRemainingTimeOpt(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 10)
    
    if (color.red == 255 || color.blue == 255) {
      None
    } else {
      val a = color.getGreenAsPercentage
      var b = color.getBlueAsPercentage
      
      Some((a * 1000 + b).millis)
    }
  }
  
  def hasBuff(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 10)
    
    color.red != 255 && color.blue != 255
  }

}