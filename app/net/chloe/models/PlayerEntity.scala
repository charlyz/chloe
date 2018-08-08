package net.chloe.models

case class PlayerEntity(
  val x: Float,
  val y: Float,
  val z: Float,
  val xAddress: Long,
  val yAddress: Long,
  val zAddress: Long,
  val angle: Float,
  val angleAddress: Long,
  val guid: String,
  val name: String,
  val targetNameOpt: Option[String],
  val targetGUIDOpt: Option[String],
  val entryBase: Long
) extends EntityLocation

trait EntityLocation {
  val x: Float
  val y: Float
  val z: Float
  val xAddress: Long
  val yAddress: Long
  val zAddress: Long
  val guid: String
}