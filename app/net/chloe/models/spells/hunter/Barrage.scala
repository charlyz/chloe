package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Barrage extends Cooldown {
  val id = 120360
  val cooldownIndexInAddon = 15
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = false
}