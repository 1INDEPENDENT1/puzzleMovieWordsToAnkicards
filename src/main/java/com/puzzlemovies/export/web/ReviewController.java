package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.service.ReviewCardImportService;
import com.puzzlemovies.export.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import com.puzzlemovies.export.review.ReviewPresentation;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@Controller
public class ReviewController {
    private static final String REVIEWED_COUNT = "reviewedCount";
    private static final String IMPORT_MESSAGE = "reviewImportMessage";

    private final ReviewService reviewService;
    private final ReviewCardImportService importService;
    private final ExportJobRepository exportJobRepository;
    private final SessionUserResolver sessionUserResolver;

    public ReviewController(ReviewService reviewService,
                            ReviewCardImportService importService,
                            ExportJobRepository exportJobRepository,
                            SessionUserResolver sessionUserResolver) {
        this.reviewService = reviewService;
        this.importService = importService;
        this.exportJobRepository = exportJobRepository;
        this.sessionUserResolver = sessionUserResolver;
    }

    @ModelAttribute
    void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping("/reviews")
    public String reviews(HttpSession session, Model model) {
        User user = currentUser(session);
        ReviewDtos.ReviewQueueResponse queue = queue(user, session);
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

    @GetMapping("/cards")
    public String cards(@RequestParam(value = "page", defaultValue = "0") int page,
                        HttpSession session, Model model) {
        User user = currentUser(session);
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be nonnegative");
        }
        model.addAttribute("library", reviewService.cardLibrary(user, page));
        return "cards";
    }

    @PostMapping("/reviews/cards")
    public Object createCards(@RequestParam(value = "sourceExportId", required = false) UUID sourceExportId,
                              HttpSession session) {
        User user = currentUser(session);
        if (sourceExportId == null) {
            return "redirect:/reviews";
        }
        ExportJob job = exportJobRepository.findByIdAndUser(sourceExportId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ExportCompletionProjection projection = ExportCompletionProjection.from(job);
        if (projection.completion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not completed");
        }
        if (projection.partial() && !ExportWarningPresentation.wasPresented(session, job)) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .header(org.springframework.http.HttpHeaders.LOCATION, "/exports/" + job.getId()).build();
        }
        var result = importService.importSnapshot(user, sourceExportId);
        if (!result.alreadyPublished()) session.setAttribute(REVIEWED_COUNT, 0);
        session.setAttribute(IMPORT_MESSAGE, result.alreadyPublished()
                ? "This export has already been imported."
                : "Vocabulary ready: " + result.created() + " new cards, " + result.examples() + " source examples.");
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/next")
    @ResponseBody
    public ReviewDtos.ReviewQueueResponse next(HttpSession session) {
        return queue(currentUser(session), session);
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
        ReviewPresentation.invalidate(session, user.getId(), id);
        return new ReviewDtos.ReviewAnswerResponse(response.result(),
                reviewService.beginPresentation(user, response.nextCard(), session), response.counts());
    }

    @ExceptionHandler(ReviewService.InvalidAnswerException.class)
    ResponseEntity<String> invalidAnswer() {
        return ResponseEntity.badRequest().body("Invalid answer");
    }

    @PatchMapping("/cards/{id}")
    @ResponseBody
    public ReviewDtos.CardContentUpdateResponse updateContent(@PathVariable("id") UUID id,
            @RequestBody ReviewDtos.CardContentUpdateRequest request, HttpSession session) {
        return reviewService.updateContent(currentUser(session), id, request);
    }

    @GetMapping("/reviews/cards/{id}/examples")
    @ResponseBody
    public ReviewDtos.ExamplePage<ReviewDtos.ExampleItem> reviewExamples(
            @PathVariable("id") UUID id, @RequestParam("presentationId") UUID presentationId,
            @RequestParam(value = "page", defaultValue = "0") int page, HttpSession session) {
        return reviewService.reviewExamples(currentUser(session), id, presentationId, page, session);
    }

    @PostMapping("/reviews/cards/{id}/reveal")
    @ResponseBody
    public ReviewDtos.MainReveal reveal(@PathVariable("id") UUID id,
                                        @RequestBody ReviewDtos.PresentationRequest request, HttpSession session) {
        return reviewService.reveal(currentUser(session), id, request.presentationId(), session);
    }

    @PostMapping("/reviews/cards/{id}/examples/{exampleId}/translation")
    @ResponseBody
    public ReviewDtos.ExampleReveal exampleTranslation(@PathVariable("id") UUID id,
            @PathVariable("exampleId") UUID exampleId, @RequestBody ReviewDtos.ExampleRevealRequest request,
            HttpSession session) {
        return reviewService.revealExample(currentUser(session), id, exampleId, request, session);
    }

    @GetMapping("/cards/{id}/examples")
    @ResponseBody
    public ReviewDtos.ExamplePage<ReviewDtos.EditableExampleItem> libraryExamples(@PathVariable("id") UUID id,
            @RequestParam(value = "page", defaultValue = "0") int page, HttpSession session) {
        return reviewService.libraryExamples(currentUser(session), id, page);
    }

    @PatchMapping("/cards/{id}/examples/{exampleId}")
    @ResponseBody
    public ReviewDtos.ExampleUpdateResponse updateExample(@PathVariable("id") UUID id,
            @PathVariable("exampleId") UUID exampleId, @RequestBody ReviewDtos.ExampleUpdateRequest request,
            HttpSession session) {
        return reviewService.updateExample(currentUser(session), id, exampleId, request);
    }

    @ExceptionHandler(ReviewPresentation.Stale.class)
    ResponseEntity<String> stalePresentation() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("This review presentation has expired. Show the card again.");
    }

    @ExceptionHandler(ReviewService.MainAnswerNotRevealed.class)
    ResponseEntity<java.util.Map<String, String>> mainAnswerNotRevealed() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "MAIN_ANSWER_NOT_REVEALED"));
    }

    @ExceptionHandler(ReviewCardImportService.SnapshotUnavailable.class)
    ResponseEntity<String> snapshotUnavailable() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Import snapshot is unavailable. Run a fresh export.");
    }

    private ReviewDtos.ReviewQueueResponse queue(User user, HttpSession session) {
        var queue = reviewService.nextDueCard(user, reviewedCount(session));
        return new ReviewDtos.ReviewQueueResponse(reviewService.beginPresentation(user, queue.card(), session),
                queue.counts());
    }

    @ExceptionHandler(ReviewService.InvalidCardContentException.class)
    ResponseEntity<String> invalidContent(ReviewService.InvalidCardContentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<String> malformedRequest() {
        return ResponseEntity.badRequest().body("Invalid request. Check the submitted fields.");
    }

    @ExceptionHandler({ReviewService.StaleCardException.class,
            OptimisticLockingFailureException.class})
    ResponseEntity<String> staleCard() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This card has changed. Reload the page before editing again.");
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
