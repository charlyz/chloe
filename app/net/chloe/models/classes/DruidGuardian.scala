package net.chloe.models.classes

import net.chloe.models.spells.druid._
import net.chloe.models._
import net.chloe.models.spells._
import net.chloe.wow._
import net.chloe._
import scala.collection.mutable.{ HashMap => MHashMap }
import com.sun.jna.platform.win32.WinDef._
import play.Logger

case class DruidGuardian(
  name: String,
  hWindow: HWND,
  spellTargetType: SpellTargetType
) extends WowClass {
  
  val spellAndTargetToKeys = DruidGuardian.spellAndTargetToKeys
  val spells: List[Spell] = spellAndTargetToKeys
    .map { case ((spell, _), _) =>
      spell
    }
    .toList
    .distinct
    
  def executeNextAction(implicit team: Team) = {
    Player.getLastSpellCastedOpt match {
      case Some(SkullBash) => team.updateLastInterrupt()
      case Some(MightyBash) => team.updateLastInterrupt()
      case _ =>
    }
    //println("getEnnemiesCountInRange " + Player.getEnnemiesCountInRange)
    //println("areNameplatesOn " + Player.areNameplatesOn)
    //println("Player.canCast(Ironfur) " + Player.canCast(Ironfur))
    //println("!Player.getBuffRemainingTimeOpt(Ironfur).isDefined " + !Player.getBuffRemainingTimeOpt(Ironfur).isDefined)
    //println("Player.hasBuff(BearForm) " + Player.hasBuff(BearForm))
    
    val meHealth = Player.getHealthPercentage
    
    if (meHealth > 1 && !Player.hasBuff(BearForm) && Player.canCast(BearForm)) {
      sendAction(BearForm -> None)
    } else if (meHealth > 1 && Player.isInCombat) {
      val canCastBarkskin = Player.canCast(GuardianBarkskin)
      val canCastSurvivalInstincts = Player.canCast(SurvivalInstincts) && 
        !Player.getBuffRemainingTimeOpt(FrenziedRegeneration).isDefined
      val canCastIronfur = Player.canCast(Ironfur) && 
        !Player.getBuffRemainingTimeOpt(Ironfur).isDefined &&
        Player.getPowerPercentage > 50
      val canCastFrenziedRegeneration = Player.canCast(FrenziedRegeneration) && 
        !Player.getBuffRemainingTimeOpt(FrenziedRegeneration).isDefined &&
        Player.getPowerPercentage > 30
      val canCastMightyBash = Player.canCast(MightyBash)
      val canCastSkullBash = Player.canCast(SkullBash)
      val canCastMangle = Player.canCast(Mangle)
      //println("canCastMangle " + canCastMangle)
      //println("Player.hasBuff(Gore) " + Player.hasBuff(Gore))
      if (
        Player.getHealthPercentage < Configuration.CriticalHealthThreshold &&
        (canCastBarkskin || canCastSurvivalInstincts || canCastIronfur || canCastFrenziedRegeneration)
      ) {
        if (canCastBarkskin) {
          sendAction(GuardianBarkskin -> None)
        } else if (canCastSurvivalInstincts) {
          sendAction(SurvivalInstincts -> None)
        } else if (canCastIronfur) {
          sendAction(Ironfur -> None)
        } else if (canCastFrenziedRegeneration) {
          sendAction(FrenziedRegeneration -> None)
        }
      } else if (Player.hasBuff(GalacticGuardian)) {
        sendAction(GuardianMoonfire -> None)
      } else if (Player.hasBuff(Gore)) {
        sendAction(Mangle -> None)
      } else if (Target.canInterruptAsTeam && (canCastMightyBash || canCastSkullBash)) {
        if (canCastSkullBash) {
          sendAction(SkullBash -> None)
        } else {
          sendAction(MightyBash -> None)
        }
      } else if (Player.canCast(Growl)) {
        sendAction(Growl -> None)
      } else if (Player.canCast(Trash)) {
        sendAction(Trash -> None)
      } else if (canCastMangle) {
        sendAction(Mangle -> None)
      } else if (Player.canCast(GuardianMoonfire) && !Target.getDebuffRemainingTimeOpt(GuardianMoonfire).isDefined) {
        sendAction(GuardianMoonfire -> None)
      } else if (Player.canCast(Maul) && Player.getPowerPercentage > 50) {
        sendAction(Maul -> None)
      } else if (Player.canCast(Swipe)) {
        sendAction(Swipe -> None)
      } else {
        //Logger.debug(s"${me.name} - Executing no attack.")
      }
    } else {
      //Logger.debug(s"${me.name} - Executing no attack.")
    }
  }
  
}

object DruidGuardian {
  
  val spellAndTargetToKeys: Map[(Spell, Option[SpellTargetType]), List[Int]] = Map(
    (MightyBash, None) -> List(Keys.LShiftKey, Keys.D1),
    (Trash, None) -> List(Keys.LShiftKey, Keys.D2),
    (Mangle, None) -> List(Keys.LShiftKey, Keys.D3),
    (Maul, None) -> List(Keys.LShiftKey, Keys.D4),
    (Swipe, None) -> List(Keys.LShiftKey, Keys.D5),
    //(RageOfTheSleeper, None) -> List(Keys.LShiftKey, Keys.D6),
    (SurvivalInstincts, None) -> List(Keys.LShiftKey, Keys.D7),
    (Ironfur, None) -> List(Keys.LShiftKey, Keys.D8),
    (FrenziedRegeneration, None) -> List(Keys.LShiftKey, Keys.D9),
    (SkullBash, None) -> List(Keys.LShiftKey, Keys.D0),
    (BearForm, None) -> List(Keys.LShiftKey, Keys.F1),
    (GuardianMoonfire,  None) -> List(Keys.LShiftKey, Keys.F2),
    (Growl, None) -> List(Keys.LShiftKey, Keys.F3),
    (GuardianBarkskin, None) -> List(Keys.Alt, Keys.H)
  )

}
