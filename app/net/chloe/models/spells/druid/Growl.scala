package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Growl extends Cooldown {
  val id = 6795
  val cooldownIndexInAddon = 13
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}