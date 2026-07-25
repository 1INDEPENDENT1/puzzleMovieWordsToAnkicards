package com.puzzlemovies.export.web;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.review.ReviewCardDraft;
import com.puzzlemovies.export.review.ReviewCardDraftFactory;
import com.puzzlemovies.export.service.ExportService;
import com.puzzlemovies.export.service.ReviewCardImportService;
import com.puzzlemovies.export.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Controller
public class ReviewController {
    private static final String REVIEWED_COUNT = "reviewedCount";
    private static final String IMPORT_MESSAGE = "reviewImportMessage";

    private final ReviewService reviewService;
    private final ReviewCardDraftFactory draftFactory;
    private final ReviewCardImportService importService;
    private final ExportService exportService;
    private final ExportJobRepository exportJobRepository;
    private final SessionUserResolver sessionUserResolver;

    public ReviewController(ReviewService reviewService,
                            ReviewCardDraftFactory draftFactory,
                            ReviewCardImportService importService,
                            ExportService exportService,
                            ExportJobRepository exportJobRepository,
                            SessionUserResolver sessionUserResolver) {
        this.reviewService = reviewService;
        this.draftFactory = draftFactory;
        this.importService = importService;
        this.exportService = exportService;
        this.exportJobRepository = exportJobRepository;
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping("/reviews")
    public String reviews(HttpSession session, Model model) {
        User user = currentUser(session);
        ReviewDtos.ReviewQueueResponse queue = reviewService.nextDueCard(user, reviewedCount(session));
        model.addAttribute("queue", queue);
        model.addAttribute("card", queue.card());
        model.addAttribute("counts", queue.counts());
        Object importMessage = session.getAttribute(IMPORT_MESSAGE);
        if (importMessage instanceof String message) {
            model.addAttribute("importMessage", message);
            session.removeAttribute(IMPORT_MESSAGE);
        }
        return "reviews";
    }

    @PostMapping("/reviews/cards")
    public String createCards(@RequestParam(value = "sourceExportId", required = false) UUID sourceExportId,
                              HttpSession session) {
        User user = currentUser(session);
        if (sourceExportId == null) {
            return "redirect:/reviews";
        }
        ExportJob job = exportJobRepository.findByIdAndUser(sourceExportId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (job.getStatus() != ExportStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not completed");
        }
        List<ExportRecord> records = exportService.generatedRecordsForCompletedExport(user, sourceExportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Generated review records are no longer available; run a fresh export"));
        List<ReviewCardDraft> drafts = draftFactory.createDrafts(records);
        ReviewCardImportService.ImportResult result = importService.importDrafts(user, drafts);
        session.setAttribute(REVIEWED_COUNT, 0);
        session.setAttribute(IMPORT_MESSAGE,
                "Review cards ready: " + result.created() + " created, " + result.updated() + " refreshed.");
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/next")
    @ResponseBody
    public ReviewDtos.ReviewQueueResponse next(HttpSession session) {
        return reviewService.nextDueCard(currentUser(session), reviewedCount(session));
    }

    @PostMapping("/reviews/cards/{id}/answer")
    @ResponseBody
    public ReviewDtos.ReviewAnswerResponse answer(@PathVariable("id") UUID id,
                                                  @RequestBody ReviewDtos.ReviewAnswerRequest request,
                                                  HttpSession session) {
        User user = currentUser(session);
        ReviewDtos.ReviewAnswerResponse response = reviewService.answerCard(
                user,
                id,
                request == null ? null : request.answer(),
                request == null ? null : request.responseMillis(),
                reviewedCount(session));
        session.setAttribute(REVIEWED_COUNT, response.counts().reviewedCount());
        return response;
    }

    @ExceptionHandler(ReviewService.InvalidAnswerException.class)
    ResponseEntity<String> invalidAnswer() {
        return ResponseEntity.badRequest().body("Invalid answer");
    }

    @ExceptionHandler(ReviewService.CardNotFoundException.class)
    ResponseEntity<String> cardNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Review card not found");
    }

    @ExceptionHandler(ReviewService.CardNotAnswerableException.class)
    ResponseEntity<String> cardNotAnswerable() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Review card is not currently answerable");
    }

    private User currentUser(HttpSession session) {
        return sessionUserResolver.resolve(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private int reviewedCount(HttpSession session) {
        Object value = session.getAttribute(REVIEWED_COUNT);
        return value instanceof Integer count ? count : 0;
    }
}
