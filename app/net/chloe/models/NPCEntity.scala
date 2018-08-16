package net.chloe.models

case class NPCEntity(
  val x: Float,
  val y: Float,
  val z: Float,
  val xAddress: Long,
  val yAddress: Long,
  val zAddress: Long,
  val guid: String,
  val name: String,
  val castingSpellId: Int,
  val targetGUID: String,
  val entryBase: Long
) extends WowUnit
