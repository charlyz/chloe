package net.chloe.models.classes

import net.chloe.models.spells.priest._
import net.chloe.models._
import net.chloe.models.spells._
import net.chloe.wow._
import scala.collection.mutable.{ HashMap => MHashMap }
import com.sun.jna.platform.win32.WinDef._
import play.Logger

case class Priest(
  name: String,
  hWindow: HWND,
  spellTargetType: SpellTargetType
) extends WowClass {
  
  val spellAndTargetToKeys = Priest.spellAndTargetToKeys
  val spells: List[Spell] = spellAndTargetToKeys
    .map { case ((spell, _), _) =>
      spell
    }
    .toList
    .distinct
    
  def executeNextAction(implicit team: Team) = {
    implicit val me = team.players.get(spellTargetType) match {
      case Some(player) => player
      case _ => throw new Exception("Player could not find itself.")
    }
    
    if (Player.isInCombat && Player.getHealthPercentage < 80) {
      Logger.debug(s"Attacking target - health too low!")
      Target.getDebuffRemainingTimeOpt(ShadowWordPain) match {
        case Some(_) => Wow.pressAndReleaseKeystrokes(spellAndTargetToKeys(Smite, None))
        case _ => Wow.pressAndReleaseKeystrokes(spellAndTargetToKeys(ShadowWordPain, None))
      }
    }
  }
  
}

object Priest {
  
  val spellAndTargetToKeys: Map[(Spell, Option[SpellTargetType]), List[Int]] = Map(
    (Smite, None) -> List(Keys.C),
    (ShadowWordPain, None) -> List(Keys.LShiftKey, Keys.F1)
  )

}
