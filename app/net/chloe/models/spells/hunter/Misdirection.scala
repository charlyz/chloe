package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Misdirection extends Cooldown {
  val id = 34477
  val cooldownIndexInAddon = 20
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}