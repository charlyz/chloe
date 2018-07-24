package net.chloe.win32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.WinDef.POINT;

public class MSLLHOOKSTRUCT extends Structure 
{
    public POINT pt;
    public int mouseData;
    public int flags;
    public int time;
    public ULONG_PTR dwExtraInfo;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    protected List getFieldOrder() {
        List fields = new ArrayList();
        fields.addAll(Arrays.asList(new String[]{"pt", "mouseData", "flags", "time", "dwExtraInfo"}));
        return fields;
    }
}
