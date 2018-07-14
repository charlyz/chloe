package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Sunfire extends Cooldown with Debuff {
  val id = 93402
  val cooldownIndexInAddon = 10
  val debuffIndexInAddon = 2
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}