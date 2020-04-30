package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object CatForm extends Cooldown with Buff {
  val id = 57655
  val cooldownIndexInAddon = 23
  val buffIndexInAddon = 12
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}