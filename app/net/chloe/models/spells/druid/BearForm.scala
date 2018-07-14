package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object BearForm extends Cooldown with Buff {
  val id = 5487
  val cooldownIndexInAddon = 10
  val buffIndexInAddon = 15
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}