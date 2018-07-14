package net.chloe.win32

import com.sun.jna.Native
import com.sun.jna.platform.win32.{ GDI32 => JnaGDI32 }
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.win32.W32APIOptions

trait GDI32Extra extends JnaGDI32 {

  def BitBlt(
    hObject: HDC, 
    nXDest: Int, 
    nYDest: Int, 
    nWidth: Int, 
    nHeight: Int, 
    hObjectSource: HDC, 
    nXSrc: Int, 
    nYSrc: Int, 
    dwRop: DWORD
  ): Boolean
  
}
