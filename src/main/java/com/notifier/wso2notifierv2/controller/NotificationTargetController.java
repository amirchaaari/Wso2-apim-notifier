package com.notifier.wso2notifierv2.controller;

import com.notifier.wso2notifierv2.entity.NotificationTarget;
import com.notifier.wso2notifierv2.repository.NotificationTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-targets")
@RequiredArgsConstructor
public class NotificationTargetController {

    private final NotificationTargetRepository targetRepository;

    @GetMapping
    public List<NotificationTarget> getAllTargets() {
        return targetRepository.findAll();
    }

    @PostMapping
    public NotificationTarget createTarget(@RequestBody NotificationTarget target) {
        return targetRepository.save(target);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTarget> updateTarget(@PathVariable Long id,
            @RequestBody NotificationTarget targetDetails) {
        return targetRepository.findById(id)
                .map(target -> {
                    target.setName(targetDetails.getName());
                    target.setChannel(targetDetails.getChannel());
                    target.setContact(targetDetails.getContact());
                    target.setEnabled(targetDetails.isEnabled());
                    return ResponseEntity.ok(targetRepository.save(target));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(@PathVariable Long id) {
        return targetRepository.findById(id)
                .map(target -> {
                    targetRepository.delete(target);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotificationTarget> toggleTarget(@PathVariable Long id) {
        return targetRepository.findById(id)
                .map(target -> {
                    target.setEnabled(!target.isEnabled());
                    return ResponseEntity.ok(targetRepository.save(target));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
