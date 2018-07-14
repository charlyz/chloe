package net.chloe

import scala.concurrent.duration._
import net.chloe.models._

object Configuration {
  
  val ScalingRatio = 10
  val SlaveWindowWith = 960
  val PrimaryWindowWith = 3840
  val MinimumTimeBetweenInterrupt = 2.seconds
  val PauseBetweenActions = 300.millis//2.seconds//200.millis
  
  val CriticalHealthThreshold = 40
  val MajorHealthThreshold = 70
  val FullHealthThreshold = 95
  
  val Nameplates = new AnyRef {
    val PriorityMobs = Map(
      "Voidgorged Stalker" -> Color(14235162)
    )
  }
  
}