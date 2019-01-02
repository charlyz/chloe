package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object MendPet extends Cooldown {
  val id = 982
  val cooldownIndexInAddon = 18
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}