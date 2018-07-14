package net.chloe.models.spells

import scala.concurrent.duration._

trait Spell {
  val id: Int
  val hasCharges: Boolean
  val needTarget: Boolean
  val hasCooldownOtherThanGcd: Boolean
  val isInstant: Boolean
}

trait Cooldown extends Spell {
  val cooldownIndexInAddon: Int
}

trait Item extends Spell {
  val itemIndexInAddon: Int
}