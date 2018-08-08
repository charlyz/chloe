package net.chloe.win32

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.{ User32 => JnaUser32 }
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef._
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.ptr._

trait User32Extra extends JnaUser32 with StdCallLibrary with WinUser with WinNT {
  
  def GetWindowDC(hWnd: HWND): HDC

  def GetClientRect(hWnd: HWND, rect: RECT): Boolean

  def EnumWindows(lpEnumFunc: WNDENUMPROC, arg: Pointer): Boolean

  def GetWindowTextA(hWnd: HWND, lpString: Array[Byte], nMaxCount: Int): Int
  
  def GetWindow(hWnd: HWND, uCmd: DWORD): HWND

  def SendMessage(hWnd: HWND, Msg: Int, wParam: Int, lParam: Int): Int
  
  def SendMessage(hWnd: HWND, Msg: Int, wParam: WPARAM, lParam: LPARAM): Int
  
  def FindWindowA(winClass: String, title: String): Pointer
  
  def GetWindowThreadProcessId(hWnd: Pointer, lpdwProcessId: IntByReference): Int
  
  def ClientToScreen(hWnd: HWND, pt: POINT): Boolean

}
