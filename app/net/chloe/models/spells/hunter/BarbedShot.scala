package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object BarbedShot extends Cooldown {
  val id = 217200
  val cooldownIndexInAddon = 14
  val hasCharges = true
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}