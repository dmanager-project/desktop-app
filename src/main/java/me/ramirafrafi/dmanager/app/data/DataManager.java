/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ramirafrafi.dmanager.app.data;

import me.ramirafrafi.dmanager.app.model.Download;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Admin
 */
public class DataManager {
    static public void saveDownload(Download download) throws IOException {
        String dirPath = System.getenv("APPDATA") + File.separator + "DManager";
        String filePath = dirPath + File.separator + "downloads.txt";
        try (FileWriter writer = new FileWriter(filePath, true)) {
            String line = download.getFileUrl() + " " + download.getFileName() + "\n";
            writer.append(line);
        }
    }
    
    static public void deleteDownload(Download download) throws FileNotFoundException, IOException {
        String dirPath = System.getenv("APPDATA") + File.separator + "DManager";
        String filePath = dirPath + File.separator + "downloads.txt";
        String lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            lines = "";
            while(null != (line = reader.readLine())) {
                String[] line_ = line.split(" ");
                if(!line_[0].equals(download.getFileUrl())) {
                    lines += line + "\n";
                }
            }
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(lines);
        }
    }
}
