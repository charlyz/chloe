package net.chloe.models.classes

import net.chloe.models.spells.druid._
import net.chloe.models._
import net.chloe.models.spells._
import net.chloe.wow._
import net.chloe._
import scala.collection.mutable.{HashMap => MHashMap}

import com.sun.jna.platform.win32.WinDef._
import play.Logger

case class DruidRestoCatWeave(
  name: String,
  hWindow: HWND,
  spellTargetType: SpellTargetType
) extends WowClass {

  val spellAndTargetToKeys = DruidRestoCatWeave.spellAndTargetToKeys
  val spells: List[Spell] = spellAndTargetToKeys
    .map { case ((spell, _), _) =>
      spell
    }
    .toList
    .distinct

  def executeNextAction(implicit team: Team) = {
    val spellAndTargetToPriority = MHashMap(
      spellAndTargetToKeys
        .map { case (spellAndTarget, _) =>
          spellAndTarget -> 0f
        }
        .toMap
        .toSeq: _*
    )

    //val tank = team.getPlayer(Tank)
    val healerOne = team.getPlayer(Healer)
    val healerTwo = team.getPlayer(HealerTwo)
    val healerThree = team.getPlayer(HealerThree)
    val healerFour = team.getPlayer(HealerFour)

    val (
      relativeHealerOneType: SpellTargetType,
      relativeHealerOne,
      relativeHealerTwoType: SpellTargetType,
      relativeHealerTwo,
      relativeHealerThreeType: SpellTargetType,
      relativeHealerThree
    ) = team
      .players
      .find {
        case (Healer, player) if player == me => true
        case (HealerTwo, player) if player == me => true
        case (HealerThree, player) if player == me => true
        case (HealerFour, player) if player == me => true
        case _ => false
      } match {
        case Some((Healer, player)) => (
          HealerTwo,
          healerTwo,
          HealerThree,
          healerThree,
          HealerFour,
          healerFour
        )
        case Some((HealerTwo, player)) => (
          HealerThree,
          healerThree,
          HealerFour,
          healerFour,
          Healer,
          healerOne
        )
        case Some((HealerThree, player)) => (
          HealerFour,
          healerFour,
          Healer,
          healerOne,
          HealerTwo,
          healerTwo
        )
        case Some((HealerFour, player)) => (
          Healer,
          healerOne,
          HealerTwo,
          healerTwo,
          HealerThree,
          healerThree
        )
      case _ => throw new Exception("Missing dps one.")
    }

    val meHealth = Player.getHealthPercentage

    //println("Player.getBuffRemainingTimeOpt(Lifebloom) " + Player.getBuffRemainingTimeOpt(Lifebloom))
    //println("Player.canCast(Lifebloom) " + Player.canCast(Lifebloom))
    //println("Player.getBuffRemainingTimeOpt(Rejuvenation)(tank) " + Player.getBuffRemainingTimeOpt(Rejuvenation)(tank))
    //println("Player.getBuffRemainingTimeOpt(Germination)(tank)) " + Player.getBuffRemainingTimeOpt(Germination)(tank))
    //1sendAction(Rejuvenation -> Some(HealerFour))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerTwo) " + Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerTwo))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(tank) " + Player.getBuffRemainingTimeOpt(Regrowth)(tank))
    //println("Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerOne1) " + Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerOne))
    if (meHealth > 1 && !Player.isCasting && !Player.isChanneling) {
      val relativeHealerOneHealth = Player.getHealthPercentage(relativeHealerOne)
      val relativeHealerTwoHealth = Player.getHealthPercentage(relativeHealerTwo)
      val relativeHealerThreeHealth = Player.getHealthPercentage(relativeHealerThree)
      //val tankHealth = Player.getHealthPercentage(tank)

      //val isInCombat = Player.isInCombat(tank)
      val isInCombat = Player.isInCombat

      val canCastBarkskin = Player.canCast(RestoBarkskin)
      val canCastTranquility = Player.canCast(Tranquility)
      val canCastSwiftmend = Player.canCast(Swiftmend)
      val canCastWildGrowth = Player.canCast(WildGrowth)
      //val canCastHealingTouch = Player.canCast(HealingTouch)
      val canCastIronbark = Player.canCast(Ironbark)

      val lowHealthToonsCount = List[Int](meHealth, relativeHealerOneHealth, relativeHealerTwoHealth, relativeHealerThreeHealth)//, otherHealerHealth)
        .map { health =>
          if (health < Configuration.MajorHealthThreshold && health > 1) {
            1
          } else {
            0
          }
        }
        .sum

      if (
        lowHealthToonsCount > 2 &&
          canCastTranquility &&
          isInCombat
      ) {
        sendAction(Tranquility -> None)
      } else if (lowHealthToonsCount > 2 && canCastWildGrowth && isInCombat) {
        sendAction(WildGrowth -> Some(Healer))
      } /*else if (
      //tankHealth > 1 &&
      //tankHealth < Configuration.CriticalHealthThreshold &&
      otherHealerHealth > 1 &&
        otherHealerHealth < Configuration.CriticalHealthThreshold &&
        isInCombat &&
        (canCastIronbark || canCastSwiftmend)
      ) {
        if (canCastSwiftmend) {
          sendAction(Swiftmend -> Some(otherHealerType))
        } else  {
          sendAction(Ironbark -> Some(otherHealerType))
        }
      }*/ else if (
        meHealth > 1 &&
          meHealth < Configuration.CriticalHealthThreshold &&
          isInCombat &&
          (canCastBarkskin || canCastSwiftmend)
      ) {
        if (canCastSwiftmend) {
          sendAction(Swiftmend -> Some(Healer))
        } else if (canCastBarkskin) {
          sendAction(RestoBarkskin -> None)
        }
      } else if (
        relativeHealerOneHealth > 1 &&
          relativeHealerOneHealth < Configuration.CriticalHealthThreshold &&
          isInCombat &&
          canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(HealerTwo))
      } else if (
        relativeHealerTwoHealth > 1 &&
          relativeHealerTwoHealth < Configuration.CriticalHealthThreshold &&
          isInCombat &&
          canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(HealerThree))
      } else if (
        relativeHealerThreeHealth > 1 &&
          relativeHealerThreeHealth < Configuration.CriticalHealthThreshold &&
          isInCombat &&
          canCastSwiftmend
      ) {
        sendAction(Swiftmend -> Some(HealerFour))
      } else if (
        (meHealth < Configuration.FullHealthThreshold && meHealth > 1) ||
          (relativeHealerOneHealth < Configuration.FullHealthThreshold && relativeHealerOneHealth > 1) ||
          (relativeHealerTwoHealth < Configuration.FullHealthThreshold && relativeHealerTwoHealth > 1) ||
          (relativeHealerThreeHealth < Configuration.FullHealthThreshold && relativeHealerThreeHealth > 1)// ||
          //(tankHealth < Configuration.FullHealthThreshold && tankHealth > 1)
      ) {
        /*val otherHealerHealthWeight = if (otherHealerHealth <= 1 || otherHealerHealth > 99) {
          (-100).toFloat
        } else {
          1 - otherHealerHealth.toFloat / 100f //+ 0.5f
        }*/

        val healerHealthWeight = if (meHealth <= 0 || meHealth > 99) {
          (-100).toFloat
        } else {
          1 - meHealth.toFloat / 100f //+ 0.4f
        }

        val relativeHealerOneHealthWeight = if (relativeHealerOneHealth <= 0 || relativeHealerOneHealth > 99) {
          (-100).toFloat
        } else {
          1 - relativeHealerOneHealth.toFloat / 100f //+ 0.3f
        }

        val relativeHealerTwoHealthWeight = if (relativeHealerTwoHealth <= 0 || relativeHealerTwoHealth > 99) {
          (-100).toFloat
        } else {
          1 - relativeHealerTwoHealth.toFloat / 100f //+ 0.2f
        }

        val relativeHealerThreeHealthWeight = if (relativeHealerThreeHealth <= 0 || relativeHealerThreeHealth > 99) {
          (-100).toFloat
        } else {
          1 - relativeHealerThreeHealth.toFloat / 100f //+ 0.1f
        }

        val canCastRejuvenation = Player.canCast(Rejuvenation)
        val canCastRegrowth = Player.canCast(Regrowth)
        //val canCastFlourish = Player.canCast(Flourish)
        val canCastLifebloom = Player.canCast(Lifebloom)
        var hotsCount = 0

        /*if (canCastLifebloom && !Player.getBuffRemainingTimeOpt(Lifebloom)(otherHealer).isDefined) {
          spellAndTargetToPriority(Lifebloom -> Some(otherHealerType)) += 6
        }*/

        if (canCastRejuvenation) {
          /*if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(otherHealer).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(otherHealerType)) += 4 + otherHealerHealthWeight
          } else {
            hotsCount += 1
          }*/

          if (!Player.getBuffRemainingTimeOpt(Rejuvenation).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 4 + healerHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(relativeHealerOne).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerTwo)) += 4 + relativeHealerOneHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(relativeHealerTwo).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerThree)) += 4 + relativeHealerTwoHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Rejuvenation)(relativeHealerThree).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerFour)) += 4 + relativeHealerThreeHealthWeight
          } else {
            hotsCount += 1
          }

          /*if (!Player.getBuffRemainingTimeOpt(Germination)(otherHealer).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(otherHealerType)) += 3 + otherHealerHealthWeight
          } else {
            hotsCount += 1
          }*/

          if (!Player.getBuffRemainingTimeOpt(Germination).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(Healer)) += 3 + healerHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Germination)(relativeHealerOne).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerTwo)) += 3 + relativeHealerOneHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Germination)(relativeHealerTwo).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerThree)) += 3 + relativeHealerTwoHealthWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Germination)(relativeHealerThree).isDefined) {
            spellAndTargetToPriority(Rejuvenation -> Some(HealerFour)) += 3 + relativeHealerThreeHealthWeight
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
          /*if (!Player.getBuffRemainingTimeOpt(Regrowth)(otherHealer).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(otherHealerType)) +=
              2 + otherHealerHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }*/

          if (!Player.getBuffRemainingTimeOpt(Regrowth).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(Healer)) += 2 + healerHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerOne).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(HealerTwo)) += 2 + relativeHealerOneHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerTwo).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(HealerThree)) += 2 + relativeHealerTwoHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }

          if (!Player.getBuffRemainingTimeOpt(Regrowth)(relativeHealerThree).isDefined) {
            spellAndTargetToPriority(Regrowth -> Some(HealerFour)) += 2 + relativeHealerThreeHealthWeight + clearcastingWeight
          } else {
            hotsCount += 1
          }
        }

        //if (canCastFlourish && hotsCount > 4) {
        //  spellAndTargetToPriority(Flourish -> None) += hotsCount
        //}

        //if (canCastHealingTouch) {
        //  spellAndTargetToPriority(HealingTouch -> Some(HealerTwo)) += relativeHealerOneHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(HealerThree)) += relativeHealerTwoHealthWeight
        //  spellAndTargetToPriority(HealingTouch -> Some(HealerFour)) += relativeHealerThreeHealthWeight
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
          } else {
            val hasCatForm = Player.hasBuff(CatForm)
            if (!hasCatForm && Player.canCast(CatForm)) {
              spellAndTargetToPriority(CatForm -> None) += 1
            } else if (hasCatForm) {
              val comboPoints = Player.getComboPoints
              println("comboPoints " + comboPoints)
              if (comboPoints == 5) {
                if (!Target.getDebuffRemainingTimeOpt(Rip).isDefined && Player.canCast(Rip)) {
                  spellAndTargetToPriority(Rip -> None) += 1
                } else if (Player.canCast(FerociousBite)) {
                  spellAndTargetToPriority(FerociousBite -> None) += 1
                }
              } else if (Player.getEnnemiesCountInRange > 2) {
                println("enter 1")
                if (Player.canCast(Swipe)) {
                  println("enter 2")
                  spellAndTargetToPriority(Swipe -> None) += 1
                }
              } else if (!Target.getDebuffRemainingTimeOpt(Rake).isDefined && Player.canCast(Rake)) {
                println("enter 3")
                spellAndTargetToPriority(Rake -> None) += 1
              } else if (Player.canCast(Shred)) {
                println("enter 4")
                spellAndTargetToPriority(Shred -> None) += 1
              } else {
                println("enter 1=5")
              }
            }
          }
          /*} else if (Player.canCast(SolarWrath)) {
            spellAndTargetToPriority(SolarWrath -> None) += 1
          }*/
        }

        getSpellAndTargetBasedOnPriorityOpt(spellAndTargetToPriority.toMap) match {
          case Some(spellAndTarget) => sendAction(spellAndTarget)
          case _ => //Logger.debug(s"${me.name} - Executing no attack.")
        }
      }
    }
  }

}

object DruidRestoCatWeave {

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
    //(Renewal, None) -> List(Keys.LShiftKey, Keys.F6),
    //(Flourish, None) -> List(Keys.LShiftKey, Keys.F7),
    (CatForm, None) -> List(Keys.LShiftKey, Keys.F7),
    (Rake, None) -> List(Keys.LShiftKey, Keys.F8),
    (Shred, None) -> List(Keys.LShiftKey, Keys.F9),
    (Rip, None) -> List(Keys.LShiftKey, Keys.F10),
    (FerociousBite, None) -> List(Keys.LShiftKey, Keys.F11),
    (Swipe, None) -> List(Keys.LShiftKey, Keys.F12),

    (Rejuvenation, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D1),
    (Regrowth, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D2),
    //(HealingTouch, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D3),
    (Swiftmend, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D4),
    (WildGrowth, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D5),
    (Efflorescence, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D6),
    (Ironbark, Some(HealerTwo)) -> List(Keys.LControlKey, Keys.D7),

    (Rejuvenation, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F1),
    (Regrowth, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F2),
    //(HealingTouch, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F3),
    (Swiftmend, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F4),
    (WildGrowth, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F5),
    (Efflorescence, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F6),
    (Ironbark, Some(HealerThree)) -> List(Keys.LControlKey, Keys.F7),

    (Rejuvenation, Some(HealerFour)) -> List(Keys.Alt, Keys.F1),
    (Regrowth, Some(HealerFour)) -> List(Keys.Alt, Keys.F2),
    //(HealingTouch, Some(HealerFour)) -> List(Keys.Alt, Keys.F3),
    (Swiftmend, Some(HealerFour)) -> List(Keys.Alt, Keys.F8),
    (WildGrowth, Some(HealerFour)) -> List(Keys.Alt, Keys.F5),
    (Efflorescence, Some(HealerFour)) -> List(Keys.Alt, Keys.F6),
    (Ironbark, Some(HealerFour)) -> List(Keys.Alt, Keys.F7),

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
