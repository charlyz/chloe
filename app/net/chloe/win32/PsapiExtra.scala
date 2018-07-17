package net.chloe.win32

import com.sun.jna._
import com.sun.jna.platform.win32.{ User32 => JnaUser32 }
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.ptr._
import com.sun.jna.platform.win32.WinNT._
import com.sun.jna.platform.win32.WinDef._
import scala.collection.JavaConverters._
import scala.util._
import com.sun.jna.Native
import com.sun.jna.platform.win32._

trait PsapiExtra extends StdCallLibrary {
  
  def EnumProcesses(pProcessIds: Array[Int], cb: Int, pBytesReturned: IntByReference): Boolean

  def EnumProcessModules(hProcess: HANDLE, lphModule: Array[HMODULE], cb: Int, lpcbNeededs: IntByReference): Boolean
    
  def EnumProcessModulesEx(
    hProcess: HANDLE, 
    lphModule: Array[HMODULE], 
    cb: Int, 
    lpcbNeededs: IntByReference, 
    flags: Int
  ): Boolean

  def GetModuleFileNameExA(hProcess: HANDLE, hModule: HMODULE, lpImageFileName: Array[Byte], nSize: Int): Int
    
  def GetModuleBaseNameA(hProcess: HANDLE, hModule: HMODULE, lpImageFileName: Array[Byte], nSize: Int): Int

  def GetModuleInformation(hProcess: Pointer, hModule: Pointer, lpmodinfo: LPMODULEINFO, cb: Int): Boolean
  
}
