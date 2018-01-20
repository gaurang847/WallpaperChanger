package com.testpackage;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import com.sun.jna.win32.*;
import java.util.HashMap;

public class JNATest {

    public static void main(String[] args) {
        //supply your own path instead of using this one
        String path = "C:\\Users\\DELL\\Pictures\\bossun_by_aizawanara-d9iakuu.jpg";

        System.out.println("rk test");
        System.out.println(Advapi32Util.registryKeyExists(HKEY_CURRENT_USER, "Control Panel\\Desktop"));
        Advapi32Util.registrySetStringValue(
            HKEY_CURRENT_USER, "Control Panel\\Desktop", "TileWallpaper", "0");
        Advapi32Util.registrySetStringValue(
            HKEY_CURRENT_USER, "Control Panel\\Desktop", "WallpaperStyle", "2");
                
        SPI.INSTANCE.SystemParametersInfo(
                new UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                new UINT_PTR(0),
                path,
                new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
    }

    public interface SPI extends StdCallLibrary {

        //from MSDN article
        long SPI_SETDESKWALLPAPER = 20;
        long SPIF_UPDATEINIFILE = 0x01;
        long SPIF_SENDWININICHANGE = 0x02;

        SPI INSTANCE = (SPI) Native.loadLibrary("user32", SPI.class, new HashMap<String, Object>() {
            {
                put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
                put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
            }
        });

        boolean SystemParametersInfo(
                UINT_PTR uiAction,
                UINT_PTR uiParam,
                String pvParam,
                UINT_PTR fWinIni
        );
    }
}
