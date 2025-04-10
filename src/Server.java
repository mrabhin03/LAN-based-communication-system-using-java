import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
            File dir = new File("Users");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f1=new File("Users/Details.txt");
            if(f1.createNewFile());
            BufferedReader read= new BufferedReader(new FileReader("Users/Details.txt"));
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
            
            FileWriter file = new FileWriter("Users/Details.txt", true);
            file.write(User+"->"+hash(Pass)+"\n");
            file.close();
            return true;
        }catch(IOException e){

        }
        return true;
    }
    public static void main(String ar[]) throws IOException{
        ServerSocket socket=new ServerSocket(5000);
        
        System.out.println("Server Ready and Running");
        while(true){
            Socket client=socket.accept();
            BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
            String Data=in.readLine();
            String[] Ddata=Data.split("->");
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
            SendUserDetails(Details.socket);
            ThisUserDetails(Details);
            this.in=new BufferedReader(new InputStreamReader(Details.socket.getInputStream()));
            this.No=No;
            this.Details=Details;
            System.out.println(Details.UserName+" Connected");
        }
        public void run() {
            try{
                String msg;
                while((msg=in.readLine())!=null){
                    String val[]=msg.split("/<-@->/");
                    System.out.println(Details.UserName+" : "+val[1]);
                    if(val[1].startsWith("@All")){
                        SendToAll(val[1],Details);
                    }else{
                    SendToUser(val[1],val[2],Details);
                    }
                }
            }catch (IOException e) {
                System.out.println(Details.UserName+" disconnected.");
            } finally {
                try { Details.socket.close(); } catch (IOException ignored) {}
                // Clients.remove(socket);
            }
        }
        public void SendUserDetails(Socket socket){
            try{
                PrintWriter send =new PrintWriter(socket.getOutputStream(),true);
                for(ClientDetails Client: Clients){
                    if(socket!=Client.socket){
                        send.println("<-@U"+Client.UserName+"#->");
                    }
                }
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
        public void SendToUser(String msg,String User,ClientDetails Sender) throws IOException{
            for(ClientDetails Client : Clients){
                if((Client.UserName.equals(User)) && (!Client.UserName.equals(Sender.UserName)) ){
                    PrintWriter out=new PrintWriter(Client.socket.getOutputStream(),true);
                    out.println(Sender.UserName+":->:"+msg+":->:"+Client.UserName);
                    return;
                }
                
            }
            PrintWriter out=new PrintWriter(Sender.socket.getOutputStream(),true);
            out.println("User Not Found");
            
        }

        public void SendToAll(String msg,ClientDetails Sender) throws IOException{
            for(ClientDetails Client : Clients){
                if( (!Client.UserName.equals(Sender.UserName)) ){
                    PrintWriter out=new PrintWriter(Client.socket.getOutputStream(),true);
                    out.println(Sender.UserName+":->:"+msg+":->:"+Client.UserName);
                }
                
            }
            
        }

    }
}
