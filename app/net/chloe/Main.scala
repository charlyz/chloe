package net.chloe

import com.sun.jna.Memory
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
import net.chloe.models.Keys
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent._
import scala.concurrent.duration._

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
            Healer,
            Color(3547135)
          )
          Some(Healer -> player)
        } else if (title.contains("Monria")) {
          val player = HunterBM(
            name = "Monria",
            hWowWindow,
            DpsOne,
            Color(16741174)
          )
          Some(DpsOne -> player)
        } else if (title.contains("Mavang")) {
          val player = HunterBM(
            name = "Mavang",
            hWowWindow,
            DpsTwo,
            Color(16725558)
          )
          Some(DpsTwo -> player)
        } else if (title.contains("Maylva")) {
          val player = HunterBM(
            name = "Maylva",
            hWowWindow,
            DpsThree,
            Color(3604278)
          )
          Some(DpsThree -> player)
        } else if (title.contains("Cayla")) {
          val player = DruidGuardian(
            name = "Cayla",
            hWowWindow,
            Tank,
            Color(14235391)
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
  
  def findMobOnScreenOpt(
    x: Int, y: Int, width: Long, height: Long, screenshot: BufferedImage, targetColor: Color
  )(
    implicit player: WowClass
  ): Option[(Long, Long)] = {
    val color = Color(screenshot.getRGB(x, y))
    
    if (color == targetColor) {
      Some((x, y))
    } else {
      if (x == width - 1) {
        if (y == height - 1) {
          None
        } else {
          findMobOnScreenOpt(0, y + 1, width, height, screenshot, targetColor)
        }
      } else {
        findMobOnScreenOpt(x + 1, y, width, height, screenshot, targetColor)
      }
    }
  }
  
  def findFirstMobOpt(mobs: List[(String, Color)])(implicit player: WowClass): Option[(String, (Long, Long))] ={
    mobs.headOption match {
      case Some((nextMobName, nextMobColor)) =>
        val (width, height) = Wow.getWindowSize
        val screenshot = Wow.capture(
          widthOpt = Some(width),
          heightOpt = Some(height),
          columnOpt = Some(0),
          rowOpt = Some(0)
        )
    
        findMobOnScreenOpt(0, 0, width, height, screenshot, nextMobColor) match {
          case Some(mobCoordinates) => Some(nextMobName, mobCoordinates)
          case _ => 
            val nextMobs = mobs.tail
            
            if (nextMobs.isEmpty) {
              None
            } else {
              findFirstMobOpt(nextMobs)
            }
        }
      case _ => None
    }
  }
  
  def startDirectionBot(implicit team: Team) = Future {
    while (!stop) {
      if (directionBotRunning) {
        val directPlayersFutures = Future.traverse(team.players) { case (_, player) =>
          Future {
            blocking {
              implicit val me = player
              
              if (Wow.isSlave) {
                findFirstMobOpt(Configuration.Nameplates.PriorityMobs.toList) match {
                  case Some((mobName, (x, y))) => 
                    if (x >= 0 && x <= 460) {
                      //Logger.info(s"${player.name} - Turn right to center $mobName.")
                      Wow.pressAndReleaseKeystroke(Keys.Left)
                    } else if ((x >= 500 && x <= 960)) {
                      //Logger.info(s"${player.name} - Turn left to center $mobName.")
                      Wow.pressAndReleaseKeystroke(Keys.Right)
                    } else {
                      //Logger.info(s"${player.name} - $mobName is centered.")
                    }
                  case _ => //Logger.info(s"1t${player.name} - Found no priority mob.")
                }
              }
            }
          }
        }
        /*implicit val mavang = team.getPlayer(DpsTwo)
        val (width, height) = Wow.getWindowSize

        if (Wow.isSlave) {
          Logger.info("Looking for Monria...")
          val screenshot = Wow.capture(
            widthOpt = Some(width),
            heightOpt = Some(height),
            columnOpt = Some(0),
            rowOpt = Some(0)
          )
1t
          val monriaPositionOpt = findMonria(0, 0, width, height, screenshot)
          
          monriaPositionOpt match {
            case Some((x, y)) =>
              if (x >= 0 && x <= 460) {
                Logger.info("Turn right to center Monria.")
                Wow.pressAndReleaseKeystroke(Keys.Left)
              } else if ((x >= 500 && x <= 960)) {
                Logger.info("Turn left to center Monria.")
                Wow.pressAndReleaseKeystroke(Keys.Right)
              } else {
                Logger.info("Monria is centered.")
              }
            case _ =>
          }
          
          Logger.info(s"Monria is there: $monriaPositionOpt")1t
        } else {
          Logger.info("mavang not slave")
        }*/
        
        Await.result(directPlayersFutures, Duration.Inf)
      }
      Thread.sleep(1000)
    }
  }
  
  def startRotationBot(implicit team: Team) = Future {
    while (!stop) {
      if (combatRotationRunning) {
        Try {
          val actionFutures = Future.traverse(team.players) { case (spellTargetType, player) =>
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
                    //player.executeNextAction
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

/*

val isInCombat = Try(
  Wow.capture(
    widthOpt = Some(1),
    heightOpt = Some(1),
    columnOpt = Some(4),
    rowOpt = Some(1))) match {
    case Success(pixel) =>
      val color = Color(pixel.getRGB(0, 0))
      val isInCombat = color.isRed
      Logger.debug(
        s"IsInCombat: $isInCombat - Colors are red: ${color.red} " +
          s" - green: ${color.green} - blue: ${color.blue} ")

      isInCombat
    case Failure(e) =>
      Logger.debug("Could not capture a screenshot of the window.", e)
      false
  }

Try(
  Wow.capture(
    widthOpt = Some(1),
    heightOpt = Some(1),
    columnOpt = Some(1),
    rowOpt = Some(1))) match {
    case Success(pixel) =>
      val color = pixel.getRGB(0, 0)
      val red = (color & 0xff0000) / 65536
      val green = (color & 0xff00) / 256
      val blue = (color & 0xff)
      val health = (red.toFloat / 255 * 100).round

      Logger.debug(s"Health: $health - Colors are red: $red - green: $green - blue: $blue ")

      if (isInCombat && health < 80) {
        Logger.debug(s"Attacking target - health too low!")
        Target.getDebuffRemainingTimeOpt(ShadowWordPain) match {
          case Some(_) => Wow.pressAndReleaseKeystrokes(List(Keys.C))
          case _ => Wow.pressAndReleaseKeystrokes(List(Keys.LShiftKey, Keys.F1))
        }
      }
    case Failure(e) =>
      Logger.debug("Could not capture a screenshot of the window.", e)
  }
*/