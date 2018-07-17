package net.chloe.win32

import com.sun.jna._
import com.sun.jna.platform.win32.{ Kernel32 => JnaKernel32 }
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.ptr._

trait Kernel32Extra extends JnaKernel32 with StdCallLibrary {
  
  def WriteProcessMemory(p: Pointer, address: Long, buffer: Pointer, size: Int, written: IntByReference): Boolean  

  def ReadProcessMemory(
    hProcess: Pointer, 
    inBaseAddress: Long, 
    outputBuffer: Pointer, 
    nSize: Int, 
    outNumberOfBytesRead: IntByReference
  ): Boolean  
     
  def GetLastError(): Int
  
}
