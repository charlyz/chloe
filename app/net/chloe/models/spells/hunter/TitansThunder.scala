package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object TitansThunder extends Cooldown {
  val id = 207068
  val cooldownIndexInAddon = 10
  val hasCharges = false
  val needTarget = true // Pet needs a target.
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}