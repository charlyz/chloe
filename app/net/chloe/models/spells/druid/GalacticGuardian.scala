package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object GalacticGuardian extends Buff {
  val id = 203964
  val buffIndexInAddon = 17
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = false
}