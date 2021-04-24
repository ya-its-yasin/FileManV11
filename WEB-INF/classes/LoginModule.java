import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.util.PGConnection;


public class LoginModule extends HttpServlet
{
	private static Connection conn = PGConnection.getConnection();

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
	{
		String userName = req.getParameter("username");
		String password = req.getParameter("password");

		//System.out.println(userName+" "+password);
		try
		{
			String checkQuery = "select * from users where username=? and password=? ;";
			PreparedStatement ps1 = conn.prepareStatement(checkQuery);
			ps1.setString(1,userName);
			ps1.setString(2,password);

			ResultSet rs1 = ps1.executeQuery();
			Boolean status =rs1.next();
			
			//System.out.println(rs1.getInt("si_no"));
			if(userName.equals("admin") && password.equals("admin"))
			{
				res.sendRedirect("admin_homepg.html");
			}
			else if(status)
			{
				HttpSession session = req.getSession();
            	session.setAttribute("userName",userName);
            	res.sendRedirect("user_homepg.html");
			}
			else
			{
				res.getWriter().println("<script>window.alert('Username or password Incorrect');location.replace('index.html')</script>");  
		        //res.sendRedirect("index.html");
			}
		}
		catch(SQLException e1)
		{
			e1.printStackTrace();
		}
	}
}

