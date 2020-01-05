/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ramirafrafi.dmanager.app.model;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import me.ramirafrafi.dmanager.lib.FileDownload;
import me.ramirafrafi.dmanager.lib.FileDownloadListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import org.asynchttpclient.Response;
import me.ramirafrafi.dmanager.lib.State;

/**
 *
 * @author Admin
 */
public class Download {
    private final SimpleStringProperty fileUrl = new SimpleStringProperty(null);
    private final SimpleStringProperty fileName = new SimpleStringProperty(null);
    private final SimpleLongProperty fileSize = new SimpleLongProperty(-1);
    private final SimpleLongProperty downloaded = new SimpleLongProperty(0);
    private final SimpleStringProperty downloadedRate = new SimpleStringProperty(null);
    private final SimpleStringProperty status = new SimpleStringProperty(null);
    private final SimpleStringProperty speed = new SimpleStringProperty(null);
    private final SimpleBooleanProperty finished = new SimpleBooleanProperty(false);
    
    private long lastBytes = 0;  
    private long lastMillis = System.currentTimeMillis();
    
    private final FileDownload fileDownload;
    
    private String getHumanReadableBytes(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    public Download(FileDownload file) {
        this.fileDownload = file;
        
        fileUrl.set(fileDownload.getFileUrl());
        fileName.set(fileDownload.getFileName());
        fileSize.set(fileDownload.getContentLength());
        downloaded.set(fileDownload.getDownloaded());
        status.set(fileDownload.getStatus().toString());
        
        downloaded.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            try {
                long bytesDiff = downloaded.getValue() - lastBytes;
                long millisDiff = System.currentTimeMillis() - lastMillis;

                if (millisDiff > 300) {
                    String downloadedStr = getHumanReadableBytes(downloaded.getValue());
                    String fileSizeStr = getHumanReadableBytes(fileSize.getValue());

                    downloadedRate.set(downloadedStr + " /" + fileSizeStr);
                    speed.set(getHumanReadableBytes(bytesDiff / millisDiff * 1000));

                    lastBytes = downloaded.getValue();
                    lastMillis = System.currentTimeMillis();
                }
            } catch(ArithmeticException ae) {}
        });
        
        fileDownload.setListener(new FileDownloadListener() {
            @Override
            public void onDownload(FileDownload fileDownload) {
                Download.this.status.set(fileDownload.getStatus().toString());
            }

            @Override
            public void onCompleted(State status, Response response) {
                Download.this.status.set(status.toString());
                Download.this.speed.set("");
                Download.this.downloadedRate.set("");
                
                if(status == State.COMPLETE) {
                    Download.this.finished.set(true);
                }
            }

            @Override
            public void onAdvance(long downloaded) {
                Download.this.downloaded.set(downloaded);
            }
            
            @Override
            public void onError(FileDownload fileDownload) {
                Download.this.status.set(fileDownload.getStatus().toString());
                Download.this.speed.set("");
            }

            @Override
            public void onHangon(FileDownload fileDownload) {
                Download.this.status.set(fileDownload.getStatus().toString());
                Download.this.speed.set("");
            }
        });
    }
    
    public FileDownload getFileDownload() {
        return fileDownload;
    }
    
    public String getFileName() {
        return fileName.get();
    }
    
    public String getFileUrl() {
        return fileUrl.get();
    }

    public boolean isFinished() {
        return finished.get();
    }
    
    public void setFinished(boolean finished) {
        this.finished.set(finished);
    }
    
    public SimpleStringProperty fileNameProperty() {
        return fileName;
    }
    
    public SimpleLongProperty fileSizeProperty() {
        return fileSize;
    }
    
    public SimpleLongProperty downloadedProperty() {
        return downloaded;
    }
    
    public SimpleStringProperty downloadedRateProperty() {
        return downloadedRate;
    }
    
    public SimpleStringProperty statusProperty() {
        return status;
    }
    
    public SimpleStringProperty speedProperty() {
        return speed;
    }
    
    public SimpleBooleanProperty finishedProperty() {
        return finished;
    }
    
}
