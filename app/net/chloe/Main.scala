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
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.HHOOK
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc
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

object Main {

  @volatile var combatRotationRunning = false
  @volatile var directionBotRunning = false
  
  @volatile var stop = false

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
          println(hWowWindow + " e e ")
          val processId = Memory.getProcessIdFromWindowHandle(hWowWindow)
          println("process id " + processId)
          val hProcess = Kernel32.OpenProcess(Memory.PROCESS_ALL_ACCESS, false, processId)
          // 140697018564608
          val baseAddress = Memory.getBaseAddress(hProcess)
          println("base address yo " + baseAddress)
          val bytesToRead = 8 * 60
          val playerNameMemory = Memory.readMemory(
            hProcess.getPointer, 
            baseAddress + 0x01C7ED80, 
            bytesToRead
          )
          //val a = playerNameMemory.getByteArray(0, 480)
          println("tadam: " + playerNameMemory.getString(0))
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
      hookStartStop()
      Await.result(Future.sequence(List(startDirectionBot, startRotationBot)), Duration.Inf)
      ()
    } else {
      Logger.info("Team not found.")
    }
  }

  def hookStartStop() = {
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
                    Logger.info(s"Combat rotation running: $combatRotationRunning")
                  } else if (info.vkCode == Keys.T) {
                    directionBotRunning = !directionBotRunning
                    Logger.info(s"Direction bot running: $directionBotRunning")
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

  def startDirectionBot(implicit team: Team) = Future {
    while (!stop) {
      if (directionBotRunning) {
        Future {

        }
      }
      Thread.sleep(1000)
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
          1
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