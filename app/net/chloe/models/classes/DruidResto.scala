package net.chloe.models.classes

import net.chloe.models.spells.druid._
import net.chloe.models._
import net.chloe.models.spells._
import net.chloe.wow._
import net.chloe._
import scala.collection.mutable.{ HashMap => MHashMap }
import com.sun.jna.platform.win32.WinDef._
import play.Logger

case class DruidResto(
  name: String,
  hWindow: HWND,
  spellTargetType: SpellTargetType
) extends WowClass {
  
  val spellAndTargetToKeys = DruidResto.spellAndTargetToKeys
  val spells: List[Spell] = spellAndTargetToKeys
    .map { case ((spell, _), _) =>
      spell
    }
    .toList
    .distinct

  def executeNextAction(implicit team: Team) = {
    //executeNextActionForDualTeamWithDps
    executeNextActionForTinityTeam
  }
  
  def executeNextActionForDualTeamWithDps(implicit team: Team) = {
    val spellAndTargetToPriority = MHashMap(
      spellAndTargetToKeys
        .map { case (spellAndTarget, _) =>
          spellAndTarget -> 0f
        }
        .toMap
        .toSeq: _*
      ) 
  
    val dpsOne = team
      .players
      .find { 
        case (DpsOne, _) => true
        case _ => false
      } match {
        case Some((_, player)) => player
        case _ => throw new Exception("Missing dps one.")
      }
 
    //println("Player.getBuffRemainingTimeOpt(Lifebloom) " + Player.getBuffRemainingTimeOpt(Lifebloom))
    //println("Player.canCast(Lifebloom) " + Player.canCast(Lifebloom))
    if (!Player.isCasting && !Player.isChanneling) {
      val meHealth = Player.getHealthPercentage
      val dpsOneHealth = Player.getHealthPercentage(dpsOne)
      
      val isInCombat = Player.isInCombat(dpsOne)
     
      val canCastBarkskin = Player.canCast(RestoBarkskin)
      val canCastTranquility = Player.canCast(Tranquility)
      val canCastSwiftmend = Player.canCast(Swiftmend)
      val canCastWildGrowth = Player.canCast(WildGrowth)
      //val canCastHealingTouch = Player.canCast(HealingTouch)
      val canCastRenewal = Player.canCast(Renewal)
      
      if (
        meHealth < Configuration.MajorHealthThreshold &&
        dpsOneHealth < Configuration.MajorHealthThreshold && 
        dpsOneHealth > 0 &&
        meHealth > 0 &&
        canCastWildGrowth &&
        canCastTranquility &&
        isInCombat
      ) {
        if (canCastTranquility) {
          sendAction(Tranquility -> None)
        } else {
          sendAction(WildGrowth -> None)
        }
      } else if (
        meHealth > 0 && 
        meHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        (canCastBarkskin || canCastSwiftmend || canCastRenewal)
      ) {
        if (canCastRenewal) {
          sendAction(Renewal -> None)
        } else if (canCastSwiftmend) {
          sendAction(Swiftmend -> Some(Healer))
        } else if (canCastBarkskin) {
          sendAction(RestoBarkskin -> None)
        }
      } else if (
        dpsOneHealth >= 0 && 
        dpsOneHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(DpsOne))
      } else if (
        meHealth < Configuration.FullHealthThreshold ||
        dpsOneHealth < Configuration.FullHealthThreshold
      ) {
        val (dpsOneHealthWeight, healerHealthWeight) = if (meHealth <= 0) {
          (1, -100)
        } else if (dpsOneHealth <= 0) {
          (-100, 1)
        } else if (meHealth > dpsOneHealth) {
          (1, 0)
        } else {
          (0, 1)
        }
        
        val canCastRejuvenation = Player.canCast(Rejuvenation)
        val canCastRegrowth = Player.canCast(Regrowth)
        val canCastFlourish = Player.canCast(Flourish)
        val canCastLifebloom = Player.canCast(Lifebloom)
        var hotsCount = 0
        
        if (canCastLifebloom && !Player.getBuffRemainingTimeOpt(Lifebloom).isDefined) {
          spellAndTargetToPriority(Lifebloom -> Some(Healer)) += 6
        }
        
        if (canCastRejuvenation) {
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 4 + healerHealthWeight
          }
          
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(dpsOne).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Rejuvenation -> Some(DpsOne)) += 4 + dpsOneHealthWeight
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 3 + healerHealthWeight
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination)(dpsOne).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Rejuvenation -> Some(DpsOne)) += 3 + dpsOneHealthWeight
          }
        }

        if (canCastRegrowth) {
          if (!Player.getBuffRemainingTimeOpt(Regrowth).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Regrowth -> Some(Healer)) += 2 + healerHealthWeight
          }
          
          if (!Player.getBuffRemainingTimeOpt(Regrowth)(dpsOne).isDefined) {
            hotsCount += 1
            spellAndTargetToPriority(Regrowth -> Some(DpsOne)) += 2 + dpsOneHealthWeight
          }
        }
        
        if (canCastFlourish && hotsCount > 3) {
          spellAndTargetToPriority(Flourish -> None) += hotsCount
        }
         
        //if (canCastHealingTouch) {
        //  spellAndTargetToPriority(HealingTouch -> Some(DpsOne)) += dpsOneHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(Healer)) += healerHealthWeight
        //}
        
        getSpellAndTargetBasedOnPriorityOpt(spellAndTargetToPriority.toMap) match {
          case Some(spellAndTarget) => sendAction(spellAndTarget)
          case _ => Logger.debug(s"${me.name} - Executing no heal.")
        }
      } else {
        if (isInCombat) {
          if (!Target.getDebuffRemainingTimeOpt(RestoMoonfire).isDefined && Player.canCast(RestoMoonfire)) {
            spellAndTargetToPriority(RestoMoonfire -> None) += 1
          } else if (!Target.getDebuffRemainingTimeOpt(Sunfire).isDefined && Player.canCast(Sunfire)) {
            spellAndTargetToPriority(Sunfire -> None) += 1
          } else if (Player.canCast(SolarWrath)) {
            spellAndTargetToPriority(SolarWrath -> None) += 1
          }
        }

        getSpellAndTargetBasedOnPriorityOpt(spellAndTargetToPriority.toMap) match {
          case Some(spellAndTarget) => sendAction(spellAndTarget)
          case _ => Logger.debug(s"${me.name} - Executing no attack.")
        }
      }
    }
  }

  def executeNextActionForTinityTeam(implicit team: Team) = {
    val spellAndTargetToPriority = MHashMap(
      spellAndTargetToKeys
        .map { case (spellAndTarget, _) =>
          spellAndTarget -> 0f
        }
        .toMap
        .toSeq: _*
      ) 
  
    val dpsOne = team.getPlayer(DpsOne)
    val dpsTwo = team.getPlayer(DpsTwo)
    val dpsThree = team.getPlayer(DpsThree)
    val tank = team.getPlayer(Tank)
    
    val meHealth = Player.getHealthPercentage

    //println("Player.getBuffRemainingTimeOpt(Lifebloom) " + Player.getBuffRemainingTimeOpt(Lifebloom))
    //println("Player.canCast(Lifebloom) " + Player.canCast(Lifebloom))
    //println("Player.getBuffRemainingTimeOpt(Rejuvenation)(tank) " + Player.getBuffRemainingTimeOpt(Rejuvenation)(tank))
    //println("Player.getBuffRemainingTimeOpt(Germination)(tank)) " + Player.getBuffRemainingTimeOpt(Germination)(tank))
    //1sendAction(Rejuvenation -> Some(DpsThree))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(dpsTwo) " + Player.getBuffRemainingTimeOpt(Regrowth)(dpsTwo))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(tank) " + Player.getBuffRemainingTimeOpt(Regrowth)(tank))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(dpsOne1) " + Player.getBuffRemainingTimeOpt(Regrowth)(dpsOne))
    if (meHealth > 1 && !Player.isCasting && !Player.isChanneling) {
      val dpsOneHealth = Player.getHealthPercentage(dpsOne)
      val dpsTwoHealth = Player.getHealthPercentage(dpsTwo)
      val dpsThreeHealth = Player.getHealthPercentage(dpsThree)
      val tankHealth = Player.getHealthPercentage(tank)
      
      val isInCombat = Player.isInCombat(tank)
     
      val canCastBarkskin = Player.canCast(RestoBarkskin)
      val canCastTranquility = Player.canCast(Tranquility)
      val canCastSwiftmend = Player.canCast(Swiftmend)
      val canCastWildGrowth = Player.canCast(WildGrowth)
      //val canCastHealingTouch = Player.canCast(HealingTouch)
      val canCastRenewal = Player.canCast(Renewal)
      val canCastIronbark = Player.canCast(Ironbark)
      
      if (
        meHealth < Configuration.MajorHealthThreshold &&
        dpsOneHealth < Configuration.MajorHealthThreshold && 
        dpsTwoHealth < Configuration.MajorHealthThreshold &&
        dpsThreeHealth < Configuration.MajorHealthThreshold && 
        tankHealth < Configuration.MajorHealthThreshold &&
        
        dpsOneHealth > 1 &&
        meHealth > 1 &&
        dpsTwoHealth > 1 &&
        dpsThreeHealth > 1 &&
        tankHealth > 1 &&
        
        canCastWildGrowth &&
        canCastTranquility &&
        isInCombat
      ) {
        if (canCastTranquility) {
          sendAction(Tranquility -> None)
        } else {
          sendAction(WildGrowth -> None)
        }
      } else if (
        tankHealth > 1 && 
        tankHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        (canCastIronbark || canCastSwiftmend)
      ) {
        if (canCastSwiftmend) {
          sendAction(Swiftmend -> Some(Tank))
        } else  {
          sendAction(Ironbark -> Some(Tank))
        }
      } else if (
        meHealth > 1 && 
        meHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        (canCastBarkskin || canCastSwiftmend || canCastRenewal)
      ) {
        if (canCastRenewal) {
          sendAction(Renewal -> None)
        } else if (canCastSwiftmend) {
          sendAction(Swiftmend -> Some(Healer))
        } else if (canCastBarkskin) {
          sendAction(RestoBarkskin -> None)
        }
      } else if (
        dpsOneHealth > 1 && 
        dpsOneHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(DpsOne))
      } else if (
        dpsTwoHealth > 1 && 
        dpsTwoHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(DpsTwo))
      } else if (
        dpsThreeHealth > 1 && 
        dpsThreeHealth < Configuration.CriticalHealthThreshold && 
        isInCombat && 
        canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(DpsThree))
      } else if (
        (meHealth < Configuration.FullHealthThreshold && meHealth > 1) ||
        (dpsOneHealth < Configuration.FullHealthThreshold && dpsOneHealth > 1) ||
        (dpsTwoHealth < Configuration.FullHealthThreshold && dpsTwoHealth > 1) ||
        (dpsThreeHealth < Configuration.FullHealthThreshold && dpsThreeHealth > 1) ||
        (tankHealth < Configuration.FullHealthThreshold && tankHealth > 1)
      ) {
        val tankHealthWeight = if (tankHealth <= 1 || tankHealth > 99) {
          (-100).toFloat
        } else {
          1 - tankHealth.toFloat / 100f //+ 0.5f
        }

        val healerHealthWeight = if (meHealth <= 0 || meHealth > 99) {
          (-100).toFloat
        } else {
          1 - meHealth.toFloat / 100f //+ 0.4f
        }
        
        val dpsOneHealthWeight = if (dpsOneHealth <= 0 || dpsOneHealth > 99) {
          (-100).toFloat
        } else {
          1 - dpsOneHealth.toFloat / 100f //+ 0.3f
        }
        
        val dpsTwoHealthWeight = if (dpsTwoHealth <= 0 || dpsTwoHealth > 99) {
          (-100).toFloat
        } else {
          1 - dpsTwoHealth.toFloat / 100f //+ 0.2f
        }

        val dpsThreeHealthWeight = if (dpsThreeHealth <= 0 || dpsThreeHealth > 99) {
          (-100).toFloat
        } else {
          1 - dpsThreeHealth.toFloat / 100f //+ 0.1f
        }
        
        val canCastRejuvenation = Player.canCast(Rejuvenation)
        val canCastRegrowth = Player.canCast(Regrowth)
        val canCastFlourish = Player.canCast(Flourish)
        val canCastLifebloom = Player.canCast(Lifebloom)
        var hotsCount = 0
        
        if (canCastLifebloom && !Player.getBuffRemainingTimeOpt(Lifebloom)(tank).isDefined) {
          spellAndTargetToPriority(Lifebloom -> Some(Tank)) += 6
        }
        
        if (canCastRejuvenation) {
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(tank).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Tank)) += 4 + tankHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 4 + healerHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(dpsOne).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsOne)) += 4 + dpsOneHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(dpsTwo).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsTwo)) += 4 + dpsTwoHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(dpsThree).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsThree)) += 4 + dpsThreeHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination)(tank).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Tank)) += 3 + tankHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Germination).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 3 + healerHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination)(dpsOne).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsOne)) += 3 + dpsOneHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination)(dpsTwo).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsTwo)) += 3 + dpsTwoHealthWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Germination)(dpsThree).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(DpsThree)) += 3 + dpsThreeHealthWeight
          } else {
            hotsCount += 1
          }
        }
        
        val clearcastingWeight = if (Player.getBuffRemainingTimeOpt(Germination).isDefined) {
          6
        } else {
          0
        }
       
        if (canCastRegrowth) {
          if (!Player.getBuffRemainingTimeOpt(Regrowth)(tank).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(Tank)) += 2 + tankHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Regrowth).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(Healer)) += 2 + healerHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Regrowth)(dpsOne).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(DpsOne)) += 2 + dpsOneHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Regrowth)(dpsTwo).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(DpsTwo)) += 2 + dpsTwoHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
          
          if (!Player.getBuffRemainingTimeOpt(Regrowth)(dpsThree).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(DpsThree)) += 2 + dpsThreeHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
        }
        
        if (canCastFlourish && hotsCount > 4) {
          spellAndTargetToPriority(Flourish -> None) += hotsCount
        }
         
        //if (canCastHealingTouch) {
        //  spellAndTargetToPriority(HealingTouch -> Some(DpsOne)) += dpsOneHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(DpsTwo)) += dpsTwoHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(DpsThree)) += dpsThreeHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(Tank)) += tankHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(Healer)) += healerHealthWeight
        //}
        println(spellAndTargetToPriority)
        getSpellAndTargetBasedOnPriorityOpt(spellAndTargetToPriority.toMap) match {
          case Some(spellAndTarget) => sendAction(spellAndTarget)
          case _ => Logger.debug(s"${me.name} - Executing no heal.")
        }
      } else {
        if (isInCombat) {
          if (!Target.getDebuffRemainingTimeOpt(RestoMoonfire).isDefined && Player.canCast(RestoMoonfire)) {
            spellAndTargetToPriority(RestoMoonfire -> None) += 1
          } else if (!Target.getDebuffRemainingTimeOpt(Sunfire).isDefined && Player.canCast(Sunfire)) {
            spellAndTargetToPriority(Sunfire -> None) += 1
          } else if (Player.canCast(SolarWrath)) {
            spellAndTargetToPriority(SolarWrath -> None) += 1
          }
        }

        getSpellAndTargetBasedOnPriorityOpt(spellAndTargetToPriority.toMap) match {
          case Some(spellAndTarget) => sendAction(spellAndTarget)
          case _ => //Logger.debug(s"${me.name} - Executing no attack.")
        }
      }
    }
  }
  
}

object DruidResto {
  
  val spellAndTargetToKeys: Map[(Spell, Option[SpellTargetType]), List[Int]] = Map(
    (Rejuvenation, Some(Healer)) -> List(Keys.LShiftKey, Keys.D1),
    (Regrowth, Some(Healer)) -> List(Keys.LShiftKey, Keys.D2),
    //(HealingTouch, Some(Healer)) -> List(Keys.LShiftKey, Keys.D3),
    (Lifebloom, Some(Healer)) -> List(Keys.LShiftKey, Keys.D4),
    (Swiftmend, Some(Healer)) -> List(Keys.LShiftKey, Keys.D5),
    (Tranquility, None) -> List(Keys.LShiftKey, Keys.D6),
    (WildGrowth, Some(Healer)) -> List(Keys.LShiftKey, Keys.D7),
    (Innervate, Some(Healer)) -> List(Keys.LShiftKey, Keys.D8),
    (EssenceOfGHanir, None) -> List(Keys.LShiftKey, Keys.D9),
    (Efflorescence, Some(Healer)) -> List(Keys.LShiftKey, Keys.D0),
    (RestoBarkskin, None) -> List(Keys.LShiftKey, Keys.F1),
    (Ironbark, Some(Healer)) -> List(Keys.LShiftKey, Keys.F2),
    (RestoMoonfire, None) -> List(Keys.LShiftKey, Keys.F3),
    (SolarWrath, None) -> List(Keys.LShiftKey, Keys.F4),
    (Sunfire, None) -> List(Keys.LShiftKey, Keys.F5),
    (Renewal, None) -> List(Keys.LShiftKey, Keys.F6),
    (Flourish, None) -> List(Keys.LShiftKey, Keys.F7),
    
    (Rejuvenation, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D1),
    (Regrowth, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D2),
    //(HealingTouch, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D3),
    (Swiftmend, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D4),
    (WildGrowth, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D5),
    (Efflorescence, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D6),
    (Ironbark, Some(DpsOne)) -> List(Keys.LControlKey, Keys.D7),
    
    (Rejuvenation, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F1),
    (Regrowth, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F2),
    //(HealingTouch, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F3),
    (Swiftmend, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F4),
    (WildGrowth, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F5),
    (Efflorescence, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F6),
    (Ironbark, Some(DpsTwo)) -> List(Keys.LControlKey, Keys.F7),
    
    (Rejuvenation, Some(DpsThree)) -> List(Keys.Alt, Keys.F1),
    (Regrowth, Some(DpsThree)) -> List(Keys.Alt, Keys.F2),
    //(HealingTouch, Some(DpsThree)) -> List(Keys.Alt, Keys.F3),
    (Swiftmend, Some(DpsThree)) -> List(Keys.Alt, Keys.F8),
    (WildGrowth, Some(DpsThree)) -> List(Keys.Alt, Keys.F5),
    (Efflorescence, Some(DpsThree)) -> List(Keys.Alt, Keys.F6),
    (Ironbark, Some(DpsThree)) -> List(Keys.Alt, Keys.F7),
    
    (Rejuvenation, Some(Tank)) -> List(Keys.Alt, Keys.D1),
    (Regrowth, Some(Tank)) -> List(Keys.Alt, Keys.D2),
    //(HealingTouch, Some(Tank)) -> List(Keys.Alt, Keys.D3),
    (Lifebloom, Some(Tank)) -> List(Keys.Alt, Keys.D4),
    (Swiftmend, Some(Tank)) -> List(Keys.Alt, Keys.D5),
    (WildGrowth, Some(Tank)) -> List(Keys.Alt, Keys.D6),
    (Efflorescence, Some(Tank)) -> List(Keys.Alt, Keys.D7),
    (Ironbark, Some(Tank)) -> List(Keys.Alt, Keys.D8)
  )
  
}
