package net.chloe.wow

import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import scala.concurrent.ExecutionContext.Implicits._
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
import scala.collection.mutable.{Set => MSet}
import scala.concurrent._
import scala.io.Source

object Player {
  
  val futurePlayerLocations = MSet[(Float, Float, Float)]()

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
  
  def getCursorCoordinates(implicit player: WowClass) = {
    val hProcess = player.hProcess
    val baseAddress = player.baseAddress
    
    val gameUIBase = Memory.readPointer(hProcess, baseAddress + Offsets.GameUI.Base)
    val x = Memory.readFloat(hProcess, gameUIBase + Offsets.GameUI.CursorX)
    val y = Memory.readFloat(hProcess, gameUIBase + Offsets.GameUI.CursorY)
    val z = Memory.readFloat(hProcess, gameUIBase + Offsets.GameUI.CursorZ)
    
    (x, y, z)
  }
  
  def isPrimary(implicit player: WowClass) = {
    val (currentWindowWidth, _) = Wow.getWindowSize
    currentWindowWidth == Configuration.PrimaryWindowWith
  }
  
  def getDistanceBetween2DPoints(
    x1: Float, 
    x2: Float, 
    y1: Float, 
    y2: Float
  ) = {
    Math.sqrt(
      Math.pow((x1 - x2).abs, 2)
      +
      Math.pow((y1 - y2).abs, 2)
    )
  }
  
  def getDistanceBetween3DPoints(
    x1: Float, 
    x2: Float, 
    y1: Float, 
    y2: Float,
    z1: Float,
    z2: Float
  ) = {
    Math.sqrt(
      Math.pow((x1 - x2).abs, 2)
      +
      Math.pow((y1 - y2).abs, 2)
      +
      Math.pow((z1 - z2).abs, 2)
    )
  }
  
  def findSafeSpot(
    playerX: Float,
    playerY: Float,
    playerZ: Float,
    playerAngle: Float,
    waypoints: Set[(Float, Float, Float)],
    harmfulAoes: List[AreaTriggerEntity],
    tries: Int = 0,
    bestSafeSpotOpt: Option[(Long, Float, (Float, Float, Float))] // (attempts, angle, (x, y, x))
  )(
    implicit player: WowClass
  ): Option[(Float, Float, Float)] = {
    if (tries == 200000) {
      None
    } else {
      waypoints.headOption match {
        case Some((newX, newY, newZ)) =>
          //println((newX, newY, newZ) + " - " + waypoints.size)
          val harmfulAoesDamagingPlayers = harmfulAoes
            .filter { areaTriggerEntity => 
              Player.isInAoe(playerX, playerY, areaTriggerEntity)(player)
            }
          
          val harmfulAoesDamagingPlayersAtSafePoint = harmfulAoes
            .filter { areaTriggerEntity => 
              Player.isInAoe(newX, newY, areaTriggerEntity)(player)
            }
          
          val (currentInnerAngle, _, _) = getInnerAndOuterAngles(
            playerX,
            newX,
            playerY,
            newY,
            playerZ,
            newZ,
            playerAngle
          )

          //println("player " + player.name)
          //println("harmfulAoes " + harmfulAoes)
          //println("harmfulAoesDamagingPlayers " + (harmfulAoesDamagingPlayers ++ harmfulAoesDamagingPlayersAtSafePoint))
          
          val isNotSafeSpot = (harmfulAoesDamagingPlayers ++ harmfulAoesDamagingPlayersAtSafePoint)
            .find { harmfulAoeDamagingPlayers =>
              harmfulAoeDamagingPlayers.safeSpotsOpt match {
                case Some(safeSpots) =>
                  if (safeSpots.contains((newX, newY, newZ))) {
                    false
                  } else {
                    true
                  }
                case _ =>
                  val distanceBetweenAoeAndSafePoint = getDistanceBetween3DPoints(
                    harmfulAoeDamagingPlayers.x, 
                    newX, 
                    harmfulAoeDamagingPlayers.y, 
                    newY,
                    harmfulAoeDamagingPlayers.z,
                    newZ
                  )
                  
                  if (
                    harmfulAoeDamagingPlayers.radius + 1 < distanceBetweenAoeAndSafePoint && 
                    distanceBetweenAoeAndSafePoint < harmfulAoeDamagingPlayers.radius + 20
                  ) {
                    //println(s"d $distanceToNewLocation - harmfulAoe.radius ${harmfulAoe.radius}")
                    //println("found it " + Some(newX, newY))
                    //println(s"${player.name} is NOT in AOE ${harmfulAoeDamagingPlayers.id}")
                    false
                  } else {
                    true
                  }  
              }
            }
            .isDefined
          //println("player " + player.name + " - isNotSafeSpot " + isNotSafeSpot)
          if (isNotSafeSpot) {
            findSafeSpot(
              playerX, 
              playerY, 
              playerZ, 
              playerAngle, 
              waypoints.tail, 
              harmfulAoes, 
              tries + 1, 
              bestSafeSpotOpt
            ) 
          } else {
            bestSafeSpotOpt match {
              case Some((attempts, oldAngle, (oldSafeSpotX, oldSafeSpotY, oldSafeSpotZ))) =>
                val distanceBetweenToonAndOldSafeSpot = getDistanceBetween3DPoints(
                  playerX, 
                  oldSafeSpotX, 
                  playerY, 
                  oldSafeSpotY,
                  playerZ,
                  oldSafeSpotZ
                )
                
                val distanceBetweenToonAndCurrentSafeSpot = getDistanceBetween3DPoints(
                  playerX, 
                  newX, 
                  playerY, 
                  newY,
                  playerZ,
                  newZ
                )

                val (oldInnerAngle, _, _) = getInnerAndOuterAngles(
                  playerX,
                  oldSafeSpotX,
                  playerY,
                  oldSafeSpotY,
                  playerZ,
                  oldSafeSpotZ,
                  playerAngle
                )
                
                val newBestSafeSpot = if (
                  distanceBetweenToonAndOldSafeSpot < distanceBetweenToonAndCurrentSafeSpot && 
                  oldInnerAngle < currentInnerAngle
                ) {
                  (oldSafeSpotX, oldSafeSpotY, oldSafeSpotZ)
                } else if (
                  distanceBetweenToonAndOldSafeSpot > distanceBetweenToonAndCurrentSafeSpot && 
                  distanceBetweenToonAndOldSafeSpot - distanceBetweenToonAndCurrentSafeSpot < 3 &&
                  oldInnerAngle < currentInnerAngle
                ) {
                  (oldSafeSpotX, oldSafeSpotY, oldSafeSpotZ)
                } else {
                  (newX, newY, newZ)
                }
                
                if (attempts >= 30) {
                  Some(newBestSafeSpot)
                } else {
                  val newBestSafeSpotOpt = Some((attempts + 1, oldInnerAngle, newBestSafeSpot))
                  
                  findSafeSpot(
                    playerX, 
                    playerY, 
                    playerZ, 
                    playerAngle,
                    waypoints.tail, 
                    harmfulAoes, 
                    tries + 1, 
                    newBestSafeSpotOpt
                  )
                }
              case _ =>
                val newBestSafeSpotOpt = Some((1l, currentInnerAngle, (newX, newY, newZ)))
                
                findSafeSpot(
                  playerX, 
                  playerY, 
                  playerZ, 
                  playerAngle,
                  waypoints.tail, 
                  harmfulAoes, 
                  tries + 1, 
                  newBestSafeSpotOpt
                )
            }
          }
        case _ => None
      }
    }  
  }
  
  def runToPoint(
    playerEntity: PlayerEntity,
    targetX: Float,
    targetY: Float,
    targetZ: Float
  )(implicit player: WowClass) = {
    val distanceToTarget = getDistanceBetween3DPoints(
      playerEntity.x,
      targetX,
      playerEntity.y,
      targetY,
      playerEntity.z,
      targetZ
    )
    println(player.name + " in runtopoint")
    if (distanceToTarget < 2) {
      true
    } else {
      val isFacingTarget = facePoint(playerEntity, targetX, targetY, targetZ)
    
      println(s"${player.name} isFacingTarget: $isFacingTarget - distanceToTarget: $distanceToTarget - $targetX - $targetY - $targetZ")
      if (isFacingTarget && distanceToTarget >= 2) {
        futurePlayerLocations.synchronized {
          futurePlayerLocations.add((targetX, targetY, targetZ))
        }
        val runningDuration = ((distanceToTarget / 7) * 1000).milliseconds
        Wow.pressAndReleaseKeystroke(Keys.Up, runningDuration)
        futurePlayerLocations.synchronized {
          futurePlayerLocations.remove((targetX, targetY, targetZ))
        }
        false
      } else if (distanceToTarget >= 2) {
        false
      } else {
        true
      }
    }
  }
  
  def getInnerAndOuterAngles(
    playerX: Float,
    targetX: Float,
    playerY: Float,
    targetY: Float,
    playerZ: Float,
    targetZ: Float,
    playerAngle: Float
  ) = {
    val diffX = targetX - playerX
    val diffY = targetY - playerY
    val facingAngle = Math.atan2(diffY, diffX)
    
    val normalizedFacingAngle = if (facingAngle < 0) {
      (facingAngle + Math.PI * 2).toFloat
    } else if (facingAngle > Math.PI * 2) {
      (facingAngle - Math.PI * 2).toFloat
    } else {
      facingAngle.toFloat
    }
 
    val (minFacingAngle, maxFacingAngle) =
      if (normalizedFacingAngle > playerAngle) {
        (playerAngle, normalizedFacingAngle)
      } else {
        (normalizedFacingAngle, playerAngle)
      }

    val innerAngle = maxFacingAngle - minFacingAngle
    val outerAngle = (2 * Math.PI - maxFacingAngle).toFloat + minFacingAngle
    
    (innerAngle, outerAngle, normalizedFacingAngle)
  }

  def facePoint(
    playerEntity: PlayerEntity,
    targetX: Float,
    targetY: Float,
    targetZ: Float
  )(implicit player: WowClass) = {
    val (innerAngle, outerAngle, normalizedFacingAngle) = getInnerAndOuterAngles(
      playerEntity.x,
      targetX,
      playerEntity.y,
      targetY,
      playerEntity.z,
      targetZ,
      playerEntity.angle
    )
    if (innerAngle < 0.3) {
      true
    } else {        
      if (innerAngle < outerAngle) {
        val turnDuration = ((innerAngle / (2 * Math.PI)) * 2000).milliseconds
        if (normalizedFacingAngle < playerEntity.angle) {
          Wow.pressAndReleaseKeystroke(Keys.Right, turnDuration)
        } else {
          Wow.pressAndReleaseKeystroke(Keys.Left, turnDuration)
        }
      } else {
        val turnDuration = ((outerAngle / (2 * Math.PI)) * 2000).milliseconds
        if (normalizedFacingAngle < playerEntity.angle) {
          Wow.pressAndReleaseKeystroke(Keys.Left, turnDuration)
        } else {
          Wow.pressAndReleaseKeystroke(Keys.Right, turnDuration)
        }
      }
      true
    }
  }
  
  def isInAoe(
    playerEntity: PlayerEntity,
    areaTriggerEntity: AreaTriggerEntity
  )(
    implicit player: WowClass
  ): Boolean = {
    isInAoe(playerEntity.x, playerEntity.y, areaTriggerEntity)
  }
  
  def isInAoe(
    x: Float,
    y: Float,
    areaTriggerEntity: AreaTriggerEntity
  )(
    implicit player: WowClass
  ): Boolean = {
    val d = getDistanceBetween2DPoints(x, areaTriggerEntity.x, y, areaTriggerEntity.y)
    /*println(s"""
      
      name: ${player.name}
      x: $x
      y: $y
      areaTriggerEntity.x : ${areaTriggerEntity.x}
      areaTriggerEntity.y: ${areaTriggerEntity.y}
      
      """)*/
    d < areaTriggerEntity.radius + 1
  }
  
  def getEntities(
    guidToPlayerName: Map[String, String] = Map(),
    nameToPlayerEntity: Map[String, PlayerEntity] = Map(),
    areaTriggerEntities: List[AreaTriggerEntity] = List(),
    guidToUnit: Map[String, WowUnit] = Map(),
    nextEntryOpt: Option[Long] = None,
    currentIteration: Int = 0,
    previousEntries: Set[Long] = Set()
  )(
    implicit player: WowClass
  ): (Map[String, PlayerEntity], List[AreaTriggerEntity], Map[String, WowUnit]) = {
    val hProcess = player.hProcess
    val baseAddress = player.baseAddress
    
    if (currentIteration > 20000) {
      (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
    } else {
      val foundGUIDToPlayerName = if (guidToPlayerName.isEmpty) {
        getUnitNameLookup()
      } else {
        guidToPlayerName
      }
      
      //println(foundGUIDToPlayerName)
      
      val nextEntry: Long = nextEntryOpt match {
        case Some(foundNextEntry) => foundNextEntry
        case _ => 
          val entitiesListBase = Memory.readPointer(
            hProcess, baseAddress + Offsets.EntitiesList.Base
          )

          Memory.readPointer(hProcess, entitiesListBase + Offsets.EntitiesList.FirstEntry)
      }

      if (previousEntries.contains(nextEntry)) {
        (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
      } else if (nextEntry <= 0 || nextEntry == Long.MaxValue) {
        (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
      } else {
        val entryType = Memory.readByte(
          hProcess, nextEntry + Offsets.EntitiesList.Entry.Type
        )
        
        val descriptors: Long = Memory.readPointer(
          hProcess, nextEntry + Offsets.EntitiesList.Entry.Descriptors
        )
          
        val nextNextEntry: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.NextEntry)
        
        if (descriptors > 0) {  
          val (newNameToPlayerEntity, newAreaTriggerEntities, newGUIDToUnit) = entryType match {
            case Configuration.ObjectTypes.NPC =>
              val xAddress = nextEntry + Offsets.EntitiesList.Unit.X
              val yAddress = nextEntry + Offsets.EntitiesList.Unit.Y
              val zAddress = nextEntry + Offsets.EntitiesList.Unit.Z
              val x = Memory.readFloat(hProcess, xAddress)
              val y = Memory.readFloat(hProcess, yAddress)
              val z = Memory.readFloat(hProcess, zAddress)
              val npcName1: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.NPC.Name1)
              val npcName2: Long = Memory.readPointer(hProcess, npcName1 + Offsets.EntitiesList.NPC.Name2)
              val guid = Memory.readGUID(hProcess, nextEntry + Offsets.EntitiesList.Entry.GUID)
              val castingSpellId = Memory.readInt(hProcess, nextEntry + Offsets.EntitiesList.Unit.CastingSpellId)
              val nameOpt = Memory.readStringOpt(hProcess, npcName2)
              //println("NPC: " + Memory.readStringOpt(hProcess, npcName2) + s" - x $x - y $y - z $z")
              
              val targetBase: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.Unit.Target1)
              val targetGUID = Memory.readGUID(hProcess, targetBase + Offsets.EntitiesList.Unit.Target2)
       
              nameOpt match {
                case Some(foundName) =>
                  
                  if (foundName == "Hymdall") {
                    //println("hymdall is casting " + castingSpellId)
                  }
                  
                  val newNPCEntity = NPCEntity(
                    x, 
                    y, 
                    z, 
                    xAddress, 
                    yAddress, 
                    zAddress, 
                    guid, 
                    foundName, 
                    castingSpellId, 
                    targetGUID,
                    nextEntry
                  )
                  
                  (
                    nameToPlayerEntity,
                    areaTriggerEntities,
                    guidToUnit + (guid -> newNPCEntity)
                  )
                case _ =>
                  (
                    nameToPlayerEntity,
                    areaTriggerEntities,
                    guidToUnit
                  )
              }
            case 
              Configuration.ObjectTypes.LocalPlayer | Configuration.ObjectTypes.Player =>
               /*var i = 0
               while (i < 40000) {
                val nextXp = Memory.readInt(hProcess, nextEntry + (i*4))
                if (nextXp == 717000) {
                  println(s"${player.name} nextXp $nextXp - i = $i")
                }
              
                i = i+1
              }*/

              val guid = Memory.readGUID(hProcess, nextEntry + Offsets.EntitiesList.Entry.GUID)
              val xAddress = nextEntry + Offsets.EntitiesList.Unit.X
              val yAddress = nextEntry + Offsets.EntitiesList.Unit.Y
              val zAddress = nextEntry + Offsets.EntitiesList.Unit.Z
              val x = Memory.readFloat(hProcess, xAddress)
              val y = Memory.readFloat(hProcess, yAddress)
              val z = Memory.readFloat(hProcess, zAddress)
              val angleAddress = nextEntry + Offsets.EntitiesList.Unit.Angle
              val angle = Memory.readFloat(hProcess, angleAddress)
              val castingSpellId = Memory.readInt(hProcess, nextEntry + Offsets.EntitiesList.Unit.CastingSpellId)
              
              val nameOpt = if (entryType == Configuration.ObjectTypes.NPC) {
                val npcName1: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.NPC.Name1)
                val npcName2: Long = Memory.readPointer(hProcess, npcName1 + Offsets.EntitiesList.NPC.Name2)
                Memory.readStringOpt(hProcess, npcName2)
              } else {
                foundGUIDToPlayerName.get(guid)
              }
              
              /*var i = 0
              while (i < 10000) {
                val e = Memory.readInt(hProcess, nextEntry + i)
                if (e == 212040 ) {
                  println(s"${player.name} found revitalize for $nameOpt at offset $i")
                }
              
                i = i+1
              }*/
              
              val targetBase: Long = Memory.readPointer(hProcess, nextEntry + Offsets.EntitiesList.Unit.Target1)
              val targetGUID = Memory.readGUID(hProcess, targetBase + Offsets.EntitiesList.Unit.Target2)
              val targetNameOpt = foundGUIDToPlayerName.get(targetGUID)
              
              nameOpt match {
                case Some(foundName) =>
                  val shouldAddPlayerEntity = nameToPlayerEntity.get(foundName) match {
                    case Some(existingUnitPlayerEntity) 
                      if entryType == Configuration.ObjectTypes.Player ||
                         entryType == Configuration.ObjectTypes.LocalPlayer =>
                      true
                    case None => true
                    case _ => false
                  }
                  
                  if (shouldAddPlayerEntity) {
                    val newPlayerEntity = PlayerEntity(
                      x, 
                      y, 
                      z, 
                      xAddress,
                      yAddress,
                      zAddress,
                      angle, 
                      angleAddress,
                      guid, 
                      foundName, 
                      targetNameOpt, 
                      targetGUID,
                      nextEntry,
                      castingSpellId
                    )
                    
                    (
                      nameToPlayerEntity + (foundName -> newPlayerEntity),
                      areaTriggerEntities, 
                      guidToUnit + (guid -> newPlayerEntity)
                    )
                  } else {
                    (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
                  }
                case _ => (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
              }
            case 9 => 
              println("found dynamic")
              
              (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
            case 11 => 
              val spellIdX = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.AreaTrigger.X)
              val spellIdY = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.AreaTrigger.Y)
              val spellIdZ = Memory.readFloat(hProcess, nextEntry + Offsets.EntitiesList.AreaTrigger.Z)
  
              val spellId = Memory.readInt(
                hProcess, nextEntry + Offsets.EntitiesList.AreaTrigger.Base + Offsets.EntitiesList.AreaTrigger.SpellId
              )
              
              val radius = Memory.readFloat(
                hProcess, nextEntry + Offsets.EntitiesList.AreaTrigger.Base + Offsets.EntitiesList.AreaTrigger.Radius
              )
              
              //println(s"spellId $spellId - radius $radius")
              //println(s"spellIdX $spellIdX spellIdY $spellIdY spellIdZ $spellIdZ")
                
              /*if (spellId == 198895) {
                println(s"spellId $spellId - radius $radius")
                println(s"spellIdX $spellIdX spellIdY $spellIdY spellIdZ $spellIdZ")
                var i = 0
                while (i < 10000) {
                  val e = Memory.readFloat(hProcess, nextEntry + (i*4))
                  if (e > 2000 && e < 5000 ) {
                    val f = Memory.readFloat(hProcess, nextEntry + ((i+1)*4))
                    val g = Memory.readFloat(hProcess, nextEntry + ((i+2)*4))
                    println(s"$spellId - value  $e - $f - $g - i = $i")
                  }
                
                  i = i+1
                }
              }*/

              if (Configuration.HarmfulAoes.contains(spellId)) {
                (
                  nameToPlayerEntity, 
                  AreaTriggerEntity(spellId, radius, spellIdX, spellIdY, spellIdZ) +: areaTriggerEntities, 
                  guidToUnit
                )
              } else {
                (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
              }
            case _ => 
              (nameToPlayerEntity, areaTriggerEntities, guidToUnit)
          }
          
          getEntities(
            foundGUIDToPlayerName,
            newNameToPlayerEntity,
            newAreaTriggerEntities,
            newGUIDToUnit,
            Some(nextNextEntry),
            currentIteration + 1,
            previousEntries + nextEntry
          )
        } else {
          getEntities(
            guidToPlayerName,
            nameToPlayerEntity,
            areaTriggerEntities,
            guidToUnit,
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
  
  def isMovingFromMemory(playerEntity: PlayerEntity)(implicit player: WowClass) = {
    val base = Memory.readPointer(player.hProcess, playerEntity.entryBase + Offsets.EntitiesList.Player.UnitSpeed1)
    Memory.readFloat(player.hProcess, base + Offsets.EntitiesList.Player.UnitSpeed2) > 0
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
  
  def getCooldownRemainingTimeOpt(cooldown: Cooldown)(implicit player: WowClass): Option[FiniteDuration] = {
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
    //println(s"red ${color.red} - blue ${color.blue} - green ${color.green}")
    color.red != 255 && color.blue != 255
  }
  
}