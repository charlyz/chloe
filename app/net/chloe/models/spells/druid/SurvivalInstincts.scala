package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object SurvivalInstincts extends Cooldown with Buff {
  val id = 61336
  val cooldownIndexInAddon = 6
  val buffIndexInAddon = 12
  val hasCharges = true
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}