package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object KillCommand extends Cooldown {
  val id = 34026
  val cooldownIndexInAddon = 3
  val hasCharges = false
  val needTarget = true // Pet needs target.
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}