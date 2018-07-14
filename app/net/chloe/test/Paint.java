package net.chloe.test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.Pointer;
import com.sun.jna.Native;


public class Paint extends JFrame {

    public BufferedImage capture(HWND hWnd) {

        HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

        RECT bounds = new RECT();
        User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        System.out.println("Width: " + width);
        System.out.println("Height: " + height);
        if (width == 0) return null;
        //width = 1;//bounds.right - bounds.left;
        //height = 1;//bounds.bottom - bounds.top;
        
        HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

        HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
        GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY);

        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
        GDI32.INSTANCE.DeleteDC(hdcMemDC);

        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        Memory buffer = new Memory(width * height * 4);
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

        GDI32.INSTANCE.DeleteObject(hBitmap);
        User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

        return image;

    }

    public static void main(String[] args) throws InterruptedException {
        new Paint();
    }

    BufferedImage image;

    public Paint() throws InterruptedException {
        HWND hWnd = User32.INSTANCE.FindWindow(null, "is1 (Ctrl+Alt+1) Terenn-Twisting Nether - Trinity");
        System.out.println(User32.INSTANCE.IsWindowVisible(hWnd));
        Paint self = this;
        StdCallLibraryExtra.INSTANCE.EnumWindows(new StdCallLibraryExtra.WNDENUMPROC() {

            int count;

            public boolean callback(HWND hWnd, Pointer userData) {
                byte[] windowText = new byte[512];
                StdCallLibraryExtra.INSTANCE.GetWindowTextA(hWnd, windowText, 512);
                String aa = Native.toString(windowText);
                
                String wText = (aa.isEmpty()) ? "" : "; text: " + aa;
                boolean isVisible = User32.INSTANCE.IsWindowVisible(hWnd);
                if (aa.contains("Trinity") && isVisible) {
                	System.out.println("Found window " + hWnd + ", total " + ++count + wText + " - visible: " + isVisible);
                	self.image = self.capture(hWnd);
                	String b = String.format("#%06X", (0xFFFFFF & self.image.getRGB(0, 0)));
                	System.out.println("pixel color: " + b);
                	if (self.image == null) return true;
                	pack();

                	setExtendedState(MAXIMIZED_BOTH);
                    setVisible(true);
                	try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                
                return true;
            }
        }, null);
        
        
        
        //this.image = capture(hWnd);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        setExtendedState(MAXIMIZED_BOTH);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(image, 20, 40, null);
    }



}