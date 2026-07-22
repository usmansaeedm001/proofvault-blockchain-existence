package com.proofvault.api.service;

import com.proofvault.api.dto.BlockchainReceipt;
import com.proofvault.api.dto.BlockchainStatusResponse;
import com.proofvault.api.dto.OnChainProofResponse;
import java.math.BigInteger;

public interface IBlockchainAnchorService {
  BlockchainReceipt storeProof(String fileHash, String metadataHash);

  OnChainProofResponse verifyProof(String fileHash);

  BlockchainStatusResponse status();

  BigInteger totalProofs();
}
