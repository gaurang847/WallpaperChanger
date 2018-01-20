
import java.io.IOException;


public class Main{
    public static void main(String args[]) throws IOException{
        String wallpaperPath;
        
        DriveConnector dc = new DriveConnector(true);
        WallpaperSetter ws = new WallpaperSetter();
        
        wallpaperPath = dc.getWallpaper();
        System.out.println("wallpaperPath: " + wallpaperPath);
        ws.setRegistryValues();
        ws.setWallpaper(wallpaperPath);
    }
}
