package com.masquerade817;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main{
    public static void main(String args[]) throws IOException, GeneralSecurityException{
        String wallpaperPath;
        
        DriveConnector dc = new DriveConnector();
        WallpaperSetter ws = new WallpaperSetter();
    
        wallpaperPath = dc.getWallpaper();
        
        ws.setRegistryValues();
        ws.setWallpaper(wallpaperPath);
    }
}
