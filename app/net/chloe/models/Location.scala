package net.chloe.models

case class Location(
  val x: Float,
  val y: Float,
  val z: Float,
  val angle: Float,
  val guid: String,
  val name: String
)