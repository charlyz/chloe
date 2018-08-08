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
    
    val tank = team
      .players
      .find { 
        case (Tank, _) => true
        case _ => false
      } match {
        case Some((_, player)) => player
        case _ => throw new Exception("Missing tank.")
      }
      
    //println("pet health " + Pet.getHealthPercentage)
    //println("has pet " + Player.hasPet)
    //println("can revive " + Player.canCast(RevivePet))
    //println("getEnnemiesCountInRange " + Player.getEnnemiesCountInRange)
    //println("areNameplatesOn " + Player.areNameplatesOn)
    
    val canCastExhilaration = Player.canCast(Exhilaration)
    val canCastAspectOfTheTurtle = Player.canCast(AspectOfTheTurtle)
    
    if (Player.isInCombat(tank)) {
      if (Pet.getHealthPercentage == 0 && Player.canCast(RevivePet)) {
        sendAction(RevivePet -> None)
      } else if (
        Player.getHealthPercentage < Configuration.CriticalHealthThreshold &&
        (canCastAspectOfTheTurtle || canCastExhilaration)
      ) {
        if (canCastExhilaration) {
          sendAction(Exhilaration -> None)
        } else {
          sendAction(AspectOfTheTurtle -> None)
        }
      } else if (Target.canInterruptAsTeam) {
        sendAction(CounterShot -> None)
      } else if (Player.canCast(PrimalRage)) {
        sendAction(PrimalRage -> None)
      } else if (Player.canCast(BestialWrath)) {
        sendAction(BestialWrath -> None)
      } else if (Player.canCast(AspectOfTheWild)) {
        sendAction(AspectOfTheWild -> None)
      //} else if (Player.canCast(TitansThunder) && Player.getChargesCount(DireBeast) > 0) {
      //  sendAction(TitansThunder -> None)
      } else if (Player.canCast(AMurderOfCrows)) {
        sendAction(AMurderOfCrows -> None)
      } else if (Player.canCast(DireBeast)) {
        sendAction(DireBeast -> None)
      } else if (Player.canCast(KillCommand)) {
        sendAction(KillCommand -> None)
      } else if (
        Player.canCast(MultiShot) && 
        Player.getEnnemiesCountInRange > 2 &&
        Player.getPowerPercentage > 60
      ) {
        sendAction(MultiShot -> None)
      }else if (
        Player.canCast(CobraShot) && 
        Player.getPowerPercentage > 60
      ) {
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
    (AMurderOfCrows, None) -> List(Keys.LShiftKey, Keys.F2),
    (PrimalRage, None) -> List(Keys.Alt, Keys.F2)
  )

}
