/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author niallpanton
 */
@WebServlet(name = "smoothie", urlPatterns = {"/smoothie"})
public class Smoothie extends HttpServlet {
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            try {
                long rate = getRecentRateFromDB();
                JSONObject graph = new JSONObject();
                graph.put("rate", rate);
                out.println(graph);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            
        } finally {
            out.close();
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    
    private long getRecentRateFromDB() {
        long out = 999l;
        Connection connect = null;
        try {
            // MySQL
            //Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");
            
            //set up string
            //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            connect = DriverManager.getConnection("jdbc:sqlite://Users/niallpanton/NetBeansProjects/TwitterClusterVisulisation/smoothie.db");
            
            PreparedStatement preparedStatement = connect.prepareStatement("SELECT * FROM  `smoothie` ORDER BY `dateTime` DESC LIMIT 1;");
            ResultSet requests = preparedStatement.executeQuery();
            while (requests.next()) {
                out = requests.getLong("count");
            }
            requests.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
//            logger.error("Cannot update delete requests\n" + e.getMessage());
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
//            logger.error("Cannot find jdbc class");
        } finally {
            try {
                connect.close();
            } catch (SQLException se) {
                se.printStackTrace();
//                logger.error("Cannot close jdbc connection\n"+se.getMessage());
            }
        }
            return out;
    }
}
