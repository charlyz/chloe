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
  
    
  def getModuleInformation(hProcess: Pointer, hModule: Pointer) = {
    classOf[LPMODULEINFO].getDeclaredFields.foreach { a =>
      println(a.getName)
    }
    val lpModInfo = new LPMODULEINFO()
    println(s"Error: ${Native.getLastError()}")
    println("size lpmodinfo " + lpModInfo.size())
    val success = Psapi.GetModuleInformation(hProcess, hModule, lpModInfo, lpModInfo.size())
    
    if (!success) {
      println(lpModInfo.lpBaseOfDll)
      println(lpModInfo.EntryPoint)
      println(lpModInfo.SizeOfImage)
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