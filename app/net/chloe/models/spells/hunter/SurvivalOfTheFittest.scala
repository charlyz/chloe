package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object SurvivalOfTheFittest extends Cooldown {
  val id = 264735
  val cooldownIndexInAddon = 17
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}