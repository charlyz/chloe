package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Shred extends Cooldown {
  val id = 5221
  val cooldownIndexInAddon = 19
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}