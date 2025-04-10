# LAN-Based Communication System using Java

This is a simple peer-to-peer communication system developed in Java that allows multiple users to chat with each other over a Local Area Network (LAN). It uses sockets for network communication and multithreading to handle multiple clients simultaneously.

## 🔧 Features

- Client-Server architecture
- Real-time messaging over LAN
- Multithreaded server to handle multiple clients
- User login with username/password
- Support Chat history
- Basic GUI using JavaFX 
- Message display with sender highlighting

## 🧠 Technologies Used

- Java (Core)
- Java Sockets
- Multithreading
- JavaFX (for GUI, optional)

## 🚀 How to Run

### Server
1. Compile: `javac Server.java`
2. Run: `java Server`

### Client
1. ## 🚀 How to Run

Make sure you have Java (JDK 21 or above) and JavaFX installed and configured in your environment.

### Server
1. Compile: `javac Server.java`
2. Run: `java Server`

### Client (JavaFX GUI)
1. Compile: `javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml Client.java`
2. Run: `java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml Client`
3. Enter the Server IP, Password (if used), and Username

## 📦 Future Improvements

- File sharing support
- User authentication
- Private messaging
- UI improvements

## 📄 License

This project is open-source and free to use for educational purposes.
