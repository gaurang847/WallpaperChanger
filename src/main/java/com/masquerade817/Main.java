package com.masquerade817;

import java.io.IOException;

public class Main{
    public static void main(String args[]) throws IOException{
        String wallpaperPath;
        
        DriveConnector dc = new DriveConnector();
        WallpaperSetter ws = new WallpaperSetter();
    
        wallpaperPath = dc.getWallpaper();
        
        ws.setRegistryValues();
        ws.setWallpaper(wallpaperPath);
    }
}
