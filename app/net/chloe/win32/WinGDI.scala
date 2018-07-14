package net.chloe.win32

import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinGDI

object WinGDIExtra {

    val SRCCOPY = new DWORD(0x00CC0020)
    val CAPTUREBLT = new DWORD(0x00CC0020 | 0x40000000)

}