package com.project.retailproject.db;

import com.project.retailproject.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findBySaleId(Long saleId);
    List<Invoice> findByStatus(String status);
    List<Invoice> findByDateBetween(LocalDate startDate, LocalDate endDate);
    long countByStatus(String status);
}
