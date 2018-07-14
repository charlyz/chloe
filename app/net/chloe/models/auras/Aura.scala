package net.chloe.models.auras

import net.chloe.models.spells._

trait Aura extends Spell

trait Buff extends Aura {
  val buffIndexInAddon: Int
}

trait Debuff extends Aura {
  val debuffIndexInAddon: Int
}