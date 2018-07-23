package net.chloe.wow

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
import net.chloe.win32.Memory
import net.chloe.win32.Implicits._
import net.chloe.Configuration.Offsets
import scala.collection.BitSet

object Player {
  
  def getUnitNameLookup(
    guidToPlayerName: Map[String, String] = Map(),
    nextEntryOpt: Option[Long] = None,
    currentIteration: Int = 0,
    previousEntries: Set[Long] = Set()
  )(
    implicit player: WowClass
  ): Map[String, String] = {
    val hProcess = player.hProcess
    val baseAddress = player.baseAddress
    
    if (currentIteration > 1000) {
      guidToPlayerName
    } else {
      val nextEntry: Long = nextEntryOpt match {
        case Some(foundNextEntry) => foundNextEntry
        case _ => 
          Memory.readPointer(
            hProcess, baseAddress + Offsets.NamesCache.Base
          )
      }
      
      if (previousEntries.contains(nextEntry)) {
        guidToPlayerName
      } else if (nextEntry <= 0 || nextEntry == Long.MaxValue) {
        guidToPlayerName
      } else {     
        val nextNextEntry: Long = Memory.readPointer(hProcess, nextEntry + Offsets.NamesCache.NextEntry)
 
        val guid = Memory.readGUID(hProcess, nextEntry + Offsets.NamesCache.Entry.GUID)
        val newGUIDToPlayerName = Memory.readStringOpt(hProcess, nextEntry + Offsets.NamesCache.Entry.Name) match {
          case Some(name) => guidToPlayerName + (guid -> name)
          case _ => guidToPlayerName
        }
        
        getUnitNameLookup(
          newGUIDToPlayerName,
          Some(nextNextEntry),
          currentIteration + 1,
          previousEntries + nextEntry
        )
      }
    }
  }
  
  def getUnitLocations(
    guidToPlayerName: Map[String, String] = Map(),
    unitLocations: Map[String, Location] = Map(),
    nextEntryOpt: Option[Long] = None,
    currentIteration: Int = 0,
    previousEntries: Set[Long] = Set()
  )(
    implicit player: WowClass
  ): Map[String, Location] = {
    val hProcess = player.hProcess
    val baseAddress = player.baseAddress
    
    if (currentIteration > 20000) {
      unitLocations
    } else {
      val foundGUIDToPlayerName = if (guidToPlayerName.isEmpty) {
        getUnitNameLookup()
      } else {
        guidToPlayerName
      }
      
      val nextEntry: Long = nextEntryOpt match {
        case Some(foundNextEntry) => foundNextEntry
        case _ => 
          val entitiesListBase = Memory.readPointer(
            hProcess, baseAddress + Offsets.EntitiesList.Base
          )

          Memory.readPointer(hProcess, entitiesListBase + Offsets.EntitiesList.FirstEntry)
      }

      if (previousEntries.contains(nextEntry)) {
        unitLocations
      } else if (nextEntry <= 0 || nextEntry == Long.MaxValue) {
        unitLocations
      } else {
        val entryType = Memory.readByte(
          hProcess, nextEntry + Offsets.EntitiesList.Entry.Type
        )
        
        val descriptors: Long = Memory.readPointer(
          hProcess, nextEntry + Offsets.EntitiesList.Entry.Descriptors
        )
          
        val nextNextEntry: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.NextEntry)
        
        if (descriptors > 0) {  
          val newUnitLocations = entryType match {
            case 
              Configuration.ObjectTypes.LocalPlayer | 
              Configuration.ObjectTypes.Player  | 
              Configuration.ObjectTypes.NPC =>
                
              val guid = Memory.readGUID(hProcess, nextEntry + Offsets.EntitiesList.Entry.GUID)
              val x = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.Unit.X)
              val y = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.Unit.Y)
              val z = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.Unit.Z)
              val angle = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.Unit.Angle)
              
              val nameOpt = if (entryType == Configuration.ObjectTypes.NPC) {
                val npcName1: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.NPC.Name1)
                val npcName2: Long = Memory.readPointer(hProcess, npcName1 + Offsets.EntitiesList.NPC.Name2)
                Memory.readStringOpt(hProcess, npcName2)
              } else {
                foundGUIDToPlayerName.get(guid)
              }
              
              val targetNameOpt = if (
                entryType == Configuration.ObjectTypes.Player ||
                entryType == Configuration.ObjectTypes.LocalPlayer
              ) {
                val targetGUID = Memory.readGUID(hProcess, nextEntry + Offsets.EntitiesList.Player.Target)
                val a= foundGUIDToPlayerName.get(targetGUID)
                //println("targetNameOpt " + a)
                a
              } else {
                None
              }
              
              nameOpt match {
                case Some(foundName) =>
                  unitLocations + (foundName -> Location(x, y, z, angle, guid, foundName, targetNameOpt))
                case _ => unitLocations
              }
            case  _ => unitLocations
          }
          
          getUnitLocations(
            foundGUIDToPlayerName,
            newUnitLocations,
            Some(nextNextEntry),
            currentIteration + 1,
            previousEntries + nextEntry
          )
        } else {
          getUnitLocations(
            guidToPlayerName,
            unitLocations,
            Some(nextNextEntry),
            currentIteration + 1,
            previousEntries + nextEntry
          )
        }
      }
    }
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