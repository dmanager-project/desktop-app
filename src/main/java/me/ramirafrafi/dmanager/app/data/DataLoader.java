/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ramirafrafi.dmanager.app.data;

import me.ramirafrafi.dmanager.lib.FileDownload;
import me.ramirafrafi.dmanager.app.DManagerApp;
import me.ramirafrafi.dmanager.app.model.Download;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import org.apache.tika.mime.MimeTypeException;

/**
 *
 * @author Admin
 */
public class DataLoader {
    static public ArrayList<Download> loadDownloads() {
        String dirPath = 
                System.getenv("APPDATA") + File.separator + "DManager";
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdir();
        }
        
        String filePath = dirPath + File.separator + "downloads.txt";
        File file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException ex) {
            DManagerApp.alertError("Error writing to hard disk");
        }
        
        ArrayList<Download> downloads = new ArrayList<>();
        String error = null;
        try {
            try (Scanner fileScanner = new Scanner(file)) {
                while(fileScanner.hasNext()) {
                    String line = fileScanner.nextLine();
                    String[] line_ = line.split(" ");
                    FileDownload fileDownload = new FileDownload(line_[0], DManagerApp.DOWNLOAD_DIR, line_[1]);
                    Download download = new Download(fileDownload);
                    downloads.add(download);
                }
            }
        } catch (FileNotFoundException ex) {
            error = filePath + " not found";
        } catch (InterruptedException | ExecutionException ex) {
            error = "Error occured that interrupted receiving file informations";
        } catch (MimeTypeException ex) {
            error = "Couldn't recognise file mime type";
        } catch (MalformedURLException ex) {
            error = "Protocol or URL is malformed";
        } catch (IOException ex) {
            error = "Couldn't close ressources";
        } finally {
            if(null != error) {
                DManagerApp.alertError(error);
            }
        }
        
        return downloads;
    }
}
