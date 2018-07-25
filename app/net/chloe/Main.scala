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
import scala.concurrent._
import scala.concurrent.duration._
import java.util.UUID
import akka.actor._
import scala.collection.mutable.{Map => MMap}
import org.joda.time.{Duration => JodaDuration, _}

object Main {

  @volatile var combatRotationRunning = false
  @volatile var autoFacingBotRunning = false

  @volatile var stop = false
  val playerToLocationsLookup: MMap[WowClass, Map[String, Location]] = MMap()

  def main(args: Array[String]) = {
    val hWowWindows = Wow.getWindows("Ultimate")

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
          val locations = Player.getUnitLocations()(player)
          println(locations)
          //println("asdfasdf")
          //val a = Wow.capture(Some(500), Some(900), Some(6), Some(6))(player)
          //saveImage("bla"+UUID.randomUUID(), a)

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
      Await.result(Future.sequence(List(startAutoFacingBot, startRotationBot)), Duration.Inf)
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
          val unitNameToLocation = Player.getUnitLocations()
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
        @volatile var isMoving = false
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

  def startAutoFacingBot(implicit team: Team) = Future {
    while (!stop) {
      if (autoFacingBotRunning) {
        Try {
          val autoFacingFutures = Future.traverse(team.players) {
            case (spellTargetType, player) =>
              implicit val implicitPlayer = player

              if (Player.isPrimary) {
              //tif (player.name != "Cayla") {
                Future.successful(())
              } else {
                Future {
                  blocking {
                    val unitNameLocationForTarget = Player.getUnitLocations()
              
                    unitNameLocationForTarget.get(player.name) match {
                      case Some(playerLocation) =>
                        playerLocation.targetNameOpt match {
                          case Some(targetName) if player.name != targetName =>
                            unitNameLocationForTarget.get(targetName) match {
                              case Some(targetLocation) =>
                                val diffX = targetLocation.x - playerLocation.x
                                val diffY = targetLocation.y - playerLocation.y
                                val facingAngle = Math.atan2(diffY, diffX)

                                val normalizedFacingAngle = if (facingAngle < 0) {
                                  facingAngle + Math.PI * 2
                                } else if (facingAngle > Math.PI * 2) {
                                  facingAngle - Math.PI * 2
                                } else {
                                  facingAngle
                                }

                                
                                val (minFacingAngle, maxFacingAngle) =
                                  if (normalizedFacingAngle > playerLocation.angle) {
                                    (playerLocation.angle.toDouble, normalizedFacingAngle)
                                  } else {
                                    (normalizedFacingAngle, playerLocation.angle.toDouble)
                                  }

                                val innerAngle = maxFacingAngle - minFacingAngle
                                val outerAngle = (2 * Math.PI - maxFacingAngle) + minFacingAngle

                                if ((playerLocation.angle - normalizedFacingAngle).abs < 0.3) {
                                  Future.successful(())
                                } else {
                                  if (innerAngle < outerAngle) {
                                    val turnDuration =  50.milliseconds

                                    if (normalizedFacingAngle < playerLocation.angle) {
                                      println(s"RIGHT1 ${player.name} ${targetName} - playerLocation " + playerLocation + " - targetLocation: " + targetLocation + " - ttarget angle " + normalizedFacingAngle)
                                      Wow.pressAndReleaseKeystroke(Keys.Right, turnDuration)
                                    } else {
                                      println(s"LEFT1 ${player.name} ${targetName} - playerLocation " + playerLocation + " - targetLocation: " + targetLocation + " - ttarget angle " + normalizedFacingAngle)
                                      Wow.pressAndReleaseKeystroke(Keys.Left, turnDuration)
                                    }
                                  } else {
                                    val turnDuration = 50.milliseconds

                                    if (normalizedFacingAngle < playerLocation.angle) {
                                      println(s"RIGHT2 ${player.name} ${targetName} - playerLocation " + playerLocation + " - targetLocation: " + targetLocation + " - ttarget angle " + normalizedFacingAngle)
                                      Wow.pressAndReleaseKeystroke(Keys.Left)
                                    } else {
                                      println(s"LEFT12 ${player.name} ${targetName} - playerLocation " + playerLocation + " - targetLocation: " + targetLocation + " - ttarget angle " + normalizedFacingAngle)
                                      Wow.pressAndReleaseKeystroke(Keys.Right)
                                    }
                                  }
                                }
                              case _ => Future.successful(())
                            }
                          case _ => Future.successful(())
                        }
                      case _ => Future.successful(())
                    }
                  }
                }
              }
          }
          Await.result(autoFacingFutures, 10.seconds)

          Thread.sleep(50)
        } match {
          case Success(_) =>
          case Failure(e) =>
            Logger.error("Unexpected error", e)
            Thread.sleep(1000)
        }
      } else {
        Thread.sleep(100)
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

              Future {
                blocking {
                  val currentResolution = Wow.getWindowSize
                  val nextScalingAction = Wow.nextScalingAction

                  nextScalingAction match {
                    case ScaleUp =>
                      Logger.debug(s"Rescaling detected - Scaling up - $currentResolution")
                      Wow.pressAndReleaseKeystroke(Keys.F8)
                      Wow.pressAndReleaseKeystrokes(List(Keys.LControlKey, Keys.U))
                    case ScaleDown =>
                      Logger.debug(s"Rescaling detected - Scaling down - $currentResolution")
                      Wow.pressAndReleaseKeystroke(Keys.F7)
                      Wow.pressAndReleaseKeystrokes(List(Keys.LControlKey, Keys.U))
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