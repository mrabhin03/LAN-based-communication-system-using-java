import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;
import javafx.scene.text.*;
import java.net.Socket;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import javafx.util.Duration;



public class Client extends Application {
    private TextField inputField;
    private PrintWriter out;
    ComboBox<String> selector;
    String SelectedUser=null;
    private TextFlow chatFlow;
    ScrollPane scrollPane;
    String SystemUser;
    String Me="rgb(230, 230, 230)",Other="rgb(223, 223, 223)";
    

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        String[] inputs=UserName();
        String UserName =SystemUser= inputs[2];
        selector = new ComboBox<>();
        selector.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selector, Priority.ALWAYS);
        if(!connectToServer(inputs)){
            return;
        }

        chatFlow = new TextFlow();
        chatFlow.setPadding(new Insets(10));
        chatFlow.setLineSpacing(5);

        scrollPane = new ScrollPane(chatFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setPrefHeight(400);

        inputField = new TextField();
        inputField.setPromptText("Type your message...");
        Button sendButton = new Button("Send");

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox topBox = new HBox(selector);
        topBox.setPadding(new Insets(10));
        topBox.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(scrollPane);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);
        stage.setTitle("Logged as - " + UserName);
        stage.show();
        

        sendButton.setOnAction(e -> sendMessage(UserName));
        inputField.setOnAction(e -> sendMessage(UserName));
        selector.setOnAction(e -> {
            SelectedUser = selector.getValue();
            getChatDetails(UserName,SelectedUser);
        });
    }

    private String[] UserName() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Connect to Server");
        dialog.setHeaderText("Enter your login details:");

        ButtonType loginButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server Host");


        TextField usernameField = new TextField("User1");
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Server IP:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new String[]{ipField.getText(), passwordField.getText(), usernameField.getText()};
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    public void getChatDetails(String SuperUser,String To){
        try{
            chatFlow.getChildren().clear();
            String Line;
            String FileName=encrypt(SuperUser + "-" + To);
            BufferedReader read=new BufferedReader(new FileReader("Msg/" + FileName + ".txt"));
            while (( Line=read.readLine())!=null) {
                Line=decrypt(Line);
                boolean isMe = Line.startsWith("Me :");
                String Owner = isMe ? "Me" : To + "";
                Text senderText = new Text(Owner);
                senderText.setFill(isMe ? Color.DARKBLUE : Color.DARKGREEN);
                senderText.setStyle("-fx-font-weight: bold;");
                senderText.setWrappingWidth(280); 
                
                Text msgText = new Text(Line.replace(Owner+" : ", ""));
                msgText.setFill(Color.BLACK);
                msgText.setWrappingWidth(280);
                
                VBox messageBox = new VBox(10, senderText, msgText);
                messageBox.setStyle("-fx-background-color:"+((isMe)?Me:Other)+"; -fx-padding: 8; -fx-background-radius: 10;");
                messageBox.setMaxWidth(300);

                HBox wrapper = new HBox(messageBox);
                wrapper.setAlignment(isMe ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT); 
                wrapper.setPadding(new Insets(5));

                chatFlow.getChildren().add(wrapper);


            }
            read.close();
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.millis(300));
                delay.setOnFinished(event -> {
                    scrollPane.layout(); 
                    scrollPane.setVvalue(1.0); 
                });
                delay.play();
            });
        }catch(IOException e){

        }
    }
    public void setChatDetails(String SuperUser,String From,String Msg,boolean me){
        try {
            File dir = new File("Msg");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String FileName=encrypt(SuperUser + "-" + From);

            FileWriter file = new FileWriter("Msg/" + FileName + ".txt", true);
            String text = (me ? "Me : " : From + " : ") + Msg;;
            file.write(encrypt(text)+"\n");
            file.close();
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.millis(1000));
                delay.setOnFinished(event -> {
                    scrollPane.setVvalue(0);
                    scrollPane.layout(); 
                    scrollPane.setVvalue(1.0); 
                });
                delay.play();
            });
        } catch (IOException e) {
            e.printStackTrace(); 
        }

    }

    private boolean connectToServer(String[] Inputs) {
        try {
            Socket socket = new Socket(Inputs[0], 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(Inputs[2]+"->"+Inputs[1]);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if(in.readLine().equals("19404")){
                socket.close();
                System.out.println("REJECTED");
                showAlert("Connection Failed", "The Connection to the server has been rejected");
                return false;
            }
            new Thread(() -> {
                try {
                    int i=0;
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        String finalMsg = msg;
                        if(finalMsg.startsWith("<-@U") && finalMsg.endsWith("#->")){
                            String val=finalMsg.replace("<-@U","");
                            final String User=val.replace("#->","");
                            addToTheList(User);
                            
                        }else{
                            String msgs[]=finalMsg.split(":->:");
                            if(msgs[0].equals(SelectedUser)){
                                Platform.runLater(() -> {
                                    Text senderText = new Text(msgs[0]);
                                    senderText.setFill(Color.DARKGREEN);
                                    senderText.setStyle("-fx-font-weight: bold;");
                                    senderText.setWrappingWidth(280);
                            
                                    Text msgText = new Text(msgs[1]);
                                    msgText.setFill(Color.BLACK);
                                    msgText.setWrappingWidth(280);
                            
                                    VBox messageBox = new VBox(10, senderText, msgText);
                                    messageBox.setStyle("-fx-background-color:"+Other+"; -fx-padding: 8; -fx-background-radius: 10;");
                                    messageBox.setMaxWidth(300);
                            
                                    HBox wrapper = new HBox(messageBox);
                                    wrapper.setAlignment(Pos.CENTER_RIGHT);
                                    wrapper.setPadding(new Insets(5));
                                    chatFlow.getChildren().add(wrapper);
                                });
                            }
                            setChatDetails(msgs[2],msgs[0],msgs[1],false);
                        }
                    }
                } catch (IOException ignored) {}
            }).start();
            getSavedFile();
            return true;
        } catch (IOException e) {
            showAlert("Connection Error", "Unable to connect to server.");
            return false;
        }
    }

    public void addToTheList(String User){
        
        if (!selector.getItems().contains(User)) {
            selector.getItems().add(User);
            if(SelectedUser == null){
                Platform.runLater(() -> selector.setValue(User));
                SelectedUser=User;
            }
        }
    }

    public void getSavedFile(){
        File Thedir=new File("Msg");
        // try{
            if(Thedir.isDirectory()){
                File[] Dir = Thedir.listFiles();
                if (Dir == null) return;
                for(File File: Dir){
                    String FileDetails=decrypt(File.getName().split("\\.")[0]);
                    String User=FileDetails.split("-")[1];
                    if(!User.equals(SystemUser)){
                        addToTheList(User);
                    }
                }
            }
        // }catch(IOException e){}
    }
    private void sendMessage(String UserName) {
        String msg = inputField.getText();
        if (!msg.isEmpty()) {
            out.println(UserName+"/<-@->/"+msg+"/<-@->/"+SelectedUser);
            Text senderText = new Text("Me");
            senderText.setFill(Color.DARKBLUE);
            senderText.setStyle("-fx-font-weight: bold;");
            senderText.setWrappingWidth(280); 
                                
            Text msgText = new Text(msg);
            msgText.setFill(Color.BLACK);
            msgText.setWrappingWidth(280);
                                
            VBox messageBox = new VBox(10, senderText, msgText);
            messageBox.setStyle("-fx-background-color:"+Me+"; -fx-padding: 8; -fx-background-radius: 10;");
            messageBox.setMaxWidth(300);

            HBox wrapper = new HBox(messageBox);
            wrapper.setAlignment( Pos.CENTER_RIGHT); 
            wrapper.setPadding(new Insets(5));

            chatFlow.getChildren().add(wrapper);

            inputField.clear();
            setChatDetails(UserName,SelectedUser,msg,true);
        }
        

    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static final String secretKey = "1234567890123456";


    public static String encrypt(String strToEncrypt) {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(strToEncrypt.getBytes());

            String base64Encoded = Base64.getEncoder().encodeToString(encryptedBytes);
            String safe = base64Encoded
                    .replace("+", "-")  
                    .replace("/", "_") 
                    .replace("=", ""); 

            return safe;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String safeEncrypted) {
        try {
            int paddingLength = (4 - safeEncrypted.length() % 4) % 4;
            String padded = safeEncrypted
                    .replace("-", "+")
                    .replace("_", "/") + "====".substring(0, paddingLength);

            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(padded));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
