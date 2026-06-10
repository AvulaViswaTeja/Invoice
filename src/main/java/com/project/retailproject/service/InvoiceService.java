package com.project.retailproject.service;

import com.project.retailproject.clients.AuditLogClient;
import com.project.retailproject.db.InvoiceRepository;
import com.project.retailproject.dto.*;
import com.project.retailproject.exception.BadRequestException;
import com.project.retailproject.exception.ResourceNotFoundException;
import com.project.retailproject.model.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private AuditLogClient auditLogClient;

    private void log(String action) {
        try { auditLogClient.log(new AuditLogRequestDTO(action)); }
        catch (Exception e) { System.err.println("AuditLog failed: " + e.getMessage()); }
    }

    public InvoiceResponseDTO insertInvoice(InvoiceRequestDTO dto) {
        invoiceRepository.findBySaleId(dto.getSaleId()).ifPresent(i -> {
            log("Invoice.CREATE_FAILED | Duplicate invoice for SaleID: " + dto.getSaleId());
            throw new BadRequestException("Invoice already exists for sale ID: " + dto.getSaleId());
        });

        Invoice invoice = new Invoice();
        invoice.setSaleId(dto.getSaleId());
        invoice.setAmount(dto.getAmount());
        invoice.setDate(LocalDate.now());
        invoice.setCustomerId(dto.getCustomerId());
        System.out.println(dto.getCustomerId());
        invoice.setStatus("PENDING");
        Invoice saved = invoiceRepository.save(invoice);

        log("Invoice.CREATE_SUCCESS | InvoiceID: " + saved.getInvoiceId()
                + " | SaleID: " + dto.getSaleId()
                + " | Amount: " + dto.getAmount()
                + " | Status: PENDING");
        return mapToDTO(saved);
    }

    public InvoiceResponseDTO updateInvoice(Long id, InvoiceRequestDTO dto) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        if ("PAID".equals(invoice.getStatus())) {
            log("Invoice.UPDATE_FAILED | Attempt to update PAID InvoiceID: " + id);
            throw new BadRequestException("Cannot update a paid invoice");
        }
        String before = "Amount: " + invoice.getAmount() + " | Status: " + invoice.getStatus();
        invoice.setAmount(dto.getAmount());
        invoice.setStatus(dto.getStatus());
        Invoice saved = invoiceRepository.save(invoice);
        log("Invoice.UPDATE_SUCCESS | InvoiceID: " + id + " | Before: " + before
                + " | After: Amount: " + dto.getAmount() + " | Status: " + dto.getStatus());
        return mapToDTO(saved);
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        if ("PAID".equals(invoice.getStatus())) {
            log("Invoice.CANCEL_FAILED | Attempt to cancel PAID InvoiceID: " + id);
            throw new BadRequestException("Cannot cancel a paid invoice");
        }
        invoice.setStatus("CANCELLED");
        invoiceRepository.save(invoice);
        log("Invoice.CANCEL_SUCCESS | InvoiceID: " + id + " | SaleID: " + invoice.getSaleId()
                + " | Amount: " + invoice.getAmount() + " | Status: CANCELLED");
    }

    // Called by Sale Service via Feign
    public void deleteInvoiceBySaleId(Long saleId) {
        invoiceRepository.findBySaleId(saleId).ifPresent(invoice -> {
            if (!"PAID".equals(invoice.getStatus())) {
                invoice.setStatus("CANCELLED");
                invoiceRepository.save(invoice);
                log("Invoice.AUTO_CANCELLED | SaleID: " + saleId);
            }
        });
    }

    // Called by Sale Service via Feign
    public InvoiceResponseDTO getInvoiceBySaleId(Long saleId) {
        return mapToDTO(invoiceRepository.findBySaleId(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for SaleID: " + saleId)));
    }

    // Called by Payment Service via Feign
    public void updateInvoiceStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        invoice.setStatus(status);
        invoiceRepository.save(invoice);
        log("Invoice.STATUS_UPDATED | InvoiceID: " + id + " | Status: " + status);
    }

    public InvoiceResponseDTO findInvoiceById(Long id) {
        return mapToDTO(invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id)));
    }

    public List<InvoiceResponseDTO> findAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<InvoiceResponseDTO> getByStatus(String status) {
        return invoiceRepository.findByStatus(status).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<InvoiceResponseDTO> getByDateRange(LocalDate start, LocalDate end) {
        return invoiceRepository.findByDateBetween(start, end).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public Page<InvoiceResponseDTO> getAllInvoicesPaginated(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(this::mapToDTO);
    }

    private InvoiceResponseDTO mapToDTO(Invoice i) {
        InvoiceResponseDTO dto = new InvoiceResponseDTO();
        dto.setInvoiceId(i.getInvoiceId());
        dto.setSaleId(i.getSaleId());
        dto.setCustomerId(i.getCustomerId());
        dto.setAmount(i.getAmount());
        dto.setDate(i.getDate());
        dto.setStatus(i.getStatus());
        return dto;
    }
}
