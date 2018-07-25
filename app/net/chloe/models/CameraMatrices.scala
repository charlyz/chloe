package net.chloe.models

case class CameraMatrices(
  val xX: Float,
  val xY: Float,
  val xZ: Float,
  val yX: Float,
  val yY: Float,
  val yZ: Float,
  val zX: Float,
  val zY: Float,
  val zZ: Float,
  val originX: Float,
  val originY: Float,
  val originZ: Float,
  val fov: Float
)