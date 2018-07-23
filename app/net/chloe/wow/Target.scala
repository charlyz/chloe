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
import net.chloe.models.classes._
import javax.imageio._
import net.chloe.models._
import com.sun.jna.platform.win32.BaseTSD._
import scala.util._
import net.chloe.models.Color
import net.chloe.Configuration
import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._
import org.joda.time.DateTime

object Target {
  
  def getHealthPercentage(implicit player: WowClass) = {
    Wow
      .captureColor(column = 3, row = 1)
      .getRedAsPercentage
  }
  
  def isBoss(implicit player: WowClass) = {
    Wow
      .captureColor(column = 7, row = 1)
      .isBlue
  }
  
  def getCastingSpellIdOpt(implicit player: WowClass) = {
    if (!Player.hasTarget) {
      None
    } else {
      Wow.buildSpellIdOpt(
        leftColumn = 7,
        leftRow = 7,
        rightColumn = 8,
        rightRow = 7
      )
    } 
  }
  
  def isCasting(implicit player: WowClass) = {
    getCastingSpellIdOpt match {
      case Some(spellId) if spellId > 0 => true
      case _ => false
    }
  }
  
  def isCastingAndSpellIsInterruptible(implicit player: WowClass) = {
    Wow
      .captureColor(column = 9, row = 1)
      .getRedAsBoolean
  }
  
  def canInterruptAsTeam(implicit team: Team, me: WowClass) = {
    val now = new DateTime()
    team.lastInterrupt.synchronized {
      if ((now.getMillis - team.lastInterrupt.getMillis).millis < Configuration.MinimumTimeBetweenInterrupt) {
        false
      } else {
        isCastingAndSpellIsInterruptible
      }
    }
  }
  
  def getCastingPercentage(implicit player: WowClass) = {
    100 - 
      Wow
        .captureColor(column = 1, row = 1)
        .getGreenAsPercentage
  }
  
  def isVisible(implicit player: WowClass) = {
    Wow
      .captureColor(column = 11, row = 1)
      .isRed
  }
  
  def isFriend(implicit player: WowClass) = {
    Wow
      .captureColor(column = 6, row = 1)
      .isGreen
  }
  
  def isPlayer(implicit player: WowClass) = {
    Wow
      .captureColor(column = 3, row = 7)
      .isRed
  }
  
  def getDebuffRemainingTimeOpt(debuff: Debuff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = debuff.debuffIndexInAddon, row = 4)
    
    if (color.red == 255) {
      None
    } else {
      val a = color.getGreenAsPercentage
      var b = color.getBlueAsPercentage
      
      Some((a * 1000 + b).millis)
    }
  }
  
  def hasBuff(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 6)
    
    color.red != 255 && color.blue != 255
  }
  
  def getDebuffStacksCount(debuff: Debuff)(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = debuff.debuffIndexInAddon, row = 4)
    
    if (color.red == 255) {
      0
    } else {
      color.getRedAsPercentage
    }
  }
  
  def hasDebuff(debuff: Debuff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = debuff.debuffIndexInAddon, row = 4)
    
    color.red != 255 && color.blue != 255
  }

}