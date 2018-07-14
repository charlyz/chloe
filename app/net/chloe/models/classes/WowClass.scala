package net.chloe.models.classes

import net.chloe.models.spells._
import net.chloe.models.auras._
import net.chloe.models._
import scala.collection.mutable.{ HashMap => MHashMap }
import scala.concurrent.duration._
import com.sun.jna.platform.win32.WinDef._
import net.chloe.models.classes._
import net.chloe.wow._
import play.Logger

trait WowClass {
  
  val remainingTimeForCooldowns = MHashMap[Cooldown, FiniteDuration]()
  val remainingTimeForBuffs = MHashMap[Buff, FiniteDuration]()
  val remainingTimeForDebuffs = MHashMap[Debuff, FiniteDuration]()
  
  //val cooldowns: List[Cooldown]
  //val buffs: List[Buff]
  //val debuffs: List[Debuff]
  val spellAndTargetToKeys: Map[(Spell, Option[SpellTargetType]), List[Int]]
  val spells: List[Spell]
  val spellTargetType: SpellTargetType
  
  val name: String
  val color: Color
  val hWindow: HWND
  
  implicit val me = this
  
  var lastSpellIdOpt: Option[Int] = None
  
  def executeNextAction(implicit team: Team)
  
  def getSpellAndTargetBasedOnPriorityOpt(
    spellAndTargetToPriority: Map[(Spell, Option[SpellTargetType]), Float]
  ) = {
    spellAndTargetToPriority
      .toSeq
      .sortWith { case ((_, priorityA), (_, priorityB)) =>
        priorityA > priorityB
      }
      .toList
      .head match {
        case (spellAndTarget, priority) if priority > 0 => Some(spellAndTarget)
        case _ => None
      }
  }
    
  def sendAction(spellAndTarget: (Spell, Option[SpellTargetType])) = {
    Logger.debug(s"$name - Executing: $spellAndTarget")
    Wow.pressAndReleaseKeystrokes(spellAndTargetToKeys(spellAndTarget))
  }
    
}
