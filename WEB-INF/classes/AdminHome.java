import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.util.PGConnection;


public class  AdminHome extends HttpServlet
{
	private static Connection conn = PGConnection.getConnection();

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
	{
		String userName = req.getParameter("username");
		String password = req.getParameter("password");
		System.out.println(userName+" "+password);
		try
		{
			String insertQuery = "insert into users(username,password) values(?,?) ;";
			PreparedStatement ps = conn.prepareStatement(insertQuery);
			ps.setString(1,userName);
			ps.setString(2,password);
			ps.executeUpdate();
			res.getWriter().println("User added successfully");
		}
		catch(SQLException e)
		{
			res.getWriter().println("User already exist");
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException
	{  
	    try
	    {
	    	PreparedStatement ps=conn.prepareStatement("select * from users;");
	    	ResultSet rs=ps.executeQuery();
	        JSONArray jsonArray = new JSONArray();
	    	while(rs.next())
	    	{
	    		if(rs.getString("username").equals("admin"))
	    			continue;
	    		JSONObject record = new JSONObject();
	    		record.put("si_no",rs.getInt("si_no"));
	            record.put("userName", rs.getString("username"));
	            record.put("password",rs.getString("password"));
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
  
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			String userName1=request.getParameter("userName");
			String deleteQuery = "delete from users where username=?;";
	    	PreparedStatement ps=conn.prepareStatement(deleteQuery);
	    	ps.setString(1, userName1);
	    	ps.executeUpdate();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
}

