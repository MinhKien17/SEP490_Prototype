package com.evidencepilot.service.impl;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Dataset;
import com.evidencepilot.model.Paper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.Source;
import com.evidencepilot.model.User;
import com.evidencepilot.model.UserRole;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    @Override
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "No authenticated user found");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found: " + email));
    }

    @Override
    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    @Override
    public boolean isInstructor(User user) {
        return user.getRole() == UserRole.INSTRUCTOR;
    }

    @Override
    public boolean ownsUserIdOrAdmin(User currentUser, Integer userId) {
        return isAdmin(currentUser) || currentUser.getId().equals(userId);
    }

    @Override
    public void requireRole(User currentUser, UserRole role) {
        if (currentUser.getRole() != role && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Requires role: " + role);
        }
    }

    @Override
    public void requireUserIdOrAdmin(User currentUser, Integer userId) {
        if (!ownsUserIdOrAdmin(currentUser, userId)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Access denied: not your resource");
        }
    }

    @Override
    public void requireProjectAccess(User currentUser, Project project) {
        if (isAdmin(currentUser)) return;
        if (isInstructor(currentUser)) {
            if (!project.getStudent().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Instructor access denied to project");
            }
            return;
        }
        if (!project.getStudent().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Student access denied to project");
        }
    }

    @Override
    public void requireProjectWriteAccess(User currentUser, Project project) {
        if (isAdmin(currentUser)) return;
        if (isInstructor(currentUser)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Instructors cannot modify student projects");
        }
        if (!project.getStudent().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Write access denied to project");
        }
    }

    @Override
    public void requireDatasetAccess(User currentUser, Dataset dataset) {
        if (isAdmin(currentUser)) return;
        if (isInstructor(currentUser)) {
            if (!dataset.getInstructor().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Instructor access denied to dataset");
            }
            return;
        }
        throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Students cannot access datasets");
    }

    @Override
    public void requireClaimAccess(User currentUser, Claim claim) {
        requireProjectAccess(currentUser, claim.getProject());
    }

    @Override
    public void requirePaperAccess(User currentUser, Paper paper) {
        requireProjectAccess(currentUser, paper.getProject());
    }

    @Override
    public void requireSourceAccess(User currentUser, Source source) {
        if (source.getProject() != null) {
            requireProjectAccess(currentUser, source.getProject());
        } else if (source.getDataset() != null) {
            requireDatasetAccess(currentUser, source.getDataset());
        } else {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Source not associated with project or dataset");
        }
    }
}
