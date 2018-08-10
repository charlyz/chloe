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
  val write = new PrintWriter(new FileOutputStream(new File("waypoints.txt"), true))
  val autoFacingForkjoinPool = ExecutionContext.fromExecutor(new ForkJoinPool())
  
  def main(args: Array[String]) = {
    val hWowWindows = Wow.getWindows("Ultimate")
    
    val waypoints = Source.fromFile("conf/waypoints/hov.txt").getLines
      .map { line =>
        val splitLine = line.split(",")
        (splitLine(0).toFloat, splitLine(1).toFloat, splitLine(2).toFloat)
      }
      .toSet

    val players: Map[SpellTargetType, WowClass] = hWowWindows
      .flatMap { hWowWindow =>
        val title = Wow.getWindowTitleImpl(hWowWindow)

        if (title.contains("Ditto")) {
          None
        } else if (title.contains("Rooke")) {
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
          //println("asdfasdf")
          //val a = Wow.capture(Some(500), Some(900), Some(6), Some(6))(player)
          //saveImage("bla"+UUID.randomUUID(), a)
          
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
        } else if (title.contains("Cayla")) {
          val player = DruidGuardian(
            name = "Cayla",
            hWowWindow,
            Tank
          )
          
          Some(Tank -> player)
        } else {
          None
        }
      }
      .toMap

    implicit val team = Team(players)

    if (team.players.size == 5) {
      //refreshUnitLocationsForever(team)
      mouseHookStartStop(team)
      keyboardHookStartStop()
      Await.result(Future.sequence(List(startAutoFacingBot(waypoints), startRotationBot)), Duration.Inf)
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
  
  def keyboardHookStartStop() = {
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
                    Logger.info(s"Combat rotation running: $combatRotationRunning")
                  } else if (info.vkCode == Keys.T) {
                    autoFacingBotRunning = !autoFacingBotRunning
                    Logger.info(s"Auto-facing bot running: $autoFacingBotRunning")
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

  def startAutoFacingBot(waypoints: Set[(Float, Float, Float)])(implicit team: Team) = Future {
    while (!stop) { 
      if (autoFacingBotRunning) {
        Try {
          val autoFacingFutures = Future.traverse(team.players) {
            case (spellTargetType, player) =>
              implicit val implicitPlayer = player

              if (Player.isPrimary) {
              //if (player.name != "Cayla") {
                Future.successful(true)
              } else {
                Future {
                  blocking {
                    val (
                      nameToPlayerEntityForTarget, 
                      harmfulAoesFromEntities, 
                      guidToEntityLocation
                    ) = Player.getEntities()
 
                    nameToPlayerEntityForTarget.get(player.name) match {
                      case Some(playerEntity) =>
                        val harmfulAoesWithTargetCasting = Target.getCastingSpellIdOpt match {
                          case Some(spellId) =>
                            playerEntity.targetGUIDOpt match {
                                case Some(targetGUID) if playerEntity.guid == targetGUID =>
                                  if (spellId == Configuration.Aoes.DancingBlade) {
                                    val dancingBladeEntity = AreaTriggerEntity(
                                      id = Configuration.Aoes.DancingBlade,
                                      radius = 6,
                                      playerEntity.x,
                                      playerEntity.y,
                                      playerEntity.z
                                    )
                                    harmfulAoesFromEntities :+ dancingBladeEntity
                                  } else {
                                    harmfulAoesFromEntities
                                  }
                                case _ => harmfulAoesFromEntities
                            }
                          case _ => harmfulAoesFromEntities
                        }
                        
                       val hybridNPCAoes = guidToEntityLocation.values
                         .flatMap { entityLocation =>
                           if (entityLocation.name == Configuration.HydridNPCsAreaTriggers.CracklingStorm) {
                             Some(
                               AreaTriggerEntity(
                                 id = Configuration.Aoes.CracklingStorm,
                                 radius = 3,
                                 entityLocation.x,
                                 entityLocation.y,
                                 entityLocation.z
                               )
                             )
                           } else {
                             None
                           }
                         }
                         .toList
                        
                       val harmfulAoesWithHybridNPCs = harmfulAoesWithTargetCasting.filter { 
                           harmfulAoeWithTargetCasting =>
                             // Remove area trigger entity for crackling storm because we
                             // want to the NPC location.
                             
                             harmfulAoeWithTargetCasting.id != Configuration.Aoes.CracklingStorm
                         } ++ hybridNPCAoes
                       
                        val harmfulAoes = if (Wow.foundSentinel) {
                          if (Player.hasDebuff(Thunderstrike)) {
                            val thunderstrikeEntities = team.players.keys
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
                            harmfulAoesWithHybridNPCs ++ thunderstrikeEntities
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
                        val shouldTryAutoFacing = if (isPlayerMoving) {
                          false
                        } else if (isInAtLeastOneHarmfulAoe) {
                          Player.findSafeSpot(
                            playerEntity.x, 
                            playerEntity.y, 
                            playerEntity.z,
                            waypoints, 
                            harmfulAoes
                          ) match {
                            case Some((ctmX, ctmY, ctmZ)) =>
                              //Wow.clickToMoveSlave(ctmX, ctmY, playerEntity.z, team)
                              //false
                              Player.runToPoint(playerEntity, ctmX, ctmY, ctmZ)
                            case _ => true
                          }
                        } else {
                          true
                        }
                        
                        if (!shouldTryAutoFacing) {
                          if (isInAtLeastOneHarmfulAoe) {
                            false
                          } else {
                            true
                          }
                        } else {
                          playerEntity.targetGUIDOpt match {
                            case Some(targetGUID) if playerEntity.guid != targetGUID =>
                              guidToEntityLocation.get(targetGUID) match {
                                case Some(targetEntity) =>
                                  if (Wow.isCtmInProgress.get) {
                                    true
                                  } else {
                                    Player.facePoint(playerEntity, targetEntity.x, targetEntity.y, targetEntity.z)
                                  }
                                case _ => true
                              }
                            case _ => true
                          }
                        }
                      case _ => true
                    }
                  }
                }(autoFacingForkjoinPool)
              }
          }
          val isOneToonNotFacingTarget = Await
            .result(autoFacingFutures, 10.seconds)
            .find(!_)
            .isDefined
            
          if (isOneToonNotFacingTarget) {
            Thread.sleep(10)
          } else {
            Thread.sleep(1000)
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
  }

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
          Await.result(actionFutures, 5.seconds)

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
  }

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