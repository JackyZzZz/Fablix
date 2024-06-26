import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jasypt.util.password.StrongPasswordEncryptor;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() {
        try {
            InitialContext initialContext = new InitialContext();
            dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("username");
        String password = request.getParameter("password");
        //String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        JsonObject responseJsonObject = new JsonObject();

        /*
        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "reCAPTCHA verification failed" );
            response.setContentType("application/json");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }
         */

        try (Connection conn = dataSource.getConnection()) {
            String userQuery = "SELECT id, firstName, lastName, password FROM customers WHERE email = ?";
            PreparedStatement userStatement = conn.prepareStatement(userQuery);
            userStatement.setString(1, email);
            ResultSet userRs = userStatement.executeQuery();

            if (userRs.next()) {
                String correctPassword = userRs.getString("password");

                if (new StrongPasswordEncryptor().checkPassword(password, correctPassword)) {
                    // Password is correct
                    int userId = userRs.getInt("id");
                    String userFullName = userRs.getString("firstName") + " " + userRs.getString("lastName");
                    request.getSession().setAttribute("userId", userId); // Store user ID in session
                    request.getSession().setAttribute("user", userFullName); // Store user's name in session
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "Login successful.");
                } else {
                    // Password is incorrect
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Incorrect password.");
                }
            } else {
                // User does not exist
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Username does not exist.");
            }
            userRs.close();
            userStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "An error occurred: " + e.getMessage());
        }

        response.setContentType("application/json");
        response.getWriter().write(responseJsonObject.toString());
    }
}
