package net.chloe.models.spells.priest

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object ShadowWordPain extends Cooldown with Debuff {
  val id = 589
  val cooldownIndexInAddon = 16
  val debuffIndexInAddon = 11
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}