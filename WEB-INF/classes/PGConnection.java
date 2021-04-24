package com.util;
  
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
  
public class PGConnection {
  
    private static Connection con = null;
  
    static
    {
        String url = "jdbc:postgresql://localhost:5432/fileman";
        String user = "postgres";
        String pass = "password";
        try 
        {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(url, user, pass);
        }
        catch (ClassNotFoundException | SQLException e) 
        {
            e.printStackTrace();
        }
    }
    public static Connection getConnection()
    {
        return con;
    }
}