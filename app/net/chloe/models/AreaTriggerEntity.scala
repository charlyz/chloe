package net.chloe.models

case class AreaTriggerEntity(
  val id: Long,
  val radius: Float,
  val x: Float,
  val y: Float,
  val z: Float,
  val safeSpotsOpt: Option[Set[(Float, Float, Float)]] = None
)