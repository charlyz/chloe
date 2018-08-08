package net.chloe.models.spells.hov

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Thunderstrike extends Debuff {
  val id = 198605
  val radiusOpt = Some(8f)
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
  val debuffIndexInAddon = 2
}