package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.nio.file.Path;
import java.util.UUID;

@Controller
public class ExportDownloadController {
    private final ExportJobRepository exportJobRepository;
    private final SessionUserResolver sessionUserResolver;

    public ExportDownloadController(ExportJobRepository exportJobRepository,
                                    SessionUserResolver sessionUserResolver) {
        this.exportJobRepository = exportJobRepository;
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping("/exports/{id}/download")
    public Object download(@PathVariable("id") UUID id, HttpSession session) {
        User user = sessionUserResolver.resolve(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ExportJob job = exportJobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (job.getStatus() != ExportStatus.COMPLETED || job.getOutputFilePath() == null) {
            ModelAndView modelAndView = new ModelAndView("export-not-ready");
            modelAndView.setStatus(HttpStatus.CONFLICT);
            modelAndView.addObject("job", job);
            return modelAndView;
        }

        Path path = Path.of(job.getOutputFilePath());
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/tab-separated-values"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getOutputFileName() + "\"")
                .body(resource);
    }
}
