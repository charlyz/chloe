package net.chloe.test;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.Pointer;
import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;

public interface StdCallLibraryExtra extends StdCallLibrary, WinUser, WinNT {

	StdCallLibraryExtra INSTANCE = (StdCallLibraryExtra) Native.loadLibrary("user32", StdCallLibraryExtra.class, W32APIOptions.DEFAULT_OPTIONS);

    interface WNDENUMPROC extends StdCallCallback {
        boolean callback(HWND hWnd, Pointer arg);
    }

    boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer arg);

    int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
    HWND GetWindow(HWND hWnd, DWORD uCmd);

}