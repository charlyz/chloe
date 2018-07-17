package net.chloe.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LPMODULEINFO extends Structure {
	public Pointer lpBaseOfDll;	
	public int  SizeOfImage;
	public Pointer EntryPoint;
	
	@Override
    protected List getFieldOrder() {
        List fields = new ArrayList();
        fields.addAll(Arrays.asList(new String[]{"EntryPoint", "SizeOfImage", "lpBaseOfDll"}));
        return fields;
    }
}