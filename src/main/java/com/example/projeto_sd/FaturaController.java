package com.example.projeto_sd;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FaturaController {

    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/fatura/{id}")
    public ResponseEntity<Map<String, Object>> getFaturaDetalhes(@PathVariable Long id, Authentication auth) {
        try {
            String clienteEmail = auth.getName();
            Optional<Fatura> faturaOpt = faturaRepository.findByIdWithItens(id);
            if (faturaOpt.isEmpty()) return ResponseEntity.notFound().build();
            Fatura fatura = faturaOpt.get();
            if (!fatura.getClienteEmail().equals(clienteEmail)) return ResponseEntity.status(403).build();

            Map<String, Object> response = new HashMap<>();
            response.put("id", fatura.getId());
            response.put("dataCompra", fatura.getDataCompra());
            response.put("totalPago", fatura.getTotalPago());

            List<Map<String, Object>> itensResponse = fatura.getItens().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("nomeVeiculo", item.getNomeVeiculo());
                    itemMap.put("precoUnitario", item.getPrecoUnitario());
                    itemMap.put("quantidade", item.getQuantidade());
                    itemMap.put("subtotal", item.getSubtotal());
                    return itemMap;
                })
                .collect(Collectors.toList());
            response.put("itens", itensResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erro interno do servidor");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/fatura/{id}/pdf")
    public ResponseEntity<byte[]> downloadFaturaPdf(@PathVariable Long id, Authentication auth) {
        try {
            String clienteEmail = auth.getName();
            Optional<Fatura> faturaOpt = faturaRepository.findByIdWithItens(id);
            if (faturaOpt.isEmpty()) return ResponseEntity.notFound().build();
            Fatura fatura = faturaOpt.get();
            if (!fatura.getClienteEmail().equals(clienteEmail)) return ResponseEntity.status(403).build();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Paragraph title = new Paragraph("AutoUBI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 14, new java.awt.Color(80, 80, 80));
            Paragraph sub = new Paragraph("Fatura de Compra", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);
            doc.add(new Paragraph(" "));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            doc.add(new Paragraph("Nº Fatura: #" + fatura.getId(), infoFont));
            doc.add(new Paragraph("Cliente: " + fatura.getClienteEmail(), infoFont));
            doc.add(new Paragraph("Data: " + fatura.getDataCompra().format(fmt), infoFont));
            if (fatura.getMetodoPagamento() != null) {
                doc.add(new Paragraph("Método de Pagamento: " + fatura.getMetodoPagamento(), infoFont));
            }
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{50, 10, 20, 20});

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            java.awt.Color headerBg = new java.awt.Color(200, 200, 200);
            for (String h : new String[]{"Veículo", "Qtd.", "Preço Unit.", "Subtotal"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (ItemFatura item : fatura.getItens()) {
                PdfPCell c1 = new PdfPCell(new Phrase(item.getNomeVeiculo(), infoFont));
                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantidade()), infoFont));
                PdfPCell c3 = new PdfPCell(new Phrase(String.format("€%.2f", item.getPrecoUnitario()), infoFont));
                PdfPCell c4 = new PdfPCell(new Phrase(String.format("€%.2f", item.getSubtotal()), infoFont));
                for (PdfPCell c : new PdfPCell[]{c1, c2, c3, c4}) c.setPadding(4);
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Paragraph total = new Paragraph(String.format("Total Pago: €%.2f", fatura.getTotalPago()), totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);

            doc.close();

            byte[] pdfBytes = baos.toByteArray();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fatura-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}