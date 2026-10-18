package com.amplify.amplify_backend.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amplify.amplify_backend.model.Contribution;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {
	  List<Contribution> findByUserIdOrderByCreatedAtDesc(Long userId);
	}
