package net.chloe.models.classes

import net.chloe.models.spells.hunter._
import net.chloe.models._
import net.chloe.models.spells._
import net.chloe.wow._
import net.chloe._
import scala.collection.mutable.{ HashMap => MHashMap }
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinNT._
import play.Logger
import scala.util._
import scala.concurrent.duration._

case class HunterBM(
  name: String,
  hWindow: HWND,
  spellTargetType: SpellTargetType
) extends WowClass {
  
  val spellAndTargetToKeys = HunterBM.spellAndTargetToKeys
  val spells: List[Spell] = spellAndTargetToKeys
    .map { case ((spell, _), _) =>
      spell
    }
    .toList
    .distinct
    
  def executeNextAction(implicit team: Team) = {
    Player.getLastSpellCastedOpt match {
      case Some(CounterShot) => team.updateLastInterrupt()
      case _ =>
    }
    
    val meHealth = Player.getHealthPercentage
    
    /*val tank = team
      .players
      .find { 
        case (Tank, _) => true
        case _ => false
      } match {
        case Some((_, player)) => player
        case _ => throw new Exception("Missing tank.")
      }*/
    
    val isAnyoneInCombat = team.players
      .find { case (_, player) =>
         Player.isInCombat(player)
      }
      .isDefined

    //println("pet health " + Pet.getHealthPercentage)
    //println("has pet " + Player.hasPet)
    //println("can revive " + Player.canCast(RevivePet))
    //println("getEnnemiesCountInRange " + Player.getEnnemiesCountInRange)
    //println("areNameplatesOn " + Player.areNameplatesOn)
    //println(me.name + " Player.canCast(PrimalRage) " + Player.canCast(PrimalRage) + " Player.hasDebuff(Fatigued) " + Player.hasDebuff(Fatigued))

    val canCastExhilaration = Player.canCast(Exhilaration)
    val canCastAspectOfTheTurtle = Player.canCast(AspectOfTheTurtle)
    val canCastSurvivalOfTheFittest = Player.canCast(SurvivalOfTheFittest)
    
    val misdirectionFinishedLately = Player.getCooldownRemainingTimeOpt(Misdirection) match {
        case Some(cooldownRemainingTime) if cooldownRemainingTime < 10.seconds => true
        case _ => false
      }

    if (
      meHealth > 1 && 
      /*(
        (Player.isAlive(tank) && Player.isInCombat(tank)) ||
        (!Player.isAlive(tank) && Player.isInCombat)
      ) &&*/
      isAnyoneInCombat && !Player.isChanneling
    ) {
      if (Pet.getHealthPercentage == 0 && Player.canCast(RevivePet)) {
        sendAction(CallPet -> None)
        sendAction(RevivePet -> None)
      } else if (Pet.getHealthPercentage < Configuration.MajorHealthThreshold && Player.canCast(MendPet)) {
        sendAction(MendPet -> None)
      } else if (
        Player.getHealthPercentage < Configuration.CriticalHealthThreshold &&
        (/*canCastAspectOfTheTurtle || */canCastExhilaration || canCastSurvivalOfTheFittest)
      ) {
        if (canCastExhilaration) {
          sendAction(Exhilaration -> None)
        } else if (canCastSurvivalOfTheFittest) { 
          sendAction(SurvivalOfTheFittest -> None)
        } else {
          //sendAction(AspectOfTheTurtle -> None)
        }
      //} else if (Target.canInterruptAsTeam) {
      //  sendAction(CounterShot -> None)
      //} else if (Player.canCast(PrimalRage) && !Player.hasDebuff(Fatigued)) {
      //  sendAction(PrimalRage -> None)
      //} else if (
      //  Player.canCast(Barrage) && 
      //  Player.getEnnemiesCountInRange > 1 &&
      //  !Player.isMoving
      //) {
      //  sendAction(Barrage -> None)
      } else if (Player.canCast(Misdirection)) {
        sendAction(Misdirection -> None)
      } else if (Player.canCast(KillCommand)) {
        sendAction(KillCommand -> None)
      } else if (Player.canCast(BarbedShot)) {
        sendAction(BarbedShot -> None)
      } else if (Player.canCast(BestialWrath)) {
        sendAction(BestialWrath -> None)
      } else if (Player.canCast(AspectOfTheWild)) {
        sendAction(AspectOfTheWild -> None)
      } else if (Player.canCast(AMurderOfCrows)) {
        sendAction(AMurderOfCrows -> None)
      //} else if (Player.canCast(FeignDeath) && misdirectionFinishedLately) {
        //sendAction(FeignDeath -> None)
        //Wow.pressAndReleaseKeystrokes(List(Keys.W))
      } else if (Player.canCast(ChimaeraShot)) {
        sendAction(ChimaeraShot -> None)
      //} else if (Player.canCast(DireBeast)) {
      //  sendAction(DireBeast -> None)
      } else if (
        Player.canCast(MultiShot) && 
        Player.getEnnemiesCountInRange > 1 &&
        !Player.isMoving
      ) {
        sendAction(MultiShot -> None)
      } else if (Player.canCast(CobraShot)) {
        sendAction(CobraShot -> None)
      } else {
        //Logger.debug(s"${me.name} - Executing no attack.")
      }
    } else {
      //Logger.debug(s"${me.name} - Executing no attack.")
    }
  }
  
}

object HunterBM {
  
  val spellAndTargetToKeys: Map[(Spell, Option[SpellTargetType]), List[Int]] = Map(
    (MultiShot, None) -> List(Keys.LShiftKey, Keys.D1),
    (DireBeast, None) -> List(Keys.LShiftKey, Keys.D2),
    (KillCommand, None) -> List(Keys.LShiftKey, Keys.D3),
    (CobraShot, None) -> List(Keys.LShiftKey, Keys.D4),
    (BestialWrath, None) -> List(Keys.LShiftKey, Keys.D5),
    (AspectOfTheWild, None) -> List(Keys.LShiftKey, Keys.D6),
    (Exhilaration, None) -> List(Keys.LShiftKey, Keys.D7),
    (AspectOfTheTurtle, None) -> List(Keys.LShiftKey, Keys.D8),
    (CounterShot, None) -> List(Keys.LShiftKey, Keys.D9),
    //(TitansThunder, None) -> List(Keys.LShiftKey, Keys.D0),
    (RevivePet, None) -> List(Keys.LShiftKey, Keys.F1),
    (MendPet, None) -> List(Keys.LShiftKey, Keys.F1),
    (AMurderOfCrows, None) -> List(Keys.LShiftKey, Keys.F2),
    (BarbedShot, None) -> List(Keys.Alt, Keys.F1),
    //(PrimalRage, None) -> List(Keys.Alt, Keys.F2),
    (SurvivalOfTheFittest, None) -> List(Keys.Alt, Keys.F2),
    //(Barrage, None) -> List(Keys.Alt, Keys.F3),
    (ChimaeraShot, None) -> List(Keys.Alt, Keys.F5),
    (Misdirection, None) -> List(Keys.Alt, Keys.F9),
    //(Misdirection, None) -> List(Keys.D8),
    (FeignDeath, None) -> List(Keys.LShiftKey, Keys.F3),
    (CallPet, None) -> List(Keys.LShiftKey, Keys.D0)
  )

}
