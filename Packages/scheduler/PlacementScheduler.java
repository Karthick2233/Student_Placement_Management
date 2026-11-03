package com.college.placement.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.college.placement.enums.OfferStatus;
import com.college.placement.repository.OfferRepository;
import com.college.placement.repository.StudentRepository;
import com.college.placement.service.JobDriveService;
import com.college.placement.service.OfferService;

@Component
public class PlacementScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlacementScheduler.class);

    private final JobDriveService driveService;
    private final OfferService offerService;
    private final StudentRepository students;
    private final OfferRepository offers;

    public PlacementScheduler(JobDriveService driveService, OfferService offerService, StudentRepository students,
            OfferRepository offers) {
        this.driveService = driveService;
        this.offerService = offerService;
        this.students = students;
        this.offers = offers;
    }

    @Scheduled(cron = "${placement.scheduler.drive-close-cron:0 0 0 * * *}")
    public void closeExpiredDrives() {
        log.info("Scheduler closed {} expired job drives", driveService.closeExpiredDrives());
    }

    @Scheduled(cron = "${placement.scheduler.offer-expiry-cron:0 5 0 * * *}")
    public void expireOffers() {
        log.info("Scheduler expired {} offers", offerService.expireOffers());
    }

    @Scheduled(initialDelayString = "${placement.scheduler.report-initial-delay-ms:60000}",
            fixedDelayString = "${placement.scheduler.report-fixed-delay-ms:3600000}")
    public void logPlacementReport() {
        long total = students.count();
        long accepted = offers.findByStatus(OfferStatus.ACCEPTED, PageRequest.of(0, 1)).getTotalElements();
        double percentage = total == 0 ? 0 : (accepted * 100.0) / total;
        log.info("Placement report: students={}, acceptedOffers={}, percentage={}%", total, accepted,
                String.format("%.1f", percentage));
    }
}
