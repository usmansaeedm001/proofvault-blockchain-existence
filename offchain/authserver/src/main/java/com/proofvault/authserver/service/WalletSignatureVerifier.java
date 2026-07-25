package com.proofvault.authserver.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

@Component
public class WalletSignatureVerifier {
	public boolean verify(String message, String signature, String expectedAddress) {
		byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
		if (signatureBytes.length != 65) {
			return false;
		}

		byte v = signatureBytes[64];
		if (v < 27) {
			v += 27;
		}

		byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
		byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
		Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);

		try {
			BigInteger publicKey = Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), signatureData);
			String recoveredAddress = "0x" + Keys.getAddress(publicKey);
			return recoveredAddress.equalsIgnoreCase(expectedAddress);
		} catch (RuntimeException | SignatureException exception) {
			return false;
		}
	}
}
