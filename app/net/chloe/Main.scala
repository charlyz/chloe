package net.chloe

import com.sun.jna.{ Memory => JnaMemory, _ }
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinUser._
import com.sun.jna.Native
import net.chloe.win32._
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import java.awt.image.BufferedImage
import com.sun.jna.platform.win32.WinDef._
import javax.imageio.ImageIO
import scala.collection.mutable.ListBuffer
import java.io._
import java.awt.image._
import javax.imageio._
import net.chloe.models._
import com.sun.jna.platform.win32.BaseTSD._
import scala.util._

import com.sun.jna.platform.win32.WinDef.HMODULE
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.HHOOK
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT
import com.sun.jna.platform.win32.WinUser._
import com.sun.jna.platform.win32.WinUser.MSG
import net.chloe.wow._
import net.chloe.models.Color
import play.api.Logger
import net.chloe.models.classes._
import net.chloe.models.spells.priest._
import net.chloe.models.spells.druid._
import net.chloe.models.spells._
import net.chloe.win32._
import net.chloe.models.Keys
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import java.util.concurrent.ForkJoinPool
import scala.concurrent._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import java.util.UUID
import akka.actor._
import scala.collection.mutable.{Map => MMap}
import org.joda.time.{Duration => JodaDuration, _}
import java.io._
import net.chloe.models.spells.hov._
import scala.io.Source
import net.chloe.win32.Implicits._

object Main {

  @volatile var combatRotationRunning = false
  @volatile var autoFacingBotRunning = false
  @volatile var stop = false
  val playerToLocationsLookup: MMap[WowClass, Map[String, PlayerEntity]] = MMap()
  val write = new PrintWriter(new FileOutputStream(new File("hov-static-fields-safe-spots.txt"), true))
  val autoFacingForkjoinPool = ExecutionContext.fromExecutor(new ForkJoinPool(64))
  val rotationForkjoinPool = ExecutionContext.fromExecutor(new ForkJoinPool(64))
  
  val hovStaticFieldSafeSpots = Source.fromFile("conf/waypoints/hov-static-fields-safe-spots.txt").getLines
    .map { line =>
      val splitLine = line.split(",")
      (splitLine(0).toFloat, splitLine(1).toFloat, splitLine(2).toFloat)
    }
    .toSet
  
  val waypoints = Source.fromFile("conf/waypoints/hov.txt").getLines
    .map { line =>
      val splitLine = line.split(",")
      (splitLine(0).toFloat, splitLine(1).toFloat, splitLine(2).toFloat)
    }
    .toSet ++ hovStaticFieldSafeSpots
      
  def main(args: Array[String]) = {
    val hWowWindows = Wow.getWindows("MegaBurst")
 
    val players: Map[SpellTargetType, WowClass] = hWowWindows
      .flatMap { hWowWindow =>
        val title = Wow.getWindowTitleImpl(hWowWindow)
        if (title.contains("Ditto")) {
          None
        } /*else if (title.contains("Rooke")) {
          val player = DruidResto(
            name = "Rooke",
            hWowWindow,
            Healer
          )

          val processId = Memory.getProcessIdFromWindowHandle(hWowWindow)
          println("process id " + processId)
          val hProcess = Kernel32.OpenProcess(Memory.PROCESS_ALL_ACCESS, false, processId)
          val baseAddress = Memory.getBaseAddress(hProcess)
          println("base address yo " + baseAddress)
          //println("tadam: " + Memory.readStringOpt(hProcess, baseAddress + Configuration.Offsets.PlayerName))
          val locations = Player.getEntities()(player)
          println(locations)
          println("asdfasdf")
          val a = Wow.capture(Some(500), Some(900), Some(6), Some(6))(player)
          saveImage("bla"+UUID.randomUUID(), a)
          
          /*var i = 45500000
          Thread.sleep(10000)
          println("start")
          while (i < 55479696) {
            val gameUI: Long = Memory.readPointer(hProcess, baseAddress + i)
            if (i % 100000 == 0) println("blub " + i)
            val x = Memory.readFloat(hProcess, gameUI + 0x290)
            //3797.5623,542.4503,603.33765
            if (x <= 3800 && x >= 3790) {
              val y = Memory.readFloat(hProcess, gameUI + 0x294)
              if (y <= 548 && y >= 538) {
                val z = Memory.readFloat(hProcess, gameUI + 0x298)
                if (z <= 606 && z >= 600) {
                  println(s"FOUIND IT $x - $y - $z : $i")
                  i = 55479696
                }
              }
            }
            
            i = i + 1
          }
          println("end")*/

          Some(Healer -> player)
        }*/ else if (title.contains("Clala")) {
          val player = HunterBM(
            name = "Clala",
            hWowWindow,
            DpsFour
          )
          
          Some(DpsFour -> player)
        } else if (title.contains("Monria")) {
          val player = HunterBM(
            name = "Monria",
            hWowWindow,
            DpsOne
          )
          
          Some(DpsOne -> player)
        } else if (title.contains("Mavang")) {
          val player = HunterBM(
            name = "Mavang",
            hWowWindow,
            DpsTwo
          )
          
          Some(DpsTwo -> player)
        } else if (title.contains("Maylva")) {
          val player = HunterBM(
            name = "Maylva",
            hWowWindow,
            DpsThree
          )
          
          Some(DpsThree -> player)
        } else if (title.contains("Feathe")) {
          val player = HunterBM(
            name = "Feathe",
            hWowWindow,
            DpsFive
          )
          
          Some(DpsFive -> player)
        } else {
          None
        }
      }
      .toMap

    implicit val team = Team(players)

    if (team.players.size == 5) {
      //refreshUnitLocationsForever(team)
      //mouseHookStartStop(team)
      keyboardHookStartStop(team)
      
      /*val autoFacingFutures = team.players
        .map { case (_, player) =>
          startAutoFacingBot(waypoints, team)(player)
        }
        .toList*/
      
      Await.result(
        startRotationBot,//Future.sequence(startRotationBot +: autoFacingFutures), 
        Duration.Inf
      )
      ()
    } else {
      Logger.info("Team not found.")
    }
  }

  def refreshUnitLocationsForever(team: Team): Unit = {
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler

    val refreshFutures = Future.traverse(team.players) { case (_, player) =>
      Future {
        blocking {
          val start = DateTime.now
          implicit val implicitPlayer = player
          val (unitNameToLocation, _, _) = Player.getEntities()
          playerToLocationsLookup.synchronized {
            playerToLocationsLookup(player) = unitNameToLocation
          }
          println("get location for " + player.name + " in " + (DateTime.now.getMillis - start.getMillis) + " millis")
        }
      }
    }
    
    Await.result(refreshFutures, 5.seconds)
    
    scheduler.scheduleOnce(25.millis) {
      refreshUnitLocationsForever(team)
    }
  }

  def mouseHookStartStop(team: Team) = {
    Future {
      blocking {
        var hhk: HHOOK = null
        val hMod = Kernel32.GetModuleHandle(null)
        var lastLeftClick = DateTime.now
        val mouseHook = new LowLevelMouseProc() {
          def callback(nCode: Int, wParam: WPARAM, info: MSLLHOOKSTRUCT): LRESULT = {
            if (nCode >= 0) {
              wParam.intValue() match {
               case MouseActions.WM_LBUTTONDOWN => Wow.clickToMoveSlaves(team)
               case _ =>
              }
            }
            User32.CallNextHookEx(hhk, nCode, wParam, info.getPointer)
          } 
        }
        
        hhk = User32.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseHook, hMod, 0)
        User32.GetMessage(new MSG(), null, 0, 0)
      }
    }
  }
  
  def keyboardHookStartStop(team: Team) = {
    Future {
      blocking {
        var hhk: HHOOK = null
        val hMod = Kernel32.GetModuleHandle(null)
        val keyboardHook = new LowLevelKeyboardProc() {
          def callback(nCode: Int, wParam: WPARAM, info: KBDLLHOOKSTRUCT): LRESULT = {
            if (nCode >= 0) {
              wParam.intValue() match {
                case WinUser.WM_KEYDOWN =>
                  if (info.vkCode == Keys.D1) {
                    combatRotationRunning = !combatRotationRunning
                    //autoFacingBotRunning = !autoFacingBotRunning
                    team.players.foreach { 
                      case (spellTargetType, player) if player.isInstanceOf[HunterBM] =>
                        if (!combatRotationRunning) {
                          // Put pet in passive mode.
                          Wow.pressAndReleaseKeystrokes(List(Keys.Alt, Keys.F6))(player)
                          Wow.pressAndReleaseKeystrokes(List(Keys.Alt, Keys.F6))(player)
                          Wow.pressAndReleaseKeystrokes(List(Keys.Alt, Keys.F6))(player)
                        } else {
                          // Put pet in assist mode.
                          Wow.pressAndReleaseKeystrokes(List(Keys.LShiftKey, Keys.T))(player)
                        }
                      case _ =>
                    }
                    
                    Logger.info(s"Combat rotation running: $combatRotationRunning")
                  } else if (info.vkCode == Keys.T) {
                    //autoFacingBotRunning = !autoFacingBotRunning
                    //Logger.info(s"Auto-facing bot running: $autoFacingBotRunning")
                  }
                case _ =>
              }
            }

            val peer = Pointer.nativeValue(info.getPointer())
            return User32.CallNextHookEx(hhk, nCode, wParam, new LPARAM(peer))
          }
        }

        hhk = User32.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0)
        User32.GetMessage(new MSG(), null, 0, 0)
      }
    }
  }

  def startAutoFacingBot(waypoints: Set[(Float, Float, Float)], team: Team)(implicit  player: WowClass) = Future {
    while (!stop) { 
      if (autoFacingBotRunning) {
        Try {
          if (Player.isPrimary) {
          //if (player.name != "Cayla") {
            Thread.sleep(1000)
            ()
          } else {
            val isToonFacingTarget = {
              val (
                nameToPlayerEntityForTarget, 
                harmfulAoesFromEntities, 
                guidToUnit
              ) = Player.getEntities()
 
              nameToPlayerEntityForTarget.get(player.name) match {
                case Some(playerEntity) =>      
                  val hybridAoes = guidToUnit.values
                    .flatMap { unit =>
                      //println(unit.name + " targets " + unit.targetGUID + " and cast " + unit.castingSpellId)
                      
                      /*if (
                       unit.guid == playerEntity.targetGUID
                      ) {
                        val d = Player.getDistanceBetween3DPoints(
                          playerEntity.x,
                          unit.x,
                          playerEntity.y,
                          unit.y,
                          playerEntity.z,
                          unit.z
                        )
                        //println("distance betwee " + player.name +  " and " + unit.name + " - d " + d)
                        println("start")
                        var i = 6000
                        while (i < 7000) {
                          //
                          val a: Long = Memory.readPointer(player.hProcess, unit.entryBase + i)
                          /*var j = 0
                          while (j < 1000) {
                            val e = Memory.readGUID(player.hProcess, a + j)
                            if (guidToUnit.get(e).isDefined && guidToUnit(e).name == "Maylva") {
                              println(unit.name + " i:" + i + " - j:" + j + " -  target is " + guidToUnit(e).name)
                            }
                            j = j + 1
                          }*/
                          
                          val g = Memory.readInt(player.hProcess, unit.entryBase + i)
                          /*if (g != 0) {
                            println(s"${player.name} found spell $g for ${unit.name} at offset $i")
                          }*/
                          if (g == 198605 || g == 198595) {
                            println(s"${player.name} found $g for ${unit.name} at offset $i")
                          }
                        
                          i = i+1
                        }
                        println("end")
                      }*/
                      
                     if (unit.name == Configuration.HydridNPCsAreaTriggers.CracklingStorm) {
                        val areaTriggerEntity = AreaTriggerEntity(
                          id = Configuration.Aoes.CracklingStorm,
                          radius = 8,
                          unit.x,
                          unit.y,
                          unit.z
                        )
                        val d = Player.getDistanceBetween3DPoints(
                          playerEntity.x,
                          unit.x,
                          playerEntity.y,
                          unit.y,
                          playerEntity.z,
                          unit.z
                        )
                        println(player.name + " is " + d + " yards away from NPC CracklingStorm " + areaTriggerEntity)
                        List(areaTriggerEntity)
                      } else if (unit.name == Configuration.HydridNPCsAreaTriggers.DancingBlade) {
                        val areaTriggerEntity = AreaTriggerEntity(
                          id = Configuration.Aoes.DancingBlade,
                          radius = 10,
                          unit.x,
                          unit.y,
                          unit.z
                        )
                        val d = Player.getDistanceBetween3DPoints(
                          playerEntity.x,
                          unit.x,
                          playerEntity.y,
                          unit.y,
                          playerEntity.z,
                          unit.z
                        )
                        println(player.name + " is " + d + " yards away from NPC DANCING BLADE " + areaTriggerEntity)
                        List(areaTriggerEntity)
                      } else if (
                        unit.castingSpellId == Configuration.Aoes.CracklingStorm ||
                        unit.castingSpellId == Configuration.Aoes.Thunderstrike ||
                        unit.castingSpellId == 212040 // revitalize
                      ) {
                        guidToUnit.get(unit.targetGUID) match {
                          case Some(targetUnit) =>
                            println(unit.name + " is casting " + unit.castingSpellId + " - target guid is " + targetUnit.name)
                            List(
                              AreaTriggerEntity(
                                unit.castingSpellId,
                                radius = 8,
                                targetUnit.x,
                                targetUnit.y,
                                targetUnit.z
                              )
                            )
                          case _ => List()
                        }
                      } else if (unit.castingSpellId == Configuration.CastingSpells.DancingBlade) {
                          println(unit.name + " is casting " + unit.castingSpellId + " - " + player.name + " runs!")
                          team.players
                            .flatMap { case (_, player) =>
                              nameToPlayerEntityForTarget.get(player.name) match {
                                case Some(playerEntity) =>
                                  Some(
                                    AreaTriggerEntity(
                                      unit.castingSpellId,
                                      radius = 10,
                                      playerEntity.x,
                                      playerEntity.y,
                                      playerEntity.z
                                    )
                                  )
                                case _ => None   
                              }
                            }
                            .toList
                      } else if (unit.castingSpellId == Configuration.CastingSpells.HornOfValor) {
                        println(unit.name + " is casting HornOfValor - " + player.name + " goes to safe spot")
                        val areaTriggerEntity = AreaTriggerEntity(
                          unit.castingSpellId,
                          radius = 1,
                          playerEntity.x,
                          playerEntity.y,
                          playerEntity.z,
                          safeSpotsOpt = Some(hovStaticFieldSafeSpots)
                        )
                        List(areaTriggerEntity)
                      } else {
                        List()
                      }
                    }
                    .toList

                  val harmfulAoesWithHybridNPCs = hybridAoes ++ harmfulAoesFromEntities
               
                  val harmfulAoes = if (Wow.foundSentinel) {
                    if (Player.hasDebuff(Thunderstrike)) {
                      val thunderstrikeEntitiesForPlayersLocations = team.players.keys
                        .flatMap { case spellTargetType =>
                          if (spellTargetType != player.spellTargetType) {
                            nameToPlayerEntityForTarget.get(player.name) match {
                              case Some(playerEntity) =>
                                Some(
                                  AreaTriggerEntity(
                                    id = Thunderstrike.id,
                                    radius = Thunderstrike.radiusOpt.get,
                                    playerEntity.x,
                                    playerEntity.y,
                                    playerEntity.z
                                  )
                                )
                              case _ => None
                            }
                          } else {
                            None
                          }
                        }
                        .toList 
                        
                      val thunderstrikeEntitiesForFuturePlayersLocations = Player.futurePlayerLocations
                        .map { case (futureX, futureY, futureZ) =>
                          AreaTriggerEntity(
                            id = Thunderstrike.id,
                            radius = Thunderstrike.radiusOpt.get,
                            futureX,
                            futureY,
                            futureZ
                          )
                        }
                        .toList 
                        
                      harmfulAoesWithHybridNPCs ++ 
                        thunderstrikeEntitiesForPlayersLocations ++
                        thunderstrikeEntitiesForFuturePlayersLocations
                    } else {
                      harmfulAoesWithHybridNPCs
                    }
                  } else {
                    harmfulAoesWithHybridNPCs
                  }

                  val isInAtLeastOneHarmfulAoe = harmfulAoes
                    .find { areaTriggerEntity => 
                      Player.isInAoe(playerEntity, areaTriggerEntity)
                    }
                    .isDefined

                  val isPlayerMoving = Player.isMovingFromMemory(playerEntity)
                  
                  val shouldTryAutoFacing = true/*if (isPlayerMoving) {
                    false
                  } else if (isInAtLeastOneHarmfulAoe) {
                    Player.findSafeSpot(
                      playerEntity.x, 
                      playerEntity.y, 
                      playerEntity.z,
                      playerEntity.angle,
                      waypoints, 
                      harmfulAoes,
                      bestSafeSpotOpt = None
                    ) match {
                      case Some((ctmX, ctmY, ctmZ)) =>
                        //Wow.clickToMoveSlave(ctmX, ctmY, playerEntity.z, team)
                        //false
                        Player.runToPoint(playerEntity, ctmX, ctmY, ctmZ)
                      case _ => true
                    }
                  } else {
                    true
                  }*/
                  
                  if (!shouldTryAutoFacing) {
                    if (isInAtLeastOneHarmfulAoe) {
                      false
                    } else {
                      true
                    }
                  } else {
                    if (playerEntity.guid != playerEntity.targetGUID) {
                      guidToUnit.get(playerEntity.targetGUID) match {
                        case Some(targetEntity) =>
                          if (Wow.isCtmInProgress.get) {
                            true
                          } else {
                            Player.facePoint(playerEntity, targetEntity.x, targetEntity.y, targetEntity.z)
                          }
                        case _ => true
                      }
                    } else {
                      true
                    }
                  }
                case _ => true
              }
            }

            //println(player.name + " isToonFacingTarget " + isToonFacingTarget)
            if (isToonFacingTarget) {
              Thread.sleep(200)
            } else {
              Thread.sleep(50)
            }
          } 
        } match {
          case Success(_) =>
          case Failure(e) =>
            Logger.error("Unexpected error", e)
            Thread.sleep(1000)
        }
      } else {
        Thread.sleep(1000)
      }
    }
  }(autoFacingForkjoinPool)

  def startRotationBot(implicit team: Team) = Future {
    while (!stop) {
      if (combatRotationRunning) {
        Try {
          val actionFutures = Future.traverse(team.players) {
            case (spellTargetType, player) =>
              implicit val implicitPlayer = player
              
              /*if (Player.isPrimary) {
                val (nameToPlayerEntity, _, _) = Player.getEntities()
                nameToPlayerEntity.get(player.name) match {
                  case Some(playerEntity) =>
                    write.println(s"${playerEntity.x},${playerEntity.y},${playerEntity.z}")
                    write.flush()
                  case _ =>
                }
              }*/

              Future {
                blocking {
                  val currentResolution = Wow.getWindowSize
                  val nextScalingAction = Wow.nextScalingAction

                  nextScalingAction match {
                    case ScaleUp =>
                      Logger.debug(s"Rescaling detected - Scaling up - $currentResolution")
                      Wow.pressAndReleaseKeystroke(Keys.F8)
                      //Wow.pressAndReleaseKeystrokes(List(Keys.LControlKey, Keys.U))
                    case ScaleDown =>
                      Logger.debug(s"Rescaling detected - Scaling down - $currentResolution")
                      Wow.pressAndReleaseKeystroke(Keys.F7)
                      //Wow.pressAndReleaseKeystrokes(List(Keys.LControlKey, Keys.U))
                    case NoScaling =>
                      player.executeNextAction
                  }
                }
              }
          }
          Await.result(actionFutures, 25.seconds)

          Thread.sleep(Configuration.PauseBetweenActions.toMillis)
        } match {
          case Success(_) =>
          case Failure(e) =>
            Logger.error("Unexpected error", e)
            Thread.sleep(1000)
        }
      } else {
        Thread.sleep(1000)
      }
    }
  }(rotationForkjoinPool)

  def saveImage(imageName: String, image: BufferedImage) = {
    val outputfile = new File(s"$imageName.jpg")
    ImageIO.write(image, "jpg", outputfile)
  }

  def debugFlags(implicit player: WowClass) = {
    Logger.debug(
      s"""
      | Player.getHealthPercentage: ${Player.getHealthPercentage} 
      | Player.isInCombat: ${Player.isInCombat} 
      | Player.isChanneling: ${Player.isChanneling} 
      | Player.isCasting: ${Player.isCasting} 
      | Player.hasTarget: ${Player.hasTarget} 
      | Player.getLastSpellCastedIdOpt: ${Player.getLastSpellCastedIdOpt} 
      | Player.getLastSpellCastedOpt: ${Player.getLastSpellCastedOpt} 
      | Player.getEnnemiesCountInRange: ${Player.getEnnemiesCountInRange} 
      | Player.areNameplatesOn: ${Player.areNameplatesOn} 
      | Player.hasPet: ${Player.hasPet} 
      | Player.getPowerPercentage: ${Player.getPowerPercentage} 
      | Player.getHastePercentage: ${Player.getHastePercentage} 
      | Player.isAutoAttacking: ${Player.isAutoAttacking} 
      | Player.isMoving: ${Player.isMoving} 
      | Player.isMounted: ${Player.isMounted} 
      | Player.isOutdoor: ${Player.isOutdoor} 
      |
      | Target.getHealthPercentage: ${Target.getHealthPercentage} 
      | Target.isBoss: ${Target.isBoss} 
      | Target.getCastingSpellIdOpt: ${Target.getCastingSpellIdOpt} 
      | Target.isCasting: ${Target.isCasting} 
      | Target.isCastingAndSpellIsInterruptible: ${Target.isCastingAndSpellIsInterruptible} 
      | Target.getCastingPercentage: ${Target.getCastingPercentage} 
      | Target.isVisible: ${Target.isVisible} 
      | Target.isFriend: ${Target.isFriend} 
      | Target.isPlayer: ${Target.isPlayer} 
      | Target.getDebuffRemainingTimeOpt(ShadowWordPain): ${Target.getDebuffRemainingTimeOpt(ShadowWordPain)} 
      |
      | Pet.getHealthPercentage: ${Pet.getHealthPercentage} 
      | 
      |
      """.stripMargin)
  }

}