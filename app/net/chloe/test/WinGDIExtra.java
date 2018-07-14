package net.chloe.test;

import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinGDI;

public interface WinGDIExtra extends WinGDI {

    public DWORD SRCCOPY = new DWORD(0x00CC0020);
    public DWORD CAPTUREBLT = new DWORD(0x00CC0020 | 0x40000000);

}