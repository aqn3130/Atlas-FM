package atlas;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.util.logging.Level;
import java.util.logging.Logger;
public class Controller {

    @FXML
    private TextField username;
    @FXML
    private TextField password;

    @FXML
    private Button login;

    @FXML
    private void signIn(){
        try {
            logIn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void btnSignIn() {
        try {
            logIn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void logIn() throws Exception {
        if (username.getText().isEmpty() && password.getText().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Username or Password cannot be blank");
            alert.show();
        }else {
            try {
                Controller_content.setUsername(username.getText());
                Controller_content.setPassword(password.getText());
                Main.replaceSceneContent("content.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}