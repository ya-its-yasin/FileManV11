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
import java.security.spec.KeySpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.stream.Stream;
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


public class UserUpload extends HttpServlet
{
	private static Connection conn = PGConnection.getConnection();
	private static File file ;
    private static String filePath = "D:\\main\\",fileName,userName; 
    private static ArrayList<String> ipAddressArr = new ArrayList<String>();
	private static ArrayList<Integer> portArr = new ArrayList<Integer>();
    private static int arrSize;
    private static ArrayList<String> list = new ArrayList<String>();
	
	public UserUpload()
	{
		updatePaths();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException 
	{ 
		int cnt=0;
		Boolean status = true;
		HttpSession session = request.getSession();
		userName = (String) session.getAttribute("userName");
		//System.out.println(userName+"loged in");
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
      	try 
      	{ 
			List fileItems = upload.parseRequest(request);
			Iterator i = fileItems.iterator();
			while ( i.hasNext () ) 
			{
				FileItem fi = (FileItem)i.next();
				if(fi.isFormField())
				{
					cnt = Integer.parseInt(fi.getString());
					//System.out.println(fi.getString());
				}
				if ( !fi.isFormField () ) 
				{
				   fileName = fi.getName();
				   fileName= userName + "_" +fileName;
				   //System.out.println(fileName);
				    try
					{
						String insertQuery = "insert into filelist(username,filename,pieces) values(?,?,?) ;";
						PreparedStatement ps = conn.prepareStatement(insertQuery);
						ps.setString(1,userName);
						ps.setString(2,fileName);
						ps.setInt(3,cnt);
						ps.executeUpdate();
						response.getWriter().println("file successfully uploaded");
					}
					catch(SQLException e1)
					{
						status = false;
						response.getWriter().println("file already exists");
						System.out.println(e1);
					}
					if(status)
					{
					    file = new File( filePath + fileName) ;
						fi.write(file);
						list.clear();
						SplitFile(cnt);		//splitting file into ramdom number of pieces
						
						//Encrypting all the pieces of files parallely 
						String key = "This is a secret";
						Stream<String> ps = list.parallelStream();
						ps.forEach(s->fileProcessor(Cipher.ENCRYPT_MODE,key,s, s+".enc"));	

					    //distributing the pices fo file to all the locations
					    updatePaths();
					    doDistribute();
					}
				}
			}
	         	
         } 
         catch(Exception ex) 
         {
            System.out.println(ex+"here");
         }
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			HttpSession session = request.getSession();
			String userName1 = (String) session.getAttribute("userName");
			String fileName1=request.getParameter("fileName");
			int pieces1= Integer.parseInt(request.getParameter("piecesIn"));
			fileName1 = userName1 + "_" + fileName1;
			
			//delete all the file pieces
			updatePaths();
			doDeleteIt(fileName1,pieces1);
			String deleteQuery = "delete from filelist where filename=?;";
	    	PreparedStatement ps=conn.prepareStatement(deleteQuery);
	    	ps.setString(1, fileName1);	    	
	    	ps.executeUpdate();
		}
		catch(SQLException e)
		{
			System.out.println(e);
		}
	}

	private void doDeleteIt(String filename, int pieces)
	{
		int i,j;
		for(i=0,j=0; j<pieces; j++,i++)
		{
			String newFileName = filename + j+ ".txt" ;
			if(i==arrSize)
				i=0;
			try{
				//Path temp = Files.move(Paths.get(filePath+str+".enc"),
				//Paths.get(filePathArray.get(i)+str+".enc"));
				deleteFile(ipAddressArr.get(i),portArr.get(i),filePath,newFileName+".enc");
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
	}

	private void deleteFile(String host, int port, String path, String file) throws IOException 
    {
        Socket s = null;
        try 
        {
            s = new Socket(host, port);
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF("delete");
            dos.writeUTF(file);
            dos.close();
        } 
        catch (Exception e) 
        {
           System.out.println(e);
        }    
    }

	private void doDistribute()
	{
		int i=0;
		for(String str : list)
		{
			if(i==arrSize)
				i=0;
			try{
				//Path temp = Files.move(Paths.get(filePath+str+".enc"),
				//Paths.get(filePathArray.get(i)+str+".enc"));
				sendFile(ipAddressArr.get(i),portArr.get(i),filePath,str+".enc");
				File inputFile = new File(filePath + str+".enc");
				inputFile.delete();
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
			i++;
		}
	}
	
	private void sendFile(String host, int port, String path, String file) throws IOException 
    {
        Socket s = null;
        try 
        {
            s = new Socket(host, port);
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF("upload");
            dos.writeUTF(file);
            FileInputStream fis = new FileInputStream(path+file);
            byte[] buffer = new byte[1];
            while (fis.read(buffer) > 0) 
            {
                dos.write(buffer);
            }
            fis.close();
            dos.close();
        } 
        catch (Exception e) 
        {
           System.out.println(e);
        }    
    }

	private void SplitFile(int cnt)
	{
		File inputFile = new File(filePath + fileName);
        FileInputStream inputStream;
        String newFileName;
        FileOutputStream filePart;
        int fileSize = (int) inputFile.length();
        int nChunks = 0, read = 0, readLength = (int) Math.ceil((double)fileSize/cnt);
        //System.out.println(fileSize+" "+cnt+" "+readLength+" "+Math.ceil(fileSize/cnt));
        byte[] byteChunkPart;
        int tint=0;        
        try 
        {
            inputStream = new FileInputStream(inputFile);
			for(int i=0;i<cnt;i++)
			{
			    newFileName = fileName + i+ ".txt" ;
			    list.add(newFileName);
			    filePart = new FileOutputStream(filePath + newFileName);
			    if (fileSize <= readLength) 
			    {
			        readLength = fileSize;
			    }
			    byteChunkPart = new byte[readLength];
			    read = inputStream.read(byteChunkPart, 0, readLength);
			    fileSize -= read;
			    nChunks++;
			    filePart.write(byteChunkPart);
			    filePart.flush();
			    filePart.close();
			    byteChunkPart = null;
			    filePart = null;
			    tint++;
			}
            inputStream.close();
            //System.out.println(list);
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }
       	inputFile.delete();
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
	       //System.out.println(secretKey);
	       FileInputStream inputStream = new FileInputStream(inputFile);
	       byte[] inputBytes = new byte[(int) inputFile.length()];
	       inputStream.read(inputBytes);
	       byte[] outputBytes = cipher.doFinal(inputBytes);
	       FileOutputStream outputStream = new FileOutputStream(outputFile);
	       outputStream.write(outputBytes);
	       inputStream.close();
	       outputStream.close();
	       //System.out.println("File Encrypted.");
			inputFile.delete();
	    } 
	    catch (NoSuchPaddingException | NoSuchAlgorithmException  | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException e) 
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
			System.out.println(ipAddressArr);
			System.out.println(portArr);
		}
		catch(SQLException e1)
		{
			System.out.println("SQL Exception while getting locations");
		}
	}
}