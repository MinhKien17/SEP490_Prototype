package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Graph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GraphRepository extends JpaRepository<Graph, Integer> {

    Optional<Graph> findByClaimId(Integer claimId);
}
