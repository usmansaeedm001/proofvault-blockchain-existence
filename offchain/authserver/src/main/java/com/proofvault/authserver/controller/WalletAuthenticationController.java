package com.proofvault.authserver.controller;

import com.proofvault.authserver.dto.WalletAuthenticationRequest;
import com.proofvault.authserver.dto.WalletNonceRequest;
import com.proofvault.authserver.dto.WalletNonceResponse;
import com.proofvault.authserver.dto.WalletTokenResponse;
import com.proofvault.authserver.service.WalletAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletAuthenticationController {
	private final WalletAuthenticationService walletAuthenticationService;

	public WalletAuthenticationController(WalletAuthenticationService walletAuthenticationService) {
		this.walletAuthenticationService = walletAuthenticationService;
	}

	@PostMapping("/nonce")
	public WalletNonceResponse nonce(@Valid @RequestBody WalletNonceRequest request) {
		return walletAuthenticationService.createChallenge(request);
	}

	@PostMapping("/authenticate")
	public WalletTokenResponse authenticate(@Valid @RequestBody WalletAuthenticationRequest request) {
		return walletAuthenticationService.authenticate(request);
	}
}
