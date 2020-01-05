package me.ramirafrafi.dmanager.app;

import me.ramirafrafi.dmanager.lib.DownloadManager;
import me.ramirafrafi.dmanager.lib.FileDownload;
import me.ramirafrafi.dmanager.app.data.DataFilter;
import me.ramirafrafi.dmanager.app.data.DataLoader;
import me.ramirafrafi.dmanager.app.data.DataManager;
import me.ramirafrafi.dmanager.app.model.Download;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.apache.tika.mime.MimeTypeException;
import me.ramirafrafi.dmanager.lib.State;
import me.ramirafrafi.dmanager.lib.StatefulRunnable;

public class FXMLController implements Initializable {
    static public DownloadManager downloadManager = new DownloadManager();
    
    @FXML
    private Button downloadingBtn;
    
    @FXML
    private Button finishedBtn;
    
    @FXML
    private Button unfinishedBtn;
    
    @FXML
    private Text title;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    @FXML
    private TableView<Download> downloadsTable;
    
    @FXML
    private Button resumeBtn;
    
    @FXML
    private Button stopBtn;
    
    @FXML
    private Button deleteBtn;
    
    private Button active;
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);
    private ObservableList<Download> downloads = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        active = downloadingBtn;
        loadingIndicator.visibleProperty().bind(loading);
        setupDownloadsTable();
        loadDownloads();
        loadRows();
    }
    
    @FXML
    public void sideMenuBtnClicked(ActionEvent event) {
        Button source = (Button)event.getSource();
        if (active == source) {
            return;
        }
        
        active.getStyleClass().remove("active");
        source.getStyleClass().add("active");
        active = source;
        
        changeTitle();
        loadRows();
    }
    
    @FXML
    public void newDownload(ActionEvent event) throws IOException, MalformedURLException, InterruptedException, ExecutionException, MimeTypeException {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New download");
        dialog.setContentText("Copy the file's url here");
        dialog.setHeaderText("");
        final Optional<String> url = dialog.showAndWait();
        
        if(url.isPresent()) {
            final Task<Download> task = new Task<Download>() {
                @Override
                protected Download call() throws Exception {
                    for(StatefulRunnable fileDownload: downloadManager.getDownloads()) {
                        if(url.get().equals(((FileDownload) fileDownload).getFileUrl())) {
                            return null;
                        }
                    }
                    
                    FileDownload fileDownload = new FileDownload(url.get(), 
                            DManagerApp.DOWNLOAD_DIR);
                    Download download = new Download(fileDownload);
                    DataManager.saveDownload(download);
                    return download;
                }
            };
            
            task.setOnRunning((WorkerStateEvent e) -> {
                loadRows();
            });
            
            task.setOnSucceeded((WorkerStateEvent e) -> {
                Download download = task.getValue();
                if(null == download) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Download information");
                    alert.setContentText("Trying to download an already downloaded file?");
                    alert.setHeaderText("");
                    alert.show();
                } else {
                    if(active == downloadingBtn) {
                        downloads.add(download);
                    }
                    downloadManager.newTask(download.getFileDownload(), true);
                }
                
                loading.set(false);
            });
            
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
            loading.set(true);
        }
    }
    
    @FXML
    public void resumeDownload(ActionEvent event) {
        Download download = (Download) downloadsTable.getSelectionModel().getSelectedItem();
        downloadsTable.getSelectionModel().clearSelection();
        
        downloadManager.resumeTask(download.getFileDownload());
    }
    
    @FXML
    public void stopDownload(ActionEvent event) {
        Download download = (Download) downloadsTable.getSelectionModel().getSelectedItem();
        downloadsTable.getSelectionModel().clearSelection();
        
        downloadManager.stopTask(download.getFileDownload());
    }
    
    @FXML
    public void deleteDownload(ActionEvent event) {
        final Download download = (Download) downloadsTable.getSelectionModel().getSelectedItem();
        downloadsTable.getSelectionModel().clearSelection();

        final Task<Object> task = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                downloadManager.removeTask(download.getFileDownload());
                downloads.remove(download);
                DataManager.deleteDownload(download);
                (new File(download.getFileDownload().getFilePath())).delete();

                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
    
    private void loadDownloads() {
        loading.set(true);
        
        final Task<ArrayList<Download>> task = new Task<ArrayList<Download>>() {
            @Override
            protected ArrayList<Download> call() throws Exception {
                return DataLoader.loadDownloads();
            }
        };
        task.setOnSucceeded((WorkerStateEvent event) -> {
            downloads = FXCollections.observableArrayList(task.getValue());
            for (Download download: downloads) {
                downloadManager.newTask(download.getFileDownload(), false);
            }
            loading.set(false);
        });
        task.setOnFailed((WorkerStateEvent event) -> {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, task.getException());
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void loadRows() {
        loading.set(true);
        
        Task<ArrayList<Download>> task = new Task<ArrayList<Download>>() {
            @Override
            protected ArrayList<Download> call() throws Exception {
                return new ArrayList<>();
            }
        };
        if (active == downloadingBtn) {
            task = new Task<ArrayList<Download>>() {
                @Override
                protected ArrayList<Download> call() throws Exception {
                    return DataFilter.loadDownloading(downloadManager);
                }
            };
        } else if (active == finishedBtn) {
            task = new Task<ArrayList<Download>>() {
                @Override
                protected ArrayList<Download> call() throws Exception {
                    return DataFilter.loadFinished(downloadManager);
                }
            };
        } else if (active == unfinishedBtn) {
            task = new Task<ArrayList<Download>>() {
                @Override
                protected ArrayList<Download> call() throws Exception {
                    return DataFilter.loadUnfinished(downloadManager);
                }
            };
        }

        task.setOnSucceeded((WorkerStateEvent event) -> {
            @SuppressWarnings("unchecked")
            Worker<ArrayList<Download>> task1 = event.getSource();
            downloads = FXCollections.observableArrayList(task1.getValue());
            downloadsTable.setItems(downloads);
            loading.set(false);
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void changeTitle() {
        if (active == downloadingBtn) {
            title.setText("Currently downloading");
        } else if (active == finishedBtn) {
            title.setText("Finished");
        } else if (active == unfinishedBtn) {
            title.setText("Unfinished");
        }
    }

    private void setupDownloadsTable() {
        TableColumn<Download, ?> nameColumn = downloadsTable.getColumns().get(0);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        
        TableColumn<Download, ?> statusColumn = downloadsTable.getColumns().get(1);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        TableColumn<Download, ?> speedColumn = downloadsTable.getColumns().get(2);
        speedColumn.setCellValueFactory(new PropertyValueFactory<>("speed"));
        
        TableColumn<Download, ?> downloadedColumn = downloadsTable.getColumns().get(3);
        downloadedColumn.setCellValueFactory(new PropertyValueFactory<>("downloadedRate"));
        
        downloadsTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Download> observable, Download oldValue, Download newValue) -> {
            if(null == newValue) {
                resumeBtn.setDisable(true);
                stopBtn.setDisable(true);
                deleteBtn.setDisable(true);
                return;
            }
            
            Download download = (Download)newValue;
            State downloadStatus = download.getFileDownload().getStatus();
            switch (downloadStatus) {
                case STOPPED:
                case ERROR:
                    resumeBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    deleteBtn.setDisable(false);
                    break;
                case PENDING:
                case DOWNLOADING:
                    resumeBtn.setDisable(true);
                    stopBtn.setDisable(false);
                    deleteBtn.setDisable(true);
                    break;
                case COMPLETE:
                    resumeBtn.setDisable(true);
                    stopBtn.setDisable(true);
                    deleteBtn.setDisable(false);
                    break;
            }
        });
    }
}
