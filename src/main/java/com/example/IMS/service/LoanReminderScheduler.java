package com.example.IMS.service;

import com.example.IMS.Utilities.Helper;
import com.example.IMS.model.Loan;
import com.example.IMS.repository.ILoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Scheduled job that runs every day at 8:00 AM IST.
 *
 * Covers:
 *   E-13 — Due date reminder (sends when 1, 2, or 3 days remain)
 *   E-14 — Overdue notice    (sends every day item is past due)
 */
@Component
public class LoanReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LoanReminderScheduler.class);

    /** Days-ahead window for reminders */
    private static final int REMINDER_DAYS = 3;

    @Autowired
    private ILoanRepository loanRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Runs daily at 08:00 AM (Asia/Kolkata = UTC+5:30).
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void sendLoanReminders() {
        logger.info("LoanReminderScheduler: starting daily reminder check");

        List<Loan> allLoans = loanRepository.findAll();
        Date today = new Date();
        int reminders = 0;
        int overdues = 0;

        for (Loan loan : allLoans) {
            // Skip already-returned loans
            if (loan.getReturnDate() != null && !loan.getReturnDate().isEmpty()) {
                continue;
            }
            // Skip loans without borrower email (safety)
            if (loan.getBorrower() == null || loan.getBorrower().getEmail() == null
                    || loan.getBorrower().getEmail().isBlank()) {
                continue;
            }
            // Skip loans without issue date
            if (loan.getIssueDate() == null || loan.getIssueDate().isBlank()) {
                continue;
            }

            try {
                String dueDateStr = Helper.getDueDate(loan.getIssueDate(), loan.getLoanDuration());
                Date dueDate = Helper.convertStringToDate(dueDateStr);

                long daysDiff = ChronoUnit.DAYS.between(today.toInstant(), dueDate.toInstant());
                // daysDiff > 0  → still before due date
                // daysDiff == 0 → due today
                // daysDiff < 0  → overdue

                String borrowerName = loan.getBorrower().getFirstName()
                        + " " + loan.getBorrower().getLastName();
                String borrowerEmail = loan.getBorrower().getEmail();
                String itemName = loan.getItem() != null ? loan.getItem().getName() : "Unknown item";

                if (daysDiff >= 0 && daysDiff <= REMINDER_DAYS) {
                    // E-13 — Due date reminder
                    emailService.sendDueDateReminderEmail(
                            borrowerEmail, borrowerName, itemName, dueDateStr, daysDiff);
                    reminders++;
                    logger.info("E-13 reminder sent to {} for item '{}', {} day(s) left",
                            borrowerEmail, itemName, daysDiff);

                } else if (daysDiff < 0) {
                    // E-14 — Overdue notice
                    long daysOverdue = Math.abs(daysDiff);
                    emailService.sendOverdueNoticeEmail(
                            borrowerEmail, borrowerName, itemName, dueDateStr, daysOverdue);
                    overdues++;
                    logger.info("E-14 overdue notice sent to {} for item '{}', {} day(s) overdue",
                            borrowerEmail, itemName, daysOverdue);
                }

            } catch (Exception e) {
                logger.error("LoanReminderScheduler: error processing loan id={} — {}",
                        loan.getId(), e.getMessage());
            }
        }

        logger.info("LoanReminderScheduler complete — {} reminder(s), {} overdue notice(s) sent",
                reminders, overdues);
    }
}
