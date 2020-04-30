package net.chloe

import scala.concurrent.duration._
import net.chloe.models._

object Configuration {
  
  val ScalingRatio = 10
  val SlaveWindowWith = 688
  val PrimaryWindowWith = 3440
  val MinimumTimeBetweenInterrupt = 2.seconds
  val PauseBetweenActions = 250.millis//2.seconds//200.millis
  
  val CriticalHealthThreshold = 40
  val MajorHealthThreshold = 80
  val FullHealthThreshold = 95
  
  object ObjectTypes {
    val Item = 1
    val NPC = 5
    val Player = 6
    val LocalPlayer = 7
  }
  
  object CastingSpells {
    val DancingBlade = 193235
    val HornOfValor = 191284
  }
 
  object Aoes {
    val FreezingTrap = 187651
    val Flare = 132950
    val TarTrap = 187699
    val DancingBlade = 193214
    val CracklingStorm = 198892
    val Thunderstrike = 198605
  }
  
  object HydridNPCsAreaTriggers {
    val CracklingStorm = "Crackling Storm"
    val DancingBlade = "Dancing Blade"
  }
  
  val HarmfulAoes = List(
    Aoes.FreezingTrap,
    Aoes.Flare,
    Aoes.TarTrap,
    Aoes.DancingBlade,
    Aoes.CracklingStorm
  )
  
  object Offsets {
    //val PlayerName = 0x2C62418
    
    object IsRunning {
      
    }
    
    object GameUI {
      val Base = 0x2B688C0//0x2B5F710 
      val CursorX = 0x290
      val CursorY = 0x294
      val CursorZ = 0x298
    }
    
    object CTM {
      val CurrentX = 0x277D7EC + 0x0C//0x277D748
      val CurrentY = 0x277D7EC + 0x0C + 4//0x277D750
      val CurrentZ = 0x277D7EC + 0x0C + 8//0x277D758
      val Distance = 0x0277D7F8
    }
    
    object Camera {
      val Base1 = 0x2B68830
      val Base2 = 0x3330
      val Origin = 0x10
      val MatrixX = 0x1C
      val MatrixY = 0x28
      val MatrixZ = 0x34
      val Fov = 0x40
    }
    
    object EntitiesList {
      val Base = 0x27DCAB8
      val FirstEntry = 0x18
      val NextEntry = 0x70
      
      object Entry {
        val Type = 0x20
        val Descriptors = 0x08
        val GUID = 0x58
      }
      
      object AreaTrigger {
        val X = 0x1B0
        val Y = 0x1B0 + 4
        val Z = 0x1B0 + 8
        val Base = 560 * 4
        val SpellId = 29 * 4
        val Radius = 32 * 4
      }
      
      object Unit {
        val X = 0x1588
        val Y = 0x158C
        val Z = 0x1590
        val Angle = 0x1598
        val CastingSpellId = 0x18F8
        val Target1 = 0x10
        val Target2 = 0x9C
      }
      
      object NPC {
        val Name1 = 0x1740 
        val Name2 = 0xE0
      }
      
      object Player {
        val UnitSpeed1 = 0x198
        val UnitSpeed2 = 0xA4  
      }
    }
    
    object NamesCache {
      val Base = 0x23D5488
      val NextEntry = 0x0
      
      object Entry {
        val GUID = 0x20
        val Name = 0x31
      }
    }
  }
  
}