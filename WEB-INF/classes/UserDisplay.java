import java.io.*;  
import javax.servlet.ServletException;  
import javax.servlet.http.*;  
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.util.PGConnection;

public class UserDisplay extends HttpServlet
{
    private static Connection conn = PGConnection.getConnection();
	private static String userName;

	public void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException 
    {  
      	HttpSession session = request.getSession();
        userName = (String) session.getAttribute("userName");
        try
        {
        	PreparedStatement ps=conn.prepareStatement("select * from filelist where username=?");
        	ps.setString(1, userName);
        	ResultSet rs=ps.executeQuery();
            JSONArray jsonArray = new JSONArray();
            
        	while(rs.next())
        	{
        		JSONObject record = new JSONObject();
        		record.put("si_no",rs.getInt("si_no"));
                record.put("userName",rs.getString("username"));
                record.put("fileName", rs.getString("filename").substring(rs.getString("filename").lastIndexOf("_") + 1));
                record.put("pieces", rs.getString("pieces"));	
        	
                jsonArray.add(record);
        	}
        	//System.out.println(jsonArray);
    	    response.getWriter().println(jsonArray);
        }
        catch(SQLException e)
        {
        	e.printStackTrace();
        }
  }
}