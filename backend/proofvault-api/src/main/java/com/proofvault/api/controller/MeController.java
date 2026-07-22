package com.proofvault.api.controller;

import com.proofvault.api.dto.UserProfileResponse;
import com.proofvault.api.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {
  private final CurrentUserService currentUserService;

  public MeController(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public UserProfileResponse me(Authentication authentication) {
    return UserProfileResponse.from(currentUserService.currentUser(authentication));
  }
}
