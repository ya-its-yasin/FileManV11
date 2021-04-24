import java.io.*;  
import javax.servlet.ServletException;  
import javax.servlet.http.*;  
import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.*;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.stream.Stream;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.*;

import com.util.PGConnection;


public class UserDownload extends HttpServlet
{
    private static Connection conn = PGConnection.getConnection();
    private static File file ;
    private static String filePath = "D:\\main\\",fileName,userName; 
    private static ArrayList<String> ipAddressArr = new ArrayList<String>();
    private static ArrayList<Integer> portArr = new ArrayList<Integer>();
    private static int arrSize,pieces;
    private static ArrayList<String> list = new ArrayList<String>();

    public UserDownload()
    {
    	updatePaths();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException
    {  
        HttpSession session = request.getSession();
        userName = (String) session.getAttribute("userName");
        pieces= Integer.parseInt(request.getParameter("piecesIn"));
        Boolean status=false;
        fileName =request.getParameter("fileName");
        fileName = userName +"_"+ fileName;
        //System.out.println("reached dlw");
        list.clear();
        for(int i=0;i<pieces;i++)
        {
            list.add(fileName+i+".txt");
        }

        //collect all the pieces of file from differet locations
        doCollect();

        //decrypt all the pieces of file parallelly 
        String key = "This is a secret";    
        Stream<String> ps = list.parallelStream();
        ps.forEach(s->fileProcessor(Cipher.DECRYPT_MODE,key,s+".enc",s));
        //System.out.println("completed decryption");

        //merging all the decrypted pieces
        MergeFiles(pieces);
        //System.out.println("completed Merging");

        fileName =fileName.substring(fileName.lastIndexOf("_") + 1);
        response.setContentType("text/html");  
        PrintWriter out = response.getWriter();  
        //String filename = "plainfile_decrypted.txt";   
        response.setContentType("APPLICATION/OCTET-STREAM");   
        response.setHeader("Content-Disposition","attachment; filename=\"" + fileName+ "\"");   
        FileInputStream fileInputStream = new FileInputStream(filePath + fileName);  
        //System.out.println("reached here");
        int i;   
        while ((i=fileInputStream.read()) != -1) 
        {  
            out.write(i);   
        }   
        //System.out.println("reached dlw 2");
        fileInputStream.close();   
        out.close();  
        File myObj = new File(filePath+fileName); 
        myObj.delete();
    }  

    private void doCollect()
    {
      	updatePaths();
      	int i=0;
      	for(String str : list)
      	{
        		if(i==arrSize)
        			i=0;
        		try
            {
        			//Path temp = Files.copy(Paths.get(filePathArray.get(i)+str+".enc"),
        			//	Paths.get(filePath+str+".enc"));
        			saveFile(ipAddressArr.get(i),portArr.get(i),filePath,str+".enc");
        		}
        		catch(Exception e)
        		{
        			System.out.println(e);
        		}
        		i++;
      	}
    }
	
    private void saveFile(String host, int port, String path, String file) throws IOException 
    {
        Socket s;
        s = new Socket(host, port);
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        dos.writeUTF("download");
        dos.writeUTF(file);
        DataInputStream dis = new DataInputStream(s.getInputStream());
        FileOutputStream fos = new FileOutputStream(path+file);
        byte[] buffer = new byte[1];
        while (dis.read(buffer) > 0) 
        {
          fos.write(buffer);
        }
        //System.out.println("read " + totalRead + " bytes.");
        fos.close();
        dis.close();
    }

    private void MergeFiles(int cnt)
    {
        String temp =fileName.substring(fileName.lastIndexOf("_") + 1);
        File ofile = new File(filePath + temp);
        FileOutputStream fos;
        FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        try 
        {
            fos = new FileOutputStream(ofile,true);
            for (int i=0;i<cnt;i++) 
            {
                File file = new File(filePath + fileName+i+".txt");
                fis = new FileInputStream(file);
                fileBytes = new byte[(int) file.length()];
                bytesRead = fis.read(fileBytes, 0,(int)  file.length());
                assert(bytesRead == fileBytes.length);
                assert(bytesRead == (int) file.length());
                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;
                fis.close();
                fis = null;
                file.delete();
            }
            fos.close();
            fos = null;
            //System.out.println("Successs");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    
    private void fileProcessor(int cipherMode,String key,String inputFile1,String outputFile1)
    {
        try 
        {
            File inputFile = new File (filePath+inputFile1);
            File outputFile = new File (filePath+outputFile1);
            Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKey);
            // System.out.println(secretKey);
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);
            byte[] outputBytes = cipher.doFinal(inputBytes);
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);
            inputStream.close();
            outputStream.close();
            inputFile.delete();
        } 
        catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException e) 
        {
            e.printStackTrace();
        }
    }

    private void updatePaths()
	  {
        try
        {
            ipAddressArr.clear();
            portArr.clear();
            String checkQuery = "select * from rem_loc ;";
            PreparedStatement ps1 = conn.prepareStatement(checkQuery);
            ResultSet rs1 = ps1.executeQuery();
            while(rs1.next())
            {
                String temp = rs1.getString("ip_address");
                int temp1 = rs1.getInt("port");
                ipAddressArr.add(temp);
                portArr.add(temp1);
            }
            arrSize=ipAddressArr.size();
            //System.out.println(ipAddressArr);
            //System.out.println(portArr);
        }
        catch(SQLException e1)
        {
        	System.out.println("SQL Exception while getting locations");
        }
	  }
    
}
