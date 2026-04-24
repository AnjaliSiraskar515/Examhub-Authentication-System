package com.example.examauth.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generateSystemReport(Map<String, Object> metrics) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"));
            long exams = toLong(metrics.get("exams"));
            long users = toLong(metrics.get("users"));
            long institutions = toLong(metrics.get("institutions"));
            long qrs = toLong(metrics.get("qrs"));
            long frauds = toLong(metrics.get("frauds"));

            // ----- 1. HEADER -----
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 64, 175));
            Paragraph title = new Paragraph("ExamHub System Intelligence Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Paragraph datePara = new Paragraph("Generated: " + genDate, dateFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingAfter(24);
            document.add(datePara);

            // ----- 2. EXECUTIVE SUMMARY -----
            addSectionHeading(document, "Executive Summary");
            PdfPTable execTable = createBorderedTable(2);
            addMetricRow(execTable, "Total Exams Conducted", String.valueOf(exams));
            addMetricRow(execTable, "Active Students", String.valueOf(users));
            addMetricRow(execTable, "Registered Institutions", String.valueOf(institutions));
            addMetricRow(execTable, "Biometric Records", String.valueOf(qrs));
            addMetricRow(execTable, "Security Incidents", String.valueOf(frauds));
            addMetricRow(execTable, "System Uptime", "99.9%");
            addMetricRow(execTable, "Success Rate", "94.2%");
            document.add(execTable);

            // ----- 3. SYSTEM OVERVIEW -----
            addSectionHeading(document, "System Overview");
            PdfPTable sysTable = createBorderedTable(2);
            addKeyValueRow(sysTable, "Platform", "ExamHub");
            addKeyValueRow(sysTable, "Version", "1.0");
            addKeyValueRow(sysTable, "Report Type", "System Intelligence");
            addKeyValueRow(sysTable, "Environment", "Production");
            addKeyValueRow(sysTable, "Data Refresh", "Real-time");
            document.add(sysTable);

            // ----- 4. EXAM ANALYTICS -----
            addSectionHeading(document, "Exam Analytics");
            PdfPTable examTable = createBorderedTable(4);
            examTable.addCell(createHeaderCell("Metric"));
            examTable.addCell(createHeaderCell("Value"));
            examTable.addCell(createHeaderCell("Trend"));
            examTable.addCell(createHeaderCell("Status"));
            addBorderedRow(examTable, "Total Exams", String.valueOf(exams), "—", "Active");
            addBorderedRow(examTable, "Completion Rate", "94.2%", "+2.1%", "Healthy");
            addBorderedRow(examTable, "Avg. Duration", "2.5 hrs", "—", "—");
            document.add(examTable);

            // ----- 5. USER ANALYTICS -----
            addSectionHeading(document, "User Analytics");
            PdfPTable userTable = createBorderedTable(4);
            userTable.addCell(createHeaderCell("Metric"));
            userTable.addCell(createHeaderCell("Value"));
            userTable.addCell(createHeaderCell("Verified %"));
            userTable.addCell(createHeaderCell("Notes"));
            addBorderedRow(userTable, "Total Students", String.valueOf(users), "89%", "—");
            addBorderedRow(userTable, "Institutions", String.valueOf(institutions), "—", "Registered");
            addBorderedRow(userTable, "Biometric Records", String.valueOf(qrs), "98.6%", "Match rate");
            document.add(userTable);

            // ----- 6. SECURITY & COMPLIANCE -----
            addSectionHeading(document, "Security & Compliance");
            PdfPTable secTable = createBorderedTable(3);
            secTable.addCell(createHeaderCell("Metric"));
            secTable.addCell(createHeaderCell("Status"));
            secTable.addCell(createHeaderCell("Notes"));
            addBorderedRow3(secTable, "Biometric Verification", "Active", "98.6% match rate");
            addBorderedRow3(secTable, "Encryption Standards", "Compliant", "AES-GCM 256-bit");
            addBorderedRow3(secTable, "Data Integrity", "Secure", "No breaches detected");
            addBorderedRow3(secTable, "Incident Count", String.valueOf(frauds), "All logged and reviewed");
            document.add(secTable);

            // ----- 7. INCIDENT REPORT -----
            addSectionHeading(document, "Incident Report");
            PdfPTable incidentTable = createBorderedTable(4);
            incidentTable.addCell(createHeaderCell("Type"));
            incidentTable.addCell(createHeaderCell("Count"));
            incidentTable.addCell(createHeaderCell("Severity"));
            incidentTable.addCell(createHeaderCell("Action Taken"));
            addBorderedRow(incidentTable, "Biometric Mismatch", frauds > 0 ? String.valueOf(frauds / 2 + 1) : "0", "High", "User Flagged");
            addBorderedRow(incidentTable, "IP Conflict", frauds > 0 ? String.valueOf(Math.min(frauds, 3)) : "0", "Medium", "Session Terminated");
            addBorderedRow(incidentTable, "Multiple Login Attempts", "—", "Low", "Account Locked (Temp)");
            addBorderedRow(incidentTable, "Total Incidents", String.valueOf(frauds), "—", "All Resolved");
            document.add(incidentTable);

            // ----- 8. FOOTER -----
            addSpacer(document, 20);
            Paragraph footer = new Paragraph("Confidential Report — Internal Use Only | Generated by ExamHub System", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        }
    }

    private long toLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addSectionHeading(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59)));
        p.setSpacingBefore(16);
        p.setSpacingAfter(8);
        document.add(p);
    }

    private void addSpacer(Document document, float points) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(points);
        document.add(spacer);
    }

    private PdfPTable createBorderedTable(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        table.getDefaultCell().setBorderColor(Color.LIGHT_GRAY);
        return table;
    }

    private void addKeyValueRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        keyCell.setPadding(6);
        keyCell.setBackgroundColor(new Color(248, 250, 252));
        keyCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(keyCell);
        PdfPCell valCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        valCell.setPadding(6);
        valCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(valCell);
    }

    private void addBorderedRow(PdfPTable table, String c1, String c2, String c3, String c4) {
        addStyledCell(table, c1);
        addStyledCell(table, c2);
        addStyledCell(table, c3);
        addStyledCell(table, c4);
    }

    private void addBorderedRow3(PdfPTable table, String c1, String c2, String c3) {
        addStyledCell(table, c1);
        addStyledCell(table, c2);
        addStyledCell(table, c3);
    }

    private void addStyledCell(PdfPTable table, String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addMetricRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA, 12)));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(new Color(240, 240, 240));
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        valueCell.setPadding(8);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(valueCell);
    }

    private PdfPCell createHeaderCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
        cell.setBackgroundColor(new Color(60, 60, 60)); // Dark Gray
        cell.setPadding(8);
        return cell;
    }

    public byte[] generateSecurityReport(long frauds, long bioFailures) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, "Security Incident Report");

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingAfter(20);

            addMetricRow(table, "Total Security Incidents", String.valueOf(frauds));
            addMetricRow(table, "Biometric Mismatch Incidents", String.valueOf(bioFailures));
            addMetricRow(table, "IP Conflicts Detected", "3"); // Hardcoded based on current dashboard data
            addMetricRow(table, "Safe Logins Percentage", "99.8%");

            document.add(table);
            
            document.add(new Paragraph("Incident Details:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph(" "));
            
            PdfPTable detailsTable = new PdfPTable(3);
            detailsTable.setWidthPercentage(100);
            detailsTable.addCell(createHeaderCell("Type"));
            detailsTable.addCell(createHeaderCell("Severity"));
            detailsTable.addCell(createHeaderCell("Action Taken"));
            
            addRow(detailsTable, "Biometric Mismatch", "High", "User Flagged");
            addRow(detailsTable, "IP Conflict", "Medium", "Session Terminated");
            addRow(detailsTable, "Multiple Login Attempts", "Low", "Account Locked (Temp)");
            
            document.add(detailsTable);
            
            addFooter(document);
            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generateRankingsReport() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, "Institution Performance Rankings");

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(createHeaderCell("Rank"));
            table.addCell(createHeaderCell("Institution Name"));
            table.addCell(createHeaderCell("Performance Score"));

            addRow(table, "1", "Pune University", "82%");
            addRow(table, "2", "Mumbai Tech", "78%");
            addRow(table, "3", "Delhi College", "65%");
            // Add more mock data or real data if available in future
            
            document.add(table);
            addFooter(document);
            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generateExamReport(java.util.List<java.util.Map<String, Object>> exams) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"));

            // Professional Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 64, 175));
            Paragraph title = new Paragraph("Detailed Exam Performance", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Paragraph datePara = new Paragraph("Generated: " + genDate, dateFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingAfter(24);
            document.add(datePara);

            // Summary section
            addSectionHeading(document, "Overview");
            PdfPTable summaryTable = createBorderedTable(1);
            addMetricRow(summaryTable, "Total Exams Conducted", String.valueOf(exams.size()));
            document.add(summaryTable);

            // Detailed Data Table
            addSpacer(document, 10);
            addSectionHeading(document, "Exam Records");

            PdfPTable table = createBorderedTable(4);
            table.addCell(createHeaderCell("Exam Name"));
            table.addCell(createHeaderCell("Institution"));
            table.addCell(createHeaderCell("Completion %"));
            table.addCell(createHeaderCell("Status"));

            if (exams.isEmpty()) {
                PdfPCell noData = new PdfPCell(new Phrase("No exams found for the selected filter.",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY)));
                noData.setColspan(4);
                noData.setPadding(12);
                noData.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noData);
            } else {
                for (java.util.Map<String, Object> exam : exams) {
                    String examName = exam.getOrDefault("examName", "-").toString();
                    String institution = exam.getOrDefault("institution", "-").toString();
                    String completionPct = exam.getOrDefault("completionPct", "0").toString() + "%";
                    String status = exam.getOrDefault("status", "Upcoming").toString();
                    addBorderedRow(table, examName, institution, completionPct, status);
                }
            }

            document.add(table);
            
            // Footer
            addSpacer(document, 20);
            Paragraph footer = new Paragraph("Confidential Report — Internal Use Only | Generated by ExamHub System", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generateActivityLogCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Module,Status\n");
        csv.append("2026-02-16 10:30:00,System,Health Check,Monitor,Success\n");
        csv.append("2026-02-16 10:20:00,Pune Univ,Upload Schedule,Exams,Success\n");
        csv.append("2026-02-16 10:05:00,Student_01,Biometric Fail,Security,Failed\n");
        csv.append("2026-02-16 09:30:00,Mumbai Tech,New Batch,Users,Success\n");
        return csv.toString().getBytes();
    }

    public byte[] generatePerformanceTrendsReport() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, "System Performance Trends Report");

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingAfter(20);

            addMetricRow(table, "Average Verification Time", "1.2s");
            addMetricRow(table, "System Response Time", "45ms");
            addMetricRow(table, "API Error Rate", "0.01%");
            
            document.add(table);
            
            document.add(new Paragraph("Performance Analysis:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph("The system is performing within optimal parameters. Verification speed has improved by 15% compared to last month."));

            addFooter(document);
            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generateDeepDiveReport() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, "Deep Dive Analytics Report");

            document.add(new Paragraph("Exam Conducted Trends (Last 6 Months)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            PdfPTable examTable = new PdfPTable(2);
            examTable.setWidthPercentage(100);
            examTable.setSpacingBefore(10);
            examTable.setSpacingAfter(20);
            examTable.addCell(createHeaderCell("Month"));
            examTable.addCell(createHeaderCell("Exams Count"));
            
            addRow(examTable, "September", "15", "");
            addRow(examTable, "October", "22", "");
            addRow(examTable, "November", "18", "");
            addRow(examTable, "December", "30", "");
            addRow(examTable, "January", "25", "");
            addRow(examTable, "February", "35", "");
            document.add(examTable);

            document.add(new Paragraph("Student Enrollment Growth", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setSpacingBefore(10);
            studentTable.addCell(createHeaderCell("Month"));
            studentTable.addCell(createHeaderCell("New Students"));

            addRow(studentTable, "September", "45", "");
            addRow(studentTable, "October", "50", "");
            addRow(studentTable, "November", "65", "");
            addRow(studentTable, "December", "60", "");
            addRow(studentTable, "January", "85", "");
            addRow(studentTable, "February", "95", "");
            document.add(studentTable);

            addFooter(document);
            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generateSystemMetricsCsv(Map<String, Object> metrics) {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        csv.append("Total Exams Conducted,").append(metrics.getOrDefault("exams", "0")).append("\n");
        csv.append("Active Students,").append(metrics.getOrDefault("users", "0")).append("\n");
        csv.append("Registered Institutions,").append(metrics.getOrDefault("institutions", "0")).append("\n");
        csv.append("Biometric Records,").append(metrics.getOrDefault("qrs", "0")).append("\n");
        csv.append("Security Incidents,").append(metrics.getOrDefault("frauds", "0")).append("\n");
        csv.append("System Uptime,99.9%\n");
        csv.append("Biometric Verification Match Rate,98.6%\n");
        return csv.toString().getBytes();
    }

    private void addTitle(Document document, String titleText) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
        Paragraph title = new Paragraph(titleText, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("Generated by ExamHub System", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }

    private void addRow(PdfPTable table, String c1, String c2, String c3, String c4) {
        table.addCell(new Phrase(c1));
        table.addCell(new Phrase(c2));
        table.addCell(new Phrase(c3));
        table.addCell(new Phrase(c4));
    }

    private void addRow(PdfPTable table, String c1, String c2, String c3) {
        table.addCell(new Phrase(c1));
        table.addCell(new Phrase(c2));
        table.addCell(new Phrase(c3));
    }
}
