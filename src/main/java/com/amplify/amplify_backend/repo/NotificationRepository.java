package com.amplify.amplify_backend.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amplify.amplify_backend.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
	}
