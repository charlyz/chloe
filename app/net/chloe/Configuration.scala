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
    //val PlayerName = 0x2C62418
    
    object CTM {
      val CurrentX = 0x277D7EC + 0x0C//0x277D748
      val CurrentY = 0x277D7EC + 0x0C + 4//0x277D750
      val CurrentZ = 0x277D7EC + 0x0C + 8//0x277D758
      val Distance = 0x0277D7F8
    }
    
    object Camera {
      val Base1 = 0x2B5B530
      val Base2 = 0x3330
      val Origin = 0x10
      val MatrixX = 0x1C
      val MatrixY = 0x28
      val MatrixZ = 0x34
      val Fov = 0x40
      
    }
    
    object EntitiesList {
      val Base = 0x27E07B8
      val FirstEntry = 0x18
      val NextEntry = 0x70
      
      object Entry {
        val Type = 0x20
        val Descriptors = 0x08
        val GUID = 0x58
      }
      
      object Unit {
        val X = 0x1588
        val Y = 0x158C
        val Z = 0x1590
        val Angle = 0x1598
      }
      
      object NPC {
        val Name1 = 0xC68
        val Name2 = 0x080
      }
      
      object Player {
        val Target1 = 0x10
        val Target2 = 0x9C
      }
    }
    
    object NamesCache {
      val Base = 0x23D9488
      val NextEntry = 0x0
      
      object Entry {
        val GUID = 0x20
        val Name = 0x31
      }
    }
  }
  
}