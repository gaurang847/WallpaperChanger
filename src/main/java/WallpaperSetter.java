
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinDef;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

public class WallpaperSetter {
    
    public void setRegistryValues(){
        //Set wallpaper tiling off
        Advapi32Util.registrySetStringValue(
            HKEY_CURRENT_USER, "Control Panel\\Desktop", "TileWallpaper", "0"
        );
        
        //Set wallpaper to stretch the bitmap vertically and horizontally to fit the desktop
        Advapi32Util.registrySetStringValue(
            HKEY_CURRENT_USER, "Control Panel\\Desktop", "WallpaperStyle", "2"
        );
    }
    
    public void setWallpaper(String imagePath){
        SPI.INSTANCE.SystemParametersInfo(
                new WinDef.UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                new WinDef.UINT_PTR(0),
                imagePath,
                new WinDef.UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
    }
}