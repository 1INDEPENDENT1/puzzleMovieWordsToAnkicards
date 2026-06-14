package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportPhase;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.service.ExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Controller
public class ExportController {
    private final ExportService exportService;
    private final ExportJobRepository exportJobRepository;
    private final SessionUserResolver sessionUserResolver;

    public ExportController(ExportService exportService,
                            ExportJobRepository exportJobRepository,
                            SessionUserResolver sessionUserResolver) {
        this.exportService = exportService;
        this.exportJobRepository = exportJobRepository;
        this.sessionUserResolver = sessionUserResolver;
    }

    @PostMapping("/exports")
    public String startExport(@RequestParam("type") ExportType type, HttpSession session) {
        User user = sessionUserResolver.resolve(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ExportJob job = exportService.startExport(user, type);
        return "redirect:/exports/" + job.getId();
    }

    @GetMapping("/exports/{id}")
    public String progress(@PathVariable("id") UUID id, HttpSession session, Model model) {
        User user = sessionUserResolver.resolve(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ExportJob job = exportJobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("job", job);
        return "progress";
    }

    @GetMapping("/exports/{id}/status")
    @ResponseBody
    public ExportJobStatus status(@PathVariable("id") UUID id, HttpSession session) {
        User user = sessionUserResolver.resolve(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ExportJob job = exportJobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ExportJobStatus.from(job);
    }

    public record ExportJobStatus(String id,
                                  ExportStatus status,
                                  int progressPercent,
                                  ExportPhase phase,
                                  Instant startedAt,
                                  Instant completedAt,
                                  Integer rowCount,
                                  String errorMessage) {
        static ExportJobStatus from(ExportJob job) {
            return new ExportJobStatus(
                    job.getId().toString(),
                    job.getStatus(),
                    job.getProgressPercent(),
                    job.getPhase(),
                    job.getStartedAt(),
                    job.getCompletedAt(),
                    job.getRowCount(),
                    job.getErrorMessage()
            );
        }
    }
}
