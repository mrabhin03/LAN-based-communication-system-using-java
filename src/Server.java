import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class ClientDetails{
    public Socket socket;
    public String UserName;
    ClientDetails(Socket socket,String UserName){
        this.socket=socket;
        this.UserName=UserName;
    }
}

public class Server {
    private static final List<ClientDetails> Clients=new ArrayList<>();
    private static int number=0;
    public static ServerSocket socket;

    public static ClientDetails CheckClient(Socket newclient,String Name){
        for(ClientDetails Client : Clients){
            if(Client.UserName.equals(Name)){
                Client.socket=newclient;
                return Client;
            }
        }
        ClientDetails obj=new ClientDetails(newclient,Name);
        Clients.add(obj);
        return obj;
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); 
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean CheckUserPassword(String User,String Pass){
        try{
            File dir = new File("ServerData");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f1=new File("ServerData/Details.txt");
            if(f1.createNewFile());
            BufferedReader read= new BufferedReader(new FileReader("ServerData/Details.txt"));
            String UserLine;
            while((UserLine=read.readLine())!=null){
                if(UserLine.startsWith(User)){
                    read.close();
                    if(hash(Pass).equals(UserLine.split("->")[1])){
                        return true;
                    }else{
                        return false;
                    }
                }
            }
            read.close();
            
            FileWriter file = new FileWriter("ServerData/Details.txt", true);
            file.write(User+"->"+hash(Pass)+"\n");
            file.close();
            return true;
        }catch(IOException e){

        }
        return true;
    }
    public static void main(String ar[]) throws IOException{
         socket=new ServerSocket(5000,50,InetAddress.getByName("0.0.0.0"));
        
        System.out.println("Server Ready and Running");
        while(true){
            Socket client=socket.accept();
            BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
            String Data=in.readLine();
            String[] Ddata=Data.split("->");
            if(Ddata.length<2){System.out.println("Connection Rejected");client.close();continue;}
            String Name=Ddata[0];
            PrintWriter out=new PrintWriter(client.getOutputStream(),true);
            if(!CheckUserPassword(Ddata[0],Ddata[1])){
                System.out.println(Name+" Login Rejected");
                out.println("19404");
                client.close();
            }else{
                number++;
                out.println("013");
                new ClientManage(CheckClient(client,Name),number).start();
            }
        }
        
        
    }
    static class ClientManage extends Thread{
        BufferedReader in;
        int No;
        ClientDetails Details;
        public ClientManage(ClientDetails Details,int No) throws IOException{
            SendUserDetails(Details.socket,Details.UserName);
            ThisUserDetails(Details);
            this.in=new BufferedReader(new InputStreamReader(Details.socket.getInputStream()));
            this.No=No;
            this.Details=Details;
            System.out.println(Details.UserName+" Connected");
            getUnsend(Details.UserName);
        }
        public void run() {
            try{
                String msg;
                while((msg=in.readLine())!=null){
                    if(msg.startsWith("@Disconnect")){
                        System.out.println(Details.UserName+" disconnected.");
                        break;
                    }
                    String val[]=msg.split("/<-@->/");
                    if(val[1].startsWith("@All")){
                        SendToAll(val[1],Details);
                    }else{
                    SendToUser(val[1],val[2],Details.UserName);
                    }
                }
            }catch (IOException e) {
                System.out.println(Details.UserName+" disconnected.");
            } finally {
                try { Details.socket.close(); } catch (IOException ignored) {}
                // Clients.remove(socket);
            }
        }
        public void SendUserDetails(Socket socket,String UserName){
            try{
                PrintWriter send =new PrintWriter(socket.getOutputStream(),true);
                for(ClientDetails Client: Clients){
                    if(socket!=Client.socket){
                        send.println("<-@U"+Client.UserName+"#->");
                    }
                }

                BufferedReader read= new BufferedReader(new FileReader("ServerData/Details.txt"));
                String UserLine;
                while((UserLine=read.readLine())!=null){
                    String Name=UserLine.split("->")[0];
                    if(!UserName.equals(Name)){
                        send.println("<-@U"+Name+"#->");
                    }
                }
                read.close();
            }catch(IOException e){
                
            }
        }
        public void ThisUserDetails(ClientDetails ThisDetails){
            try{
                for(ClientDetails Client: Clients){
                    if(ThisDetails.socket!=Client.socket){
                        PrintWriter send =new PrintWriter(Client.socket.getOutputStream(),true);
                        send.println("<-@U"+ThisDetails.UserName+"#->");
                    }
                }
            }catch(IOException e){
                
            }
        }
        public void SendToUser(String msg,String User,String UserName) throws IOException{
            for(ClientDetails Client : Clients){
                if((Client.UserName.equals(User)) && (!Client.UserName.equals(UserName)) ){
                    try{
                        PrintWriter out=new PrintWriter(Client.socket.getOutputStream(),true);
                        out.println(UserName+":->:"+msg+":->:"+Client.UserName);
                        return;
                    }catch(IOException e){
                        CacheUnsend(UserName+":->:"+msg+":->:"+Client.UserName);
                        return;
                    }
                }
            }
            // PrintWriter out=new PrintWriter(Sender.socket.getOutputStream(),true);
            // out.println(Sender.UserName+":->:"+msg+":->:"+User);
            CacheUnsend(UserName+":->:"+msg+":->:"+User);
            
        }

        public void SendToAll(String msg,ClientDetails Sender) throws IOException{
            for(ClientDetails Client : Clients){
                if( (!Client.UserName.equals(Sender.UserName)) ){
                    PrintWriter out=new PrintWriter(Client.socket.getOutputStream(),true);
                    out.println(Sender.UserName+":->:"+msg+":->:"+Client.UserName);
                }
                
            }
            
        }
        public  void CacheUnsend(String text){
            File dir = new File("ServerData");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try{
                FileWriter writer =new FileWriter("ServerData/Cache.txt", true);
                writer.write(encrypt(text)+"\n");
                writer.close();
            }catch(IOException e){
    
            }
        }
        public  void getUnsend(String User){
            File dir = new File("ServerData");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            List<String> inserts= new ArrayList<>();
            try{
               
                BufferedReader reader=new BufferedReader(new FileReader("ServerData/Cache.txt"));
                String Line;
                while((Line=reader.readLine())!=null){
                    String text=decrypt(Line);
                    if(text.endsWith(User)){
                        String msgs[]=text.split(":->:");
                        SendToUser(msgs[1],msgs[2],msgs[0]);
                    }else{
                        inserts.add(text);
                    }
                }
                reader.close();
            }
            catch(IOException e){
    
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("ServerData/Cache.txt", false))){
                for(String insert : inserts){
                    writer.write(encrypt(insert)+"\n");
                }
            }catch(IOException e){}
        } 

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
