const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const AUTH_BASE_URL = process.env.NEXT_PUBLIC_AUTH_BASE_URL || "http://localhost:9000";
const AUTH_CLIENT_ID = process.env.NEXT_PUBLIC_AUTH_CLIENT_ID || "proofvault-spa";
const AUTH_REDIRECT_URI =
  process.env.NEXT_PUBLIC_AUTH_REDIRECT_URI || "http://localhost:5173/oauth2/callback";
const AUTH_SCOPES =
  process.env.NEXT_PUBLIC_AUTH_SCOPES || "openid profile email proof:read proof:write";
const WALLET_CHAIN_ID = Number(process.env.NEXT_PUBLIC_WALLET_CHAIN_ID || "31337");
const WALLET_CHAIN_HEX = `0x${WALLET_CHAIN_ID.toString(16)}`;
const WALLET_CHAIN_NAME = process.env.NEXT_PUBLIC_WALLET_CHAIN_NAME || "Local Anvil";
const WALLET_RPC_URL = process.env.NEXT_PUBLIC_WALLET_RPC_URL || "http://172.25.179.4:8545";
const WALLET_EXPLORER_URL = process.env.NEXT_PUBLIC_WALLET_EXPLORER_URL || "";
const WALLET_NATIVE_CURRENCY_NAME = process.env.NEXT_PUBLIC_WALLET_NATIVE_CURRENCY_NAME || "Anvil Ether";
const WALLET_NATIVE_CURRENCY_SYMBOL = process.env.NEXT_PUBLIC_WALLET_NATIVE_CURRENCY_SYMBOL || "ETH";

const TOKEN_KEY = "proofvault.oauth.token";
const PKCE_KEY = "proofvault.oauth.pkce";

export const config = {
  apiBaseUrl: API_BASE_URL,
  authBaseUrl: AUTH_BASE_URL,
  authClientId: AUTH_CLIENT_ID,
  authRedirectUri: AUTH_REDIRECT_URI,
  authScopes: AUTH_SCOPES,
  walletChainId: WALLET_CHAIN_ID,
  walletChainName: WALLET_CHAIN_NAME,
  walletRpcUrl: WALLET_RPC_URL,
  walletExplorerUrl: WALLET_EXPLORER_URL
};

export function getStoredToken() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(TOKEN_KEY);
  if (!raw) return null;
  try {
    const token = JSON.parse(raw);
    if (token.expiresAt && Number(token.expiresAt) < Date.now() + 15000) {
      clearSession();
      return null;
    }
    return token;
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(PKCE_KEY);
}

export async function beginLogin() {
  const verifier = randomString(96);
  const state = randomString(32);
  const challenge = await sha256Base64Url(verifier);
  window.localStorage.setItem(PKCE_KEY, JSON.stringify({ verifier, state }));

  const params = new URLSearchParams({
    response_type: "code",
    client_id: AUTH_CLIENT_ID,
    redirect_uri: AUTH_REDIRECT_URI,
    scope: AUTH_SCOPES,
    state,
    code_challenge: challenge,
    code_challenge_method: "S256"
  });

  window.location.assign(`${AUTH_BASE_URL}/oauth2/authorize?${params.toString()}`);
}

export async function completeLogin(code, state) {
  const raw = window.localStorage.getItem(PKCE_KEY);
  if (!raw) throw new Error("Missing login session. Please start sign in again.");
  const pkce = JSON.parse(raw);
  if (pkce.state !== state) throw new Error("Login state mismatch. Please retry sign in.");

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: AUTH_CLIENT_ID,
    redirect_uri: AUTH_REDIRECT_URI,
    code_verifier: pkce.verifier,
    code
  });

  const response = await fetch(`${AUTH_BASE_URL}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });

  if (!response.ok) {
    throw new Error(`Token exchange failed with status ${response.status}.`);
  }

  const token = await response.json();
  const stored = {
    accessToken: token.access_token,
    refreshToken: token.refresh_token,
    tokenType: token.token_type || "Bearer",
    scope: token.scope || AUTH_SCOPES,
    expiresAt: Date.now() + Number(token.expires_in || 900) * 1000
  };
  window.localStorage.setItem(TOKEN_KEY, JSON.stringify(stored));
  window.localStorage.removeItem(PKCE_KEY);
  return stored;
}

export async function beginWalletLogin() {
  const ethereum = getEthereumProvider();
  const [walletAddress] = await ethereum.request({ method: "eth_requestAccounts" });
  if (!walletAddress) {
    throw new Error("No wallet account was selected.");
  }

  await ensureWalletChain(ethereum);

  const challenge = await authRequest("/api/wallet/nonce", {
    walletAddress,
    chainId: WALLET_CHAIN_ID
  });

  const signature = await ethereum.request({
    method: "personal_sign",
    params: [challenge.message, challenge.walletAddress]
  });

  const token = await authRequest("/api/wallet/authenticate", {
    walletAddress: challenge.walletAddress,
    chainId: challenge.chainId,
    nonce: challenge.nonce,
    signature
  });

  const stored = {
    accessToken: token.access_token,
    tokenType: token.token_type || "Bearer",
    scope: token.scope || AUTH_SCOPES,
    walletAddress: token.wallet_address || challenge.walletAddress,
    authMethod: "wallet",
    expiresAt: Date.now() + Number(token.expires_in || 900) * 1000
  };
  window.localStorage.setItem(TOKEN_KEY, JSON.stringify(stored));
  window.localStorage.removeItem(PKCE_KEY);
  return stored;
}

export async function apiRequest(path, options = {}) {
  const token = getStoredToken();
  const headers = new Headers(options.headers || {});
  if (token?.accessToken) headers.set("Authorization", `Bearer ${token.accessToken}`);
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    let detail = `Request failed with status ${response.status}.`;
    try {
      const error = await response.json();
      detail = error.detail || error.title || detail;
    } catch {
      // Keep the plain status message when the API returns a non-JSON body.
    }
    throw new Error(detail);
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) return response.json();
  return response;
}

export function decodeJwt(token) {
  if (!token) return null;
  const [, payload] = token.split(".");
  if (!payload) return null;
  try {
    return JSON.parse(window.atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
}

function randomString(size) {
  const bytes = new Uint8Array(size);
  window.crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function sha256Base64Url(value) {
  const data = new TextEncoder().encode(value);
  const digest = await window.crypto.subtle.digest("SHA-256", data);
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function authRequest(path, body) {
  const response = await fetch(`${AUTH_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    let detail = `Wallet authentication failed with status ${response.status}.`;
    try {
      const error = await response.json();
      detail = error.detail || error.title || detail;
    } catch {
      // Keep the status message when the auth server returns a non-JSON body.
    }
    throw new Error(detail);
  }

  return response.json();
}

function getEthereumProvider() {
  if (typeof window === "undefined" || !window.ethereum) {
    throw new Error("Install a browser wallet such as MetaMask to continue.");
  }
  return window.ethereum;
}

async function ensureWalletChain(ethereum) {
  const chainId = await ethereum.request({ method: "eth_chainId" });
  if (Number.parseInt(chainId, 16) === WALLET_CHAIN_ID) {
    return;
  }

  try {
    await ethereum.request({
      method: "wallet_switchEthereumChain",
      params: [{ chainId: WALLET_CHAIN_HEX }]
    });
  } catch (error) {
    if (error?.code !== 4902) {
      throw new Error(`Switch your wallet to ${WALLET_CHAIN_NAME} chain ID ${WALLET_CHAIN_ID}.`);
    }
    const chainParams = {
      chainId: WALLET_CHAIN_HEX,
      chainName: WALLET_CHAIN_NAME,
      nativeCurrency: {
        name: WALLET_NATIVE_CURRENCY_NAME,
        symbol: WALLET_NATIVE_CURRENCY_SYMBOL,
        decimals: 18
      },
      rpcUrls: [WALLET_RPC_URL]
    };
    if (WALLET_EXPLORER_URL) {
      chainParams.blockExplorerUrls = [WALLET_EXPLORER_URL];
    }

    await ethereum.request({
      method: "wallet_addEthereumChain",
      params: [chainParams]
    });
  }
}
