/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ramirafrafi.dmanager.app.data;

import me.ramirafrafi.dmanager.lib.DownloadManager;
import me.ramirafrafi.dmanager.lib.FileDownload;
import me.ramirafrafi.dmanager.app.model.Download;
import java.util.ArrayList;
import me.ramirafrafi.dmanager.lib.State;
import me.ramirafrafi.dmanager.lib.StatefulRunnable;

/**
 *
 * @author Admin
 */
public class DataFilter {
    static public ArrayList<Download> loadFinished(DownloadManager dm) {
        return loadDownloadsByStatus(dm, State.COMPLETE);
    }
    
    static public ArrayList<Download> loadDownloading(DownloadManager dm) {
        return loadDownloadsByStatus(dm, State.DOWNLOADING, State.PENDING);
    }
    
    static public ArrayList<Download> loadUnfinished(DownloadManager dm) {
        return loadDownloadsByStatus(dm, State.STOPPED, State.ERROR);
    }
    
    static public ArrayList<Download> loadDownloadsByStatus(DownloadManager dm, State... status) {
        ArrayList<Download> downloads = new ArrayList<>();
        for(StatefulRunnable fileDownload: dm.getDownloads()) {
            for(State status_: status) {
                if(fileDownload.getStatus() == status_) {
                    downloads.add(new Download((FileDownload) fileDownload));
                    break;
                }
            }
        }
        
        return downloads;
    }
}
