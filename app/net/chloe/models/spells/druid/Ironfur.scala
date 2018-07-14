package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Ironfur extends Cooldown with Buff {
  val id = 192081
  val cooldownIndexInAddon = 7
  val buffIndexInAddon = 13
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}