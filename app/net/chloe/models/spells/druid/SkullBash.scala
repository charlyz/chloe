package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object SkullBash extends Cooldown {
  val id = 106839
  val cooldownIndexInAddon = 9
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}