package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Ironbark extends Cooldown with Buff {
  val id = 102342
  val cooldownIndexInAddon = 14
  val buffIndexInAddon = 8
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}