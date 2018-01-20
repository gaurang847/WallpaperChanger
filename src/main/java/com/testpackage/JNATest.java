package com.testpackage;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.win32.W32APIOptions;

public class JNATest {    
 public static interface User32 extends Library {
     User32 INSTANCE = (User32) Native.loadLibrary("user32",User32.class,W32APIOptions.DEFAULT_OPTIONS);        
     boolean SystemParametersInfo (int one, int two, String s ,int three);         
 }
 
public static void main(String[] args) {   
    System.out.println(User32.INSTANCE.SystemParametersInfo(0x0014, 0, "C:\\Users\\DELL\\Pictures\\bossun_by_aizawanara-d9iakuu.jpg" , 1));
    //User32.INSTANCE.SystemParametersInfo(0x0014, 0, "C:\\Users\\DELL\\Pictures\\bossun_by_aizawanara-d9iakuu.jpg" , 1);
    
   }
 }
//
//import com.sun.jna.Native;
//import com.sun.jna.platform.win32.WinDef.UINT_PTR;
//import com.sun.jna.win32.*;
//import java.util.HashMap;
//
//public class JNATest {
//   public static void main(String[] args) {
//      //supply your own path instead of using this one
//      String path = "C:\\Users\\DELL\\Pictures\\bossun_by_aizawanara-d9iakuu.jpg";
//
//      SPI.INSTANCE.SystemParametersInfo(
//          new UINT_PTR(SPI.SPI_SETDESKWALLPAPER), 
//          new UINT_PTR(0), 
//          path, 
//          new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
//   }
//
//   public interface SPI extends StdCallLibrary {
//
//      //from MSDN article
//      long SPI_SETDESKWALLPAPER = 20;
//      long SPIF_UPDATEINIFILE = 0x01;
//      long SPIF_SENDWININICHANGE = 0x02;
//
//      SPI INSTANCE = (SPI) Native.loadLibrary("user32", SPI.class, new HashMap<String, Object>() {
//         {
//            put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
//            put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
//         }
//      });
//
//      boolean SystemParametersInfo(
//          UINT_PTR uiAction,
//          UINT_PTR uiParam,
//          String pvParam,
//          UINT_PTR fWinIni
//        );
//  }
//}