package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object AMurderOfCrows extends Cooldown {
  val id = 131894
  val cooldownIndexInAddon = 12
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}