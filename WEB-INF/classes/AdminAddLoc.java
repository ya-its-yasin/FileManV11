import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.*;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.*;

import com.util.PGConnection;


public class  AdminAddLoc extends HttpServlet
{
	private static Connection conn = PGConnection.getConnection();
	private static ArrayList<String> ipAddressArr = new ArrayList<String>();
	private static ArrayList<Integer> portArr = new ArrayList<Integer>();
    private static int arrSize;
    private static  String filePath = "D:\\main\\"; 
    private static ArrayList<String> list = new ArrayList<String>();

	public AdminAddLoc()
	{
	    updatePaths();
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
	{
		String ipAddress = req.getParameter("ipAddress");
		int port = Integer.parseInt(req.getParameter("port"));
		collectAll();
		try
		{
			String insertQuery = "insert into rem_loc(ip_address,port) values(?,?) ;";
			PreparedStatement ps = conn.prepareStatement(insertQuery);
			ps.setString(1,ipAddress);
			ps.setInt(2,port);
			ps.executeUpdate();
			res.getWriter().println("Location added successfully");
		}
		catch(SQLException e1)
		{
			res.getWriter().println("Location already exist");
		}
		updatePaths();
		distributeAll();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException 
	{  
	    try
	    {
	    	PreparedStatement ps=conn.prepareStatement("select * from rem_loc;");
	    	ResultSet rs=ps.executeQuery();
	        JSONArray jsonArray = new JSONArray();
	    	while(rs.next())
	    	{
	    		JSONObject record = new JSONObject();
	    		record.put("si_no",rs.getInt("si_no"));
	            record.put("ipAddress", rs.getString("ip_address"));
	            record.put("port", rs.getInt("port"));
	    		jsonArray.add(record);
	    	}
	    	//System.out.println(jsonArray);
		    response.getWriter().println(jsonArray);
	    }
	    catch(SQLException e)
	    {
	    	System.out.println("SQL Connection Error Occured while displaying");
	    }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
  		collectAll();
		try
		{
			int si_no= Integer.parseInt(request.getParameter("si_no"));
			String deleteQuery = "delete from rem_loc where si_no=?;";
	    	PreparedStatement ps=conn.prepareStatement(deleteQuery);
	    	ps.setInt(1, si_no);
	    	ps.executeUpdate();
		}
		catch(SQLException e)
		{
			System.out.println("SQL Exception Occured while deleting");
		}
		updatePaths();
		distributeAll();
	}

	private void collectAll()
	{
		try
	    {
	    	PreparedStatement ps=conn.prepareStatement("select * from filelist;");
	    	ResultSet rs=ps.executeQuery();        
	    	while(rs.next())
	    	{
	    		int sino=rs.getInt("si_no");
	    		String fileName = rs.getString("filename");
	    		int pieces = rs.getInt("pieces");
	    		list.clear();
			  	for(int i=0;i<pieces;i++)
			    {
			        list.add(fileName+i+".txt");
			    }
	    		collectOne();
	    	}
	    }
	    catch(SQLException e)
	    {
	    	System.out.println("SQL Connection Error Occured while displaying");
	    }
	}

	private void collectOne()
	{
		int i=0;
		for(String str : list)
		{
			if(i==arrSize)
				i=0;
			try{
				//Path temp = Files.move(Paths.get(filePathArray.get(i)+str+".enc"),
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
        dos.writeUTF("move");
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

	private void distributeAll()
	{
		try
	    {
	    	PreparedStatement ps=conn.prepareStatement("select * from filelist;");
	    	ResultSet rs=ps.executeQuery();        
	    	while(rs.next())
	    	{
	    		int sino=rs.getInt("si_no");
	    		String fileName = rs.getString("filename");
	    		int pieces = rs.getInt("pieces");
	    		list.clear();
			  	for(int i=0;i<pieces;i++)
			    {
			        list.add(fileName+i+".txt");
			    }
	    		distributeOne();
	    	}
	    }
	    catch(SQLException e)
	    {
	    	System.out.println("SQL Connection Error Occured while displaying");
	    }
	}

	private void distributeOne()
	{
		int i=0;
		for(String str : list)
		{
			if(i==arrSize)
				i=0;
			try{
			//	Path temp = Files.move(Paths.get(filePath+str+".enc"),
			//	Paths.get(filePathArray.get(i)+str+".enc"));
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

	public void sendFile(String host, int port, String path, String file) throws IOException 
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

