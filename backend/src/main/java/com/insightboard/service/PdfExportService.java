package com.insightboard.service;

import com.insightboard.security.AuthUser;
import com.insightboard.service.DashboardResponse.CategorySummary;
import com.insightboard.service.DashboardResponse.NumericSummary;
import com.insightboard.service.DashboardResponse.ValueCount;
import com.insightboard.web.ApiException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Color HEADER_BG = new Color(0x4F, 0x46, 0xE5);
    private static final Color ZEBRA_BG = new Color(0xF3, 0xF4, 0xF6);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(0x11, 0x18, 0x27));
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0x6B, 0x72, 0x80));
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(0x11, 0x18, 0x27));
    private static final Font TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TD = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0x11, 0x18, 0x27));

    private final DashboardService dashboardService;
    private final AuditService audit;

    public record PdfFile(String name, byte[] content) {
    }

    public PdfFile export(AuthUser user, UUID datasetId) {
        DashboardResponse d = dashboardService.build(user.tenantId(), datasetId);
        byte[] bytes = render(d);
        audit.log(user, "DATASET_EXPORTED_PDF", "Dataset", datasetId.toString(), d.name());
        return new PdfFile(d.name(), bytes);
    }

    byte[] render(DashboardResponse d) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.addTitle(d.name());
            doc.addCreator("InsightBoard");
            doc.open();

            doc.add(new Paragraph(d.name(), TITLE));
            doc.add(new Paragraph("Relatório gerado em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "  •  " + d.rowCount() + " linhas  •  " + d.columnCount() + " colunas", SUBTITLE));

            if (!d.numeric().isEmpty()) {
                doc.add(section("Métricas numéricas"));
                doc.add(numericTable(d.numeric()));
            }
            for (CategorySummary c : d.categorical()) {
                if (c.top().isEmpty()) {
                    continue;
                }
                doc.add(section(c.column() + "  (" + c.distinct() + " valores distintos)"));
                doc.add(categoryTable(c));
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao gerar o PDF");
        }
    }

    private Paragraph section(String text) {
        Paragraph p = new Paragraph(text, H2);
        p.setSpacingBefore(18);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPTable numericTable(List<NumericSummary> rows) {
        PdfPTable table = new PdfPTable(new float[]{3, 1.2f, 2, 1.5f, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        for (String h : List.of("Coluna", "Qtd", "Soma", "Mín", "Máx", "Média")) {
            table.addCell(header(h));
        }
        int i = 0;
        for (NumericSummary n : rows) {
            Color bg = i++ % 2 == 1 ? ZEBRA_BG : null;
            table.addCell(cell(n.column(), Element.ALIGN_LEFT, bg));
            table.addCell(cell(String.valueOf(n.count()), Element.ALIGN_RIGHT, bg));
            table.addCell(cell(num(n.sum()), Element.ALIGN_RIGHT, bg));
            table.addCell(cell(num(n.min()), Element.ALIGN_RIGHT, bg));
            table.addCell(cell(num(n.max()), Element.ALIGN_RIGHT, bg));
            table.addCell(cell(num(n.avg()), Element.ALIGN_RIGHT, bg));
        }
        return table;
    }

    private PdfPTable categoryTable(CategorySummary c) {
        PdfPTable table = new PdfPTable(new float[]{5, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        for (String h : List.of("Valor", "Qtd", "%")) {
            table.addCell(header(h));
        }
        int i = 0;
        for (ValueCount v : c.top()) {
            Color bg = i++ % 2 == 1 ? ZEBRA_BG : null;
            double pct = c.filled() == 0 ? 0 : 100.0 * v.count() / c.filled();
            table.addCell(cell(v.value(), Element.ALIGN_LEFT, bg));
            table.addCell(cell(String.valueOf(v.count()), Element.ALIGN_RIGHT, bg));
            table.addCell(cell(String.format(PT_BR, "%.1f%%", pct), Element.ALIGN_RIGHT, bg));
        }
        return table;
    }

    private PdfPCell header(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TH));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        return cell;
    }

    private PdfPCell cell(String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TD));
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        cell.setBorderWidth(0);
        if (bg != null) {
            cell.setBackgroundColor(bg);
        }
        return cell;
    }

    private static String num(double value) {
        return String.format(PT_BR, "%,.2f", value);
    }
}
