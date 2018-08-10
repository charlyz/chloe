package net.chloe.models

case class NPCEntity(
  val x: Float,
  val y: Float,
  val z: Float,
  val xAddress: Long,
  val yAddress: Long,
  val zAddress: Long,
  val guid: String,
  val name: String
) extends EntityLocation
