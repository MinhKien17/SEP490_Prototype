package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Dataset;
import com.evidencepilot.model.Paper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.Source;
import com.evidencepilot.model.User;
import com.evidencepilot.model.UserRole;
import org.springframework.web.server.ResponseStatusException;

public interface CurrentUserService {

    User requireCurrentUser();

    boolean isAdmin(User user);

    boolean isInstructor(User user);

    boolean ownsUserIdOrAdmin(User currentUser, Integer userId);

    void requireRole(User currentUser, UserRole role);

    void requireUserIdOrAdmin(User currentUser, Integer userId);

    void requireProjectAccess(User currentUser, Project project);

    void requireProjectWriteAccess(User currentUser, Project project);

    void requireDatasetAccess(User currentUser, Dataset dataset);

    void requireClaimAccess(User currentUser, Claim claim);

    void requirePaperAccess(User currentUser, Paper paper);

    void requireSourceAccess(User currentUser, Source source);
}
