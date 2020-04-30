package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Rake extends Cooldown with Debuff {
  val id = 1822
  val cooldownIndexInAddon = 18
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
  val debuffIndexInAddon = 4
  val radiusOpt = None
}