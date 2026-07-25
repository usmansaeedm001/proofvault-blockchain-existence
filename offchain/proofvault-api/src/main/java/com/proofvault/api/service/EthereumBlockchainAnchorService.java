package com.proofvault.api.service;

import com.proofvault.api.config.ProofVaultProperties;
import com.proofvault.api.dto.BlockchainReceipt;
import com.proofvault.api.dto.BlockchainStatusResponse;
import com.proofvault.api.dto.OnChainProofResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

@Service
@ConditionalOnProperty(prefix = "proofvault.blockchain", name = "mode", havingValue = "ethereum")
public class EthereumBlockchainAnchorService implements IBlockchainAnchorService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EthereumBlockchainAnchorService.class);
	private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
	private final ProofVaultProperties.Blockchain properties;
	private final Web3j web3j;
	private final PollingTransactionReceiptProcessor receiptProcessor;
	private final ObservationRegistry observationRegistry;
	private final Counter anchorCounter;
	private final Counter verificationCounter;
	private final Counter errorCounter;
	private final Timer anchorTimer;
	private final Timer verificationTimer;

	public EthereumBlockchainAnchorService(ProofVaultProperties proofVaultProperties, MeterRegistry meterRegistry, ObservationRegistry observationRegistry) {
		this.properties = proofVaultProperties.blockchain();
		this.web3j = Web3j.build(new HttpService(properties.rpcUrl()));
		this.receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1_000, 60);
		this.observationRegistry = observationRegistry;
		this.anchorCounter = Counter.builder("proofvault.blockchain.anchors")
			.description("Number of proof anchoring attempts")
			.tag("mode", "ethereum")
			.tag("network", properties.networkName())
			.register(meterRegistry);
		this.verificationCounter = Counter.builder("proofvault.blockchain.verifications")
			.description("Number of on-chain verification calls")
			.tag("mode", "ethereum")
			.tag("network", properties.networkName())
			.register(meterRegistry);
		this.errorCounter = Counter.builder("proofvault.blockchain.errors")
			.description("Number of blockchain operation failures")
			.tag("mode", "ethereum")
			.tag("network", properties.networkName())
			.register(meterRegistry);
		this.anchorTimer = Timer.builder("proofvault.blockchain.anchor.duration")
			.description("Proof anchoring duration")
			.tag("mode", "ethereum")
			.tag("network", properties.networkName())
			.register(meterRegistry);
		this.verificationTimer = Timer.builder("proofvault.blockchain.verify.duration")
			.description("On-chain verification duration")
			.tag("mode", "ethereum")
			.tag("network", properties.networkName())
			.register(meterRegistry);
		LOGGER.info("Ethereum blockchain service configured network={} chainId={} rpcConfigured={} contractConfigured={} anchorAddressConfigured={} anchorKeyConfigured={}",
			properties.networkName(), properties.chainId(), hasText(properties.rpcUrl()), hasText(properties.contractAddress()), hasText(properties.anchorAddress()),
			hasText(properties.anchorPrivateKey()));
	}

	@Override
	public BlockchainReceipt storeProof(String fileHash, String metadataHash) {
		LOGGER.info("Ethereum proof anchoring requested network={} fileHash={} metadataHash={}", properties.networkName(), shortHash(fileHash), shortHash(metadataHash));
		return Observation.createNotStarted("proofvault.blockchain.store", observationRegistry)
			.lowCardinalityKeyValue("blockchain.mode", "ethereum")
			.lowCardinalityKeyValue("blockchain.network", properties.networkName())
			.observe(() -> anchorTimer.record(() -> {
				try {
					validateWriteConfiguration();
					anchorCounter.increment();
					Function function = new Function("storeProof", Arrays.asList(new Bytes32(hexBytes32(fileHash)), new Bytes32(hexBytes32(metadataHash))),
						Collections.emptyList());

					String encodedFunction = FunctionEncoder.encode(function);
					String transactionHash =
						transactionManager().sendTransaction(BigInteger.valueOf(properties.gasPriceWei()), BigInteger.valueOf(properties.gasLimit()),
							properties.contractAddress(), encodedFunction, BigInteger.ZERO).getTransactionHash();
					LOGGER.info("Ethereum proof transaction submitted network={} tx={} fileHash={}", properties.networkName(), shortHash(transactionHash), shortHash(fileHash));

					TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(transactionHash);
					if (!receipt.isStatusOK()) {
						LOGGER.warn("Ethereum proof transaction reverted network={} tx={} fileHash={}", properties.networkName(), shortHash(transactionHash), shortHash(fileHash));
						throw new IllegalStateException("Blockchain transaction reverted: " + transactionHash);
					}

					LOGGER.info("Ethereum proof anchored network={} tx={} fileHash={} block={}", properties.networkName(), shortHash(transactionHash), shortHash(fileHash),
						receipt.getBlockNumber());
					return new BlockchainReceipt(transactionHash, properties.networkName(), Instant.now());
				} catch (Exception exception) {
					errorCounter.increment();
					LOGGER.warn("Ethereum proof anchoring failed network={} fileHash={} reason={}", properties.networkName(), shortHash(fileHash), exception.getMessage());
					throw new IllegalStateException("Unable to anchor proof on blockchain.", exception);
				}
			}));
	}

	@Override
	public OnChainProofResponse verifyProof(String fileHash) {
		LOGGER.debug("Ethereum proof verification requested network={} fileHash={}", properties.networkName(), shortHash(fileHash));
		return Observation.createNotStarted("proofvault.blockchain.verify", observationRegistry)
			.lowCardinalityKeyValue("blockchain.mode", "ethereum")
			.lowCardinalityKeyValue("blockchain.network", properties.networkName())
			.observe(() -> verificationTimer.record(() -> {
				try {
					validateReadConfiguration();
					verificationCounter.increment();
					Function function = new Function("verifyProof", List.of(new Bytes32(hexBytes32(fileHash))),
						List.of(new TypeReference<Bool>() {}, new TypeReference<Address>() {}, new TypeReference<Uint64>() {},
							new TypeReference<Bytes32>() {}));
					List<Type> values = call(function);

					boolean exists = (Boolean) values.get(0).getValue();
					String submitter = values.get(1).getValue().toString();
					BigInteger timestamp = (BigInteger) values.get(2).getValue();
					String metadataHash = bytes32ToHex((byte[]) values.get(3).getValue());

					LOGGER.info("Ethereum proof verification completed network={} fileHash={} exists={} submitter={}", properties.networkName(), shortHash(fileHash), exists,
						shortAddress(submitter));
					return new OnChainProofResponse(exists, normalizeHex(fileHash), submitter, exists ? Instant.ofEpochSecond(timestamp.longValue()) : null,
						exists ? metadataHash : null, properties.networkName(), exists ? "Proof exists on-chain." : "Proof does not exist on-chain.");
				} catch (Exception exception) {
					errorCounter.increment();
					LOGGER.warn("Ethereum proof verification failed network={} fileHash={} reason={}", properties.networkName(), shortHash(fileHash), exception.getMessage());
					throw new IllegalStateException("Unable to verify proof on blockchain.", exception);
				}
			}));
	}

	@Override
	public BlockchainStatusResponse status() {
		if (!hasText(properties.rpcUrl())) {
			LOGGER.warn("Ethereum status unavailable network={} reason=missing_rpc_url", properties.networkName());
			return new BlockchainStatusResponse("ethereum", properties.networkName(), false, BigInteger.valueOf(properties.chainId()), null,
				properties.contractAddress(), properties.anchorAddress(), "BLOCKCHAIN_RPC_URL is required when BLOCKCHAIN_MODE=ethereum.");
		}

		try {
			EthChainId chainId = web3j.ethChainId().send();
			EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
			boolean rpcConnected = !chainId.hasError() && !blockNumber.hasError();
			String configurationMessage = configurationMessage();
			String message = rpcConnected
				? configurationMessage == null ? "Ethereum JSON-RPC connection is active." : "Ethereum JSON-RPC connection is active. " + configurationMessage
				: "Ethereum JSON-RPC connection returned an error.";
			LOGGER.debug("Ethereum status checked network={} rpcConnected={} chainId={} latestBlock={} configured={}", properties.networkName(), rpcConnected,
				chainId.getChainId(), blockNumber.getBlockNumber(), configurationMessage == null);
			return new BlockchainStatusResponse("ethereum", properties.networkName(), rpcConnected && configurationMessage == null, chainId.getChainId(),
				blockNumber.getBlockNumber(), properties.contractAddress(), properties.anchorAddress(), message);
		} catch (IOException exception) {
			errorCounter.increment();
			LOGGER.warn("Ethereum status check failed network={} reason={}", properties.networkName(), exception.getMessage());
			return new BlockchainStatusResponse("ethereum", properties.networkName(), false, BigInteger.valueOf(properties.chainId()), null,
				properties.contractAddress(), properties.anchorAddress(), exception.getMessage());
		}
	}

	@Override
	public BigInteger totalProofs() {
		try {
			validateReadConfiguration();
			Function function = new Function("totalProofs", Collections.emptyList(), List.of(new TypeReference<Uint256>() {}));
			List<Type> values = call(function);
			BigInteger total = (BigInteger) values.get(0).getValue();
			LOGGER.debug("Ethereum total proofs loaded network={} total={}", properties.networkName(), total);
			return total;
		} catch (Exception exception) {
			errorCounter.increment();
			LOGGER.warn("Ethereum total proofs lookup failed network={} reason={}", properties.networkName(), exception.getMessage());
			throw new IllegalStateException("Unable to read on-chain proof total.", exception);
		}
	}

	private List<Type> call(Function function) throws IOException {
		validateReadConfiguration();
		String encodedFunction = FunctionEncoder.encode(function);
		EthCall response = web3j.ethCall(
			Transaction.createEthCallTransaction(hasText(properties.anchorAddress()) ? properties.anchorAddress() : ZERO_ADDRESS, properties.contractAddress(),
				encodedFunction), DefaultBlockParameterName.LATEST).send();

		if (response.hasError()) {
			throw new IllegalStateException(response.getError().getMessage());
		}

		return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
	}

	private void validateReadConfiguration() {
		if (!hasText(properties.contractAddress())) {
			throw new IllegalStateException("PROOFVAULT_CONTRACT_ADDRESS is required when BLOCKCHAIN_MODE=ethereum.");
		}
	}

	private void validateWriteConfiguration() {
		validateReadConfiguration();
		if (!hasText(properties.anchorPrivateKey())) {
			throw new IllegalStateException("PROOFVAULT_ANCHOR_PRIVATE_KEY is required when BLOCKCHAIN_MODE=ethereum.");
		}
	}

	private String configurationMessage() {
		if (!hasText(properties.contractAddress())) {
			return "PROOFVAULT_CONTRACT_ADDRESS is not configured; deploy the proxy and set it before using on-chain proof APIs.";
		}
		if (!hasText(properties.anchorPrivateKey())) {
			return "PROOFVAULT_ANCHOR_PRIVATE_KEY is not configured; read APIs can work, but anchoring requires an anchor key.";
		}
		return null;
	}

	private TransactionManager transactionManager() {
		return new RawTransactionManager(web3j, Credentials.create(properties.anchorPrivateKey()), properties.chainId());
	}

	private byte[] hexBytes32(String value) {
		String normalized = normalizeHex(value);
		if (normalized.length() != 66) {
			throw new IllegalArgumentException("Expected 32-byte hex value.");
		}
		byte[] bytes = new byte[32];
		for (int index = 0; index < 32; index++) {
			int offset = 2 + index * 2;
			bytes[index] = (byte) Integer.parseInt(normalized.substring(offset, offset + 2), 16);
		}
		return bytes;
	}

	private String bytes32ToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder("0x");
		for (byte value : bytes) {
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}

	private String normalizeHex(String value) {
		String normalized = value == null ? "" : value.toLowerCase();
		return normalized.startsWith("0x") ? normalized : "0x" + normalized;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String shortHash(String hash) {
		if (hash == null || hash.isBlank()) {
			return "none";
		}
		String normalized = hash.startsWith("0x") ? hash.substring(2) : hash;
		if (normalized.length() <= 12) {
			return normalized;
		}
		return normalized.substring(0, 6) + "..." + normalized.substring(normalized.length() - 6);
	}

	private String shortAddress(String address) {
		if (address == null || address.length() < 12) {
			return "unknown";
		}
		return address.substring(0, 6) + "..." + address.substring(address.length() - 6);
	}
}
