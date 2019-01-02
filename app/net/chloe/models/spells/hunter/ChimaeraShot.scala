package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object ChimaeraShot extends Cooldown {
  val id = 53209
  val cooldownIndexInAddon = 16
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}