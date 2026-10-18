package com.amplify.amplify_backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amplify.amplify_backend.model.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {}
