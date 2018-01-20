
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class DriveConnector{
    
    private static final String DRIVE_WALLPAPER_FOLDER_ID = "1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh";
    private static final String QUERY_STRING_DRIVE_WALLPAPER_FOLDER_IN_PARENTS = "'" + DRIVE_WALLPAPER_FOLDER_ID + "'" + " in parents";
    private static final String WALLPAPER_STORAGE_DIRECTORY = "src\\main\\resources\\downloads\\";
    
    private static Drive driveService;
    
    //checks if the Google Drive folder was successfully contacted.
    //Prints names of files contained in it.
    public static void checkResult(FileList result){
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            System.out.println("\nNo files found.");
        } else {
            System.out.println("\nFiles:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
    }
    
    //Selects a random wallpaper from the list
    public static String[] getRandomWallpaper(FileList result) throws IOException{
        List<File> files = result.getFiles();
        int numberOfWallpapers, index;
        String newWallpaperId, newWallpaperName;
        
        if (files == null || files.size() == 0) {
            throw new Error("No files found.");
        } else {
            //get random wallpaper
            numberOfWallpapers = files.size();
            Random rand = new Random();
            index = rand.nextInt(numberOfWallpapers);
            newWallpaperId = files.get(index).getId();
            newWallpaperName = files.get(index).getName();
            System.out.println(newWallpaperName + " " + newWallpaperId);
            
            return new String[] {newWallpaperId, newWallpaperName};
        }
    }
    
    //downloads the wallpaper with the given ID to the WALLPAPER_STORAGE_DIRECTORY
    public static void downloadImage(String wallpaperId, String wallpaperName) throws IOException{
        FileOutputStream fileToDownloadTo;
        ByteArrayOutputStream downloadBuffer;
        
        downloadBuffer = new ByteArrayOutputStream();
        fileToDownloadTo = new FileOutputStream(WALLPAPER_STORAGE_DIRECTORY + wallpaperName);
        
        driveService.files().get(wallpaperId)
            .executeMediaAndDownloadTo(downloadBuffer);
        
        downloadBuffer.writeTo(fileToDownloadTo);
    }
    
    public static void main(String args[]) throws IOException{
        String [] wallpaperMeta;
        
        // Build a new authorized API client service.
        driveService = DriveAPI.getDriveService();

        // Get the names and IDs for up to 10 files.
        FileList result = driveService.files().list()
                .setQ(QUERY_STRING_DRIVE_WALLPAPER_FOLDER_IN_PARENTS)
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        
        //check the output
        checkResult(result);
        
        //Select a random wallpaper from the list
        System.out.println("\nRandom Wallpaper");
        wallpaperMeta = getRandomWallpaper(result);
        
        downloadImage(wallpaperMeta[0], wallpaperMeta[1]);
    }
}