package atlas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;


public class Controller_content {

    public Button uploadIDs_btn;
    public Button target_dir_btn;
    public Label atlasUrl_lb;
    private List<String> atlasId;
    private List<String> file_source_list = null;
    private String title = "Atlas";
    private static String username;
    private static String password;

    @FXML
    private ListView listView;

    @FXML
    private Label target_dir_label;

    @FXML
    private Button copyFile;

    public Controller_content() {
    }

    @FXML
    private TextField atlasUrl_tf;

    public static void setUsername(String username) {
        Controller_content.username = username;
    }
    public static void setPassword(String password) {
        Controller_content.password = password;
    }

    @FXML
    private void getSourceDirectory() {
        FileChooser fileChooser = new FileChooser();
        File id = fileChooser.showOpenDialog(Main.stage);

        atlasId = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(id))){
            String line;
            while ((line = reader.readLine()) != null){
                atlasId.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ObservableList<String> atlasId_ol = FXCollections.observableArrayList(atlasId);
        listView.setItems(atlasId_ol);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setDisable(false);
    }

    @FXML
    private void getTargetDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("/Users/" + System.getProperty("user.name") + "/Downloads"));
        File targetDir = directoryChooser.showDialog(Main.stage);
        if (targetDir != null && targetDir.isDirectory()) {
            target_dir_label.setText(targetDir.toString());
        }
    }

    @FXML
    private void copyFile() {
        if (listView.getSelectionModel().getSelectedItems().isEmpty() || target_dir_label.getText().isEmpty() || atlasUrl_tf.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Source, Target Directory or URL cannot be blank");
            alert.show();
        } else {
            ProgressDialog.ProgressForm progressForm = new ProgressDialog.ProgressForm();
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    int counter = 0;
                    atlasId = listView.getSelectionModel().getSelectedItems();
                    List<InputStream> inputStreams = null;
                    for (String id1 : atlasId){
                        String url = atlasUrl_tf.getText() + id1 + "/";
                        inputStreams = readDirectory(url);
                        Path directory = Paths.get(target_dir_label.getText() + "/" + title);
                        Files.createDirectory(directory);
                        String target = directory.toString();
                        int countInner = 0;
                        for (InputStream copy : inputStreams) {
                                String name = file_source_list.get(countInner);
                                String actual_name = name.substring(name.lastIndexOf("/") + 1);
                                String copiedFile = target + "/" + actual_name;
                                Files.copy(copy, Paths.get(copiedFile), StandardCopyOption.REPLACE_EXISTING);
                                countInner++;
                                if (atlasId.size() == 1){
                                    updateProgress(countInner,inputStreams.size());
                                }
                        }
                        updateProgress(counter++,atlasId.size());
                    }
                    if (atlasId.size() == 1){
                        updateProgress(inputStreams.size(),inputStreams.size());
                    }else {
                        updateProgress(atlasId.size(), atlasId.size());
                    }
                    return null;
                }
            };
            progressForm.activateProgressBar(task);
            progressForm.getDialogStage().show();
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    progressForm.getDialogStage().close();
                    copyFile.setDisable(false);
                }
            });
            task.setOnCancelled(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    progressForm.getDialogStage().close();
                    copyFile.setDisable(false);
                }
            });
            copyFile.setDisable(true);
            Thread thread = new Thread(task);
            thread.start();
        }
    }

    @FXML
    private void logOff() throws Exception {
        Controller_content.setPassword(null);
        Controller_content.setUsername(null);
        Main.replaceSceneContent("atlas.fxml");
    }

    private List<InputStream> readDirectory(String path) throws IOException {
        URL url = new URL(path);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        Authenticator.setDefault (new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication (username, password.toCharArray());
            }
        });
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        List<InputStream> inputStreams = new ArrayList<>();
        if (httpURLConnection.getResponseCode() == 200){
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {

                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append( line );
                }

                Document document = Jsoup.parse(stringBuilder.toString());
                title = document.getElementsByClass("componentDetails").first().child(0).ownText();
                file_source_list = new ArrayList<>();
                for (Element s : document.getElementsByClass("imageInLightBoxDiv")){
                    if (s.hasAttr("src")){
                        file_source_list.add(s.attr("src"));
                        String path2 = s.attr("src");
                        String file_path = url.toString() + path2.replaceAll(" ","%20");
                        URL newUrl = new URL(file_path);
                        inputStreams.add(getFiles(newUrl));
                    }
                }

                System.out.println(file_source_list);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }else {
            System.out.println(httpURLConnection.getResponseCode()+": "+httpURLConnection.getResponseMessage());
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return inputStreams;
    }

    private InputStream getFiles(URL file_path) {
        InputStream stream = null;
        try {
            stream = file_path.openStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream;
    }
}
