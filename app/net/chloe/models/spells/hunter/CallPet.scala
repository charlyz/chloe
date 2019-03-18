package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object CallPet extends Spell {
  val id = 67777
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}