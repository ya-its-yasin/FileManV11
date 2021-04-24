
import java.io.*;

import java.net.*;


public class FileServer extends Thread {
    
    private ServerSocket ss;
    DataInputStream din ;
    private String filePath = "C:\\Users\\Yasin\\Desktop\\machine1\\storage\\";
    
    public FileServer(int port) {
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void run() {
        while (true) {
            try {
                Socket clientSock = ss.accept();
                din = new DataInputStream(clientSock.getInputStream());
                String action = din.readUTF();
                if(action.equals("upload"))
                {
                    saveFile(clientSock);
                }
                else if(action.equals("download"))
                {
                    sendFile(clientSock);
                }
                else if(action.equals("move"))
                {
                    moveFile(clientSock);
                }
                else if(action.equals("delete"))
                {
                    deleteFile(clientSock);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(Socket clientSock) throws IOException {

        String fname = din.readUTF();
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        FileOutputStream fos = new FileOutputStream(filePath+fname);
        byte[] buffer = new byte[1];
        
        int filesize = 15123; 
        int read = 0;
        int totalRead = 0;
        int remaining = filesize;
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            //System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }
        
        fos.close();
        dis.close();
        System.out.println("Uploaded "+fname);
    }

    public void sendFile(Socket clientSock) throws IOException 
    {
        //System.out.println("Download started"); 
        String fname = din.readUTF();
        DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
        FileInputStream fis = new FileInputStream(filePath+fname);
        byte[] buffer = new byte[1];
        
        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }
        
        fis.close();
        dos.close();   
        System.out.println("Downloaded "+fname); 
    }

    public void moveFile(Socket clientSock) throws IOException 
    {
        //System.out.println("Download started"); 
        String fname = din.readUTF();
        DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
        FileInputStream fis = new FileInputStream(filePath+fname);
        byte[] buffer = new byte[1];
        
        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }
        
        fis.close();
        dos.close();   
        System.out.println("Moved "+fname); 
        File inputFile = new File(filePath+fname);
        inputFile.delete();
    }

    public void deleteFile(Socket clientSock) throws IOException 
    {
        //System.out.println("Download started"); 
        String fname = din.readUTF();
        File inputFile = new File(filePath+fname);
        inputFile.delete();
    }
    
    public static void main(String[] args) 
    {
        FileServer fs = new FileServer(2000);
        fs.start();
    }

}