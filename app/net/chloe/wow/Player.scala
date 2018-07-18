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
import net.chloe.models.auras._
import net.chloe.models.spells._
import scala.concurrent.duration._
import org.joda.time._

object Player {
  
  def getUnitLocations(implicit player: WowClass) = {
    
  }
  
  def getHealthPercentage(implicit player: WowClass) = {
    val color = Wow.captureColor(column = 1, row = 1)
    val health = color.getRedAsPercentage
    
    //Logger.debug(
    //  s"Health: $health - Colors are red: ${color.red} - " + 
    //    s"green: ${color.green} - blue: ${color.blue}."
    //)
    
    health
  }
  
  def isInCombat(implicit player: WowClass) = {
    val color = Wow.captureColor(column = 4, row = 1)
    val isInCombat = color.isRed
    
   //Logger.debug(
   //   s"IsInCombat: $isInCombat - Colors are red: ${color.red} - " + 
   //     s"green: ${color.green} - blue: ${color.blue}."
   //)
    
    isInCombat
  }
  
  def isChanneling(implicit player: WowClass) = {
    Wow
      .captureColor(column = 8, row = 1)
      .isGreen
  }
  
  def isCasting(implicit player: WowClass) = {
    Wow
      .captureColor(column = 8, row = 1)
      .isRed
  }
  
  def hasTarget(implicit player: WowClass) = {
    if (Player.getHealthPercentage == 0) {
      false
    } else if (Target.getHealthPercentage == 0) {
      false
    } else if (Target.isBoss) {
      true
    }
    
    Wow
      .captureColor(column = 7, row = 1)
      .isRed
  }
  
  def getLastSpellCastedIdOpt(implicit player: WowClass) = {
    Wow.buildSpellIdOpt(
      leftColumn = 5,
      leftRow = 7,
      rightColumn = 6,
      rightRow = 7
    ) match {
      case Some(lastSpellId) =>
        player.lastSpellIdOpt = Some(lastSpellId)
        Some(lastSpellId)
      case None => None
    }
  }
  
  def getLastSpellCastedOpt(implicit player: WowClass) = {
    getLastSpellCastedIdOpt match {
      case Some(spellId) => player.spells.find(_.id == spellId)
      case _ => None
    } 
  }
  
  def getEnnemiesCountInRange(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = 1, row = 17)
    
    if (color.red != 255 || color.blue == 0) {
      0
    } else {
      color.getGreenAsPercentage
    }
  }
  
  def areNameplatesOn(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = 1, row = 17)
    
    if (color.red != 255) {
      false
    } else if(color.blue == 255) {
      true
    } else {
      false
    }
  }
  
  def hasPet(implicit player: WowClass) = {
    Wow
      .captureColor(column = 12, row = 1)
      .isRed
  }

  def getPowerPercentage(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = 2, row = 1)
    
    if (color.green == 255 && color.red == 255) {
      0
    } else if (color.green != 0) {
      color.getGreenAsPercentage + 100
    } else {
      color.getRedAsPercentage
    }
  }
  
  def getHastePercentage(implicit player: WowClass) = {
    val color =  Wow.captureColor(column = 10, row = 1)
    
    if (color.red == 0 || color.red == 255) {
      val redAsPercentage = color.getRedAsPercentage
      val greenAsPercentage = color.getGreenAsPercentage
      val blueAsPercentage = color.getBlueAsPercentage
      
      if (redAsPercentage == 100) {
        greenAsPercentage * 100 + blueAsPercentage
      } else {
        -(greenAsPercentage * 100 + blueAsPercentage)
      }
    } else {
      0
    }
  }
  
  def isAutoAttacking(implicit player: WowClass) = {
    Wow
      .captureColor(column = 2, row = 7)
      .isRed
  }
  
  def isMoving(implicit player: WowClass) = {
    val color = Wow.captureColor(column = 1, row = 7)
    color.red == 255 && color.blue == 255
  }
  
  def isMounted(implicit player: WowClass) = {
    val color = Wow.captureColor(column = 1, row = 7)
    color.green == 255 && color.blue == 255
  }
  
  def isOutdoor(implicit player: WowClass) = {
    Wow
      .captureColor(column = 4, row = 7)
      .isRed
  }
  
  def isSpellOnCooldown(cooldown: Cooldown)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = cooldown.cooldownIndexInAddon, row = 2)
    
    color.red == 0
  }
  
  def isSpellOnGcd(cooldown: Cooldown)(implicit player: WowClass) = {
    Wow
      .captureColor(column = cooldown.cooldownIndexInAddon, row = 2)
      .isRed
  }
  
  def getCooldownRemainingTimeOpt(cooldown: Cooldown)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = cooldown.cooldownIndexInAddon, row = 2)
    
    if (color.red == 255) {
      None
    } else {
      val a = color.getGreenAsPercentage
      var b = color.getBlueAsPercentage
      
      Some((a * 1000 + b).millis)
    }
  }
  
  def updateLastInterruptTime(interruptSpell: Cooldown)(implicit team: Team, player: WowClass) = {
    getCooldownRemainingTimeOpt(interruptSpell) match {
      case Some(timeRemaining) if timeRemaining.toMillis > 0 =>
        team.lastInterrupt.synchronized {
          val lastTeamInterrupt = team.lastInterrupt
          val lastPlayerInterrupt = new DateTime().minusMillis(timeRemaining.toMillis.toInt)
          
          if (lastTeamInterrupt.isBefore(lastPlayerInterrupt)) {
            team.lastInterrupt = lastPlayerInterrupt
          }
        }
      case _ =>
    }
  }
  
  def isSpellInRange(cooldown: Cooldown)(implicit player: WowClass) = {
    Wow
      .captureColor(column = cooldown.cooldownIndexInAddon, row = 3)
      .isRed
  }
  
  def getChargesCount(cooldown: Cooldown)(implicit player: WowClass) = {
    Wow
      .captureColor(column = cooldown.cooldownIndexInAddon, row = 5)
      .getGreenAsPercentage
  }
  
  def canCast(cooldown: Cooldown)(
    implicit player: WowClass
  ) = {
    val needTargetCheck = if (cooldown.needTarget) {
      Target.isVisible && isSpellInRange(cooldown)
    } else {
      true
    }
    
    val hasChargesCheck = if (cooldown.hasCharges) {
      getChargesCount(cooldown) > 0
    } else {
      true
    } 
    
    val isSpellOnCooldownCheck = if (cooldown.hasCooldownOtherThanGcd) {
      !isSpellOnCooldown(cooldown)
    } else {
      true
    } 
    
    val isMovingCheck = if (cooldown.isInstant) {
      true
    } else {
      !isMoving
    } 
    
    !isCasting && 
      !isChanneling && 
      isSpellOnCooldownCheck && 
      needTargetCheck &&
      hasChargesCheck && 
      needTargetCheck &&
      !isSpellOnGcd(cooldown) && 
      isMovingCheck
  }
  
  def isAlive(implicit player: WowClass) = {
    val health = getHealthPercentage
    health > 0 && health < 100
  }
  
  def getBuffStacksCount(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 8)
    
    if (color.red == 255) {
      0
    } else {
      color.getRedAsPercentage
    }
  }
  
  def getBuffRemainingTimeOpt(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 8)
    
    if (color.red == 255 || color.blue == 255) {
      None
    } else {
      val a = color.getGreenAsPercentage
      var b = color.getBlueAsPercentage
      
      Some((a * 1000 + b).millis)
    }
  }
  
  def isItemOnCooldown(item: Item)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = item.itemIndexInAddon, row = 9)
    
    color.red == 0
  }
  
  def hasBuff(buff: Buff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = buff.buffIndexInAddon, row = 8)
    
    color.red != 255 && color.blue != 255
  }
  
  def hasDebuff(debuff: Debuff)(implicit player: WowClass) = {
    val color = Wow.captureColor(column = debuff.debuffIndexInAddon, row = 11)
    
    color.red != 255 && color.blue != 255
  }
  
}