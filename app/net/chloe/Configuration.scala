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
  
  object ObjectTypes {
    val Item = 1
    val NPC = 5
    val Player = 6
    val LocalPlayer = 7
  }
  
  object Offsets {
    val PlayerName = 0x2C62418
    
    object Camera {
      val Base1 = 0x1BEE420
      val Base2 = 0x3328
      val Origin = 0x10
      val MatrixX = 0x1C
      val MatrixY = 0x28
      val MatrixZ = 0x34
      val Fov = 0x40
    }
    
    object EntitiesList {
      val Base = 0x27FD798
      val FirstEntry = 0x18
      val NextEntry = 0x70
      
      object Entry {
        val Type = 0x20
        val Descriptors = 0x10
        val GUID = 0x58
      }
      
      object Unit {
        val X = 0x1588
        val Y = 0x158C
        val Z = 0x1590
        val Angle = 0x1592
      }
      
      object NPC {
        val Name1 = 0xC68
        val Name2 = 0x080
      }
    }
    
    object NamesCache {
      val Base = 0x1004C3C
      val NextEntry = 0x0
      
      object Entry {
        val GUID = 0x10
        val Name = 0x21
      }
    }
  }
  
}