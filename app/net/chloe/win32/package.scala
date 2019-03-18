package net.chloe

import com.sun.jna.Native
import com.sun.jna.platform.win32.{ Kernel32 => JnaKernel32, _ }
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.win32.W32APIOptions
import com.sun.jna.Pointer
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary

package object win32 {
  
  val User32 = Native
    .loadLibrary("user32", classOf[User32Extra], W32APIOptions.DEFAULT_OPTIONS)
    .asInstanceOf[User32Extra]
  
  val GDI32 = Native
    .loadLibrary("gdi32", classOf[GDI32Extra], W32APIOptions.DEFAULT_OPTIONS)
    .asInstanceOf[GDI32Extra]
  
  val Kernel32 = Native
    .loadLibrary("kernel32", classOf[Kernel32Extra], W32APIOptions.DEFAULT_OPTIONS)
    .asInstanceOf[Kernel32Extra]
  
  val Psapi = Native
    .loadLibrary("Psapi", classOf[PsapiExtra], W32APIOptions.DEFAULT_OPTIONS)
    .asInstanceOf[PsapiExtra]
    
  object MouseActions {
    val WM_LBUTTONDOWN = 0x0201
    val WM_LBUTTONUP = 0x0202
    val WM_XBUTTONDOWN = 0x020B
  }
}