package org.example;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/materials")
public class MaterialServlet extends HttpServlet {

    // GET: Fetch all inventory materials
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        List<Material> materials = new ArrayList<>();
        String sql = "SELECT id, material_name, unit_cost FROM materials ORDER BY id ASC;";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                materials.add(new Material(
                        rs.getInt("id"),
                        rs.getString("material_name"),
                        rs.getBigDecimal("unit_cost")
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
        for (int i = 0; i < materials.size(); i++) {
            Material mat = materials.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(mat.getId()).append(",\n");
            json.append("    \"materialName\": \"").append(mat.getMaterialName()).append("\",\n");
            json.append("    \"unitCost\": ").append(mat.getUnitCost()).append("\n");
            json.append("  }");
            if (i < materials.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        out.println(json.toString());
    }

    // POST: Add a new raw material to inventory
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
            String materialName = "";
            if (payload.contains("materialName")) {
                materialName = payload.split("\"materialName\"\\s*:\\s*\"")[1].split("\"")[0];
            }

            String sanitizedCost = "0";
            if (payload.contains("unitCost")) {
                sanitizedCost = payload.split("\"unitCost\"\\s*[:]")[1].replaceAll("[^0-9.]", "");
            }
            if (sanitizedCost.isEmpty()) sanitizedCost = "0";
            BigDecimal unitCost = new BigDecimal(sanitizedCost);

            String sql = "INSERT INTO materials (material_name, unit_cost) VALUES (?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, materialName);
                stmt.setBigDecimal(2, unitCost);
                stmt.executeUpdate();

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().println("{\"status\": \"Material added successfully!\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"error\": \"Invalid request payload.\"}");
        }
    }
}