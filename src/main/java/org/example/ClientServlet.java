package org.example;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/clients")
public class ClientServlet extends HttpServlet {

    // GET: Fetch all clients
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        List<Client> clients = new ArrayList<>();
        String sql = "SELECT id, client_name, contact_number, address FROM clients ORDER BY id ASC;";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("id"),
                        rs.getString("client_name"),
                        rs.getString("contact_number"),
                        rs.getString("address")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("[]");
            return;
        }

        // Build JSON manually
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < clients.size(); i++) {
            Client c = clients.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(c.getId()).append(",\n");
            json.append("    \"clientName\": \"").append(c.getClientName()).append("\",\n");
            json.append("    \"contactNumber\": \"").append(c.getContactNumber() == null ? "" : c.getContactNumber()).append("\",\n");
            json.append("    \"address\": \"").append(c.getAddress() == null ? "" : c.getAddress()).append("\"\n");
            json.append("  }");
            if (i < clients.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        out.println(json.toString());
    }

    // POST: Add a new client
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String payload = buffer.toString();

        try {
            // Bulletproof extraction using regex/indexOf
            String clientName = extractJsonValue(payload, "clientName");
            String contactNumber = extractJsonValue(payload, "contactNumber");
            String address = extractJsonValue(payload, "address");

            String sql = "INSERT INTO clients (client_name, contact_number, address) VALUES (?, ?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, clientName);
                stmt.setString(2, contactNumber);
                stmt.setString(3, address);
                stmt.executeUpdate();

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().println("{\"status\": \"Client added successfully!\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"error\": \"Invalid request payload.\"}");
        }
    }

    // Helper method for safe JSON parsing without strict order requirements
    private String extractJsonValue(String json, String key) {
        try {
            if (!json.contains(key)) return "";
            String sub = json.split("\"" + key + "\"\\s*:\\s*\"")[1];
            return sub.split("\"")[0];
        } catch (Exception e) {
            return "";
        }
    }
}