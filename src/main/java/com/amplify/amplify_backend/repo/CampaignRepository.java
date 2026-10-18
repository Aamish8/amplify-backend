package com.amplify.amplify_backend.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amplify.amplify_backend.model.Campaign;
import com.amplify.amplify_backend.model.CampaignStatus;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
	  List<Campaign> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
	  List<Campaign> findByStatusIn(Collection<CampaignStatus> statuses);
	}