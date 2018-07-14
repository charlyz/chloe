package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Swiftmend extends Cooldown {
  val id = 18562
  val cooldownIndexInAddon = 7
  val hasCharges = true
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}