package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object DireBeast extends Cooldown {
  val id = 120679
  val cooldownIndexInAddon = 2
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}