package net.chloe.win32

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.{ User32 => JnaUser32 }
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.ptr._
import com.sun.jna.{ Memory => JnaMemory, _ }
import com.sun.jna.platform.win32.WinNT._
import com.sun.jna.platform.win32.WinDef._
import scala.collection.JavaConverters._
import scala.util._
import com.sun.jna.Native
import com.sun.jna.platform.win32._
import java.util.{ List => JList, ArrayList => JArrayList }
import scala.collection.JavaConverters._


object Memory {
  
  val PROCESS_VM_READ= 0x0010
  val PROCESS_ALL_ACCESS = 2035711
  
  def getProcessIdFromWindowHandle(hWnd: HWND) = {
     val pid = new IntByReference(0)
     User32.GetWindowThreadProcessId(hWnd, pid)

     pid.getValue()
  }
  
  def getBaseAddress(hProcess: HANDLE): Long = {
    val lphModule  = new Array[HMODULE](100 * 4)
    val lpcbNeeded = new IntByReference()

    val isEnumProcessModulesSucceeded = Psapi.EnumProcessModules(
      hProcess,
      lphModule,
      lphModule.length,
      lpcbNeeded
    )
    
    if (!isEnumProcessModulesSucceeded) {
      throw new Win32Exception(Native.getLastError)
    }

    val lpModuleInfo = getModuleInformation(hProcess.getPointer, lphModule(0).getPointer)

    Pointer.nativeValue(lpModuleInfo.EntryPoint)
  }
  
  def readPointer(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        8
      )
      .getPointer(0)
  }
  
  def readInt(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        4
      )
      .getInt(0)
  }
  
  def readByte(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        1
      )
      .getByte(0)
  }
  
  def readLong(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        8
      )
      .getLong(0)
  }
  
  def readGUID(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        16
      )
      .getByteArray(0, 8)
      .map("%02x".format(_))
      .mkString
      .toUpperCase()
  }
  
  def readFloat(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        4
      )
      .getFloat(0)
  }
  
  def readDouble(hProcess: HANDLE, address: Long) = {
    Memory
      .readMemory(
        hProcess.getPointer, 
        address, 
        8
      )
      .getDouble(0)
  }
  
  def readStringOpt(hProcess: HANDLE, address: Long) = {
    Option(
      Memory
        .readMemory(
          hProcess.getPointer, 
          address, 
          8 * 80
        )
        .getString(0)
    )
  }
    
  def getModuleInformation(hProcess: Pointer, hModule: Pointer) = {
    val lpModInfo = new LPMODULEINFO()

    val isGetModuleInformationSucceeded = Psapi.GetModuleInformation(hProcess, hModule, lpModInfo, lpModInfo.size())
    
    if (!isGetModuleInformationSucceeded) {
      throw new Exception(s"GetModuleInformation failed. Error: ${Native.getLastError()}")
    }
    
    lpModInfo
  }

  def readMemory(process: Pointer, address: Long, bytesToRead: Int) = {
    val read = new IntByReference(0)
    val output = new JnaMemory(bytesToRead)

    Kernel32.ReadProcessMemory(process, address, output, bytesToRead, read)
    output
  }
  
  
  
}