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
import java.sql.Statement;

@WebServlet("/api/quotations")
public class QuotationServlet extends HttpServlet {

    // GET: Fetch all quotations with client names using a JOIN
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String sql = "SELECT q.id, c.client_name, q.project_title, q.labor_cost, q.total_amount, q.status, q.created_at " +
                "FROM quotations q JOIN clients c ON q.client_id = c.id " +
                "ORDER BY q.created_at DESC;";

        StringBuilder json = new StringBuilder("[\n");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",\n");
                json.append("  {\n");
                json.append("    \"id\": ").append(rs.getInt("id")).append(",\n");
                json.append("    \"clientName\": \"").append(rs.getString("client_name")).append("\",\n");
                json.append("    \"projectTitle\": \"").append(rs.getString("project_title")).append("\",\n");
                json.append("    \"laborCost\": ").append(rs.getBigDecimal("labor_cost")).append(",\n");
                json.append("    \"totalAmount\": ").append(rs.getBigDecimal("total_amount")).append(",\n");
                json.append("    \"status\": \"").append(rs.getString("status")).append("\",\n");
                json.append("    \"createdAt\": \"").append(rs.getTimestamp("created_at").toString()).append("\"\n");
                json.append("  }");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("[]");
            return;
        }
        json.append("\n]");
        out.println(json.toString());
    }

    // POST: Create a new quotation and compute totals
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
            // Simple parsing for header info
            int clientId = Integer.parseInt(payload.split("\"clientId\"\\s*:\\s*")[1].split("[,\"\r\n}]")[0].trim());
            String projectTitle = payload.split("\"projectTitle\"\\s*:\\s*\"")[1].split("\"")[0];

            String laborStr = "0";
            if (payload.contains("\"laborCost\"")) {
                laborStr = payload.split("\"laborCost\"\\s*:")[1].replaceAll("[^0-9.]", "");
            }
            if (laborStr.isEmpty()) laborStr = "0";
            BigDecimal laborCost = new BigDecimal(laborStr);

            try (Connection conn = DatabaseConnection.getConnection()) {
                // Turn off auto-commit to handle transaction safely
                conn.setAutoCommit(false);

                // 1. Insert Quotation header (Start with total_amount = laborCost, we will add materials next)
                String quoteSql = "INSERT INTO quotations (client_id, project_title, labor_cost, total_amount) VALUES (?, ?, ?, ?) RETURNING id;";
                int quotationId;

                try (PreparedStatement quoteStmt = conn.prepareStatement(quoteSql)) {
                    quoteStmt.setInt(1, clientId);
                    quoteStmt.setString(2, projectTitle);
                    quoteStmt.setBigDecimal(3, laborCost);
                    quoteStmt.setBigDecimal(4, laborCost); // initial total is just labor
                    ResultSet rs = quoteStmt.executeQuery();
                    if (rs.next()) {
                        quotationId = rs.getInt("id");
                    } else {
                        throw new Exception("Failed to generate quotation ID.");
                    }
                }

                // 2. Parse and insert materials if provided in the payload array
                BigDecimal calculatedTotal = laborCost;
                if (payload.contains("materials")) {
                    // Extract material items block (e.g., [{"materialId":1,"quantity":5}, ...])
                    String itemsSub = payload.split("\"materials\"\\s*:\\s*\\[")[1].split("\\]")[0];
                    String[] itemBlocks = itemsSub.split("\\},\\s*\\{");

                    for (String block : itemBlocks) {
                        if (!block.contains("materialId")) continue;

                        int matId = Integer.parseInt(block.split("\"materialId\"\\s*:\\s*")[1].replaceAll("[^0-9]", ""));
                        BigDecimal qty = new BigDecimal(block.split("\"quantity\"\\s*:\\s*")[1].replaceAll("[^0-9.]", ""));

                        // Fetch current unit cost of the material from the database
                        BigDecimal unitCost = BigDecimal.ZERO;
                        String matSql = "SELECT unit_cost FROM materials WHERE id = ?;";
                        try (PreparedStatement matStmt = conn.prepareStatement(matSql)) {
                            matStmt.setInt(1, matId);
                            ResultSet matRs = matStmt.executeQuery();
                            if (matRs.next()) {
                                unitCost = matRs.getBigDecimal("unit_cost");
                            }
                        }

                        // Compute subtotal for this line item
                        BigDecimal subtotal = unitCost.multiply(qty);
                        calculatedTotal = calculatedTotal.add(subtotal);

                        // Insert into quotation_items
                        String itemSql = "INSERT INTO quotation_items (quotation_id, material_id, quantity, subtotal) VALUES (?, ?, ?, ?);";
                        try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                            itemStmt.setInt(1, quotationId);
                            itemStmt.setInt(2, matId);
                            itemStmt.setBigDecimal(3, qty);
                            itemStmt.setBigDecimal(4, subtotal);
                            itemStmt.executeUpdate();
                        }
                    }
                }

                // 3. Update the quotation with the final calculated total amount
                String updateSql = "UPDATE quotations SET total_amount = ? WHERE id = ?;";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBigDecimal(1, calculatedTotal);
                    updateStmt.setInt(2, quotationId);
                    updateStmt.executeUpdate();
                }

                // Commit transaction
                conn.commit();

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().println("{\"status\": \"Quotation generated successfully!\", \"quotationId\": " + quotationId + ", \"totalAmount\": " + calculatedTotal + "}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"error\": \"Failed to generate quotation.\"}");
        }
    }
}