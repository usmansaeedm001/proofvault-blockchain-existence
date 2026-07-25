"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  ArrowDownToLine,
  BadgeCheck,
  Blocks,
  CheckCircle2,
  ClipboardCheck,
  Copy,
  Database,
  ExternalLink,
  FileCheck2,
  FileSearch,
  FileUp,
  Fingerprint,
  Gauge,
  Layers3,
  Loader2,
  LockKeyhole,
  LogOut,
  PauseCircle,
  RefreshCw,
  Search,
  ShieldCheck,
  ShieldEllipsis,
  Sparkles,
  UploadCloud,
  WalletCards,
  XCircle,
  Zap
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import {
  apiRequest,
  beginWalletLogin,
  clearSession,
  config,
  decodeJwt,
  getStoredToken
} from "../lib/proofvault";

const emptyStatus = {
  mode: "awaiting backend",
  network: config.walletChainName,
  connected: false,
  chainId: config.walletChainId,
  latestBlockNumber: null,
  contractAddress: "",
  anchorAddress: "",
  message: "Authenticate to load backend blockchain status."
};

const emptyInsights = {
  status: emptyStatus,
  onChainTotalProofs: 0,
  offChainTotalProofs: 0,
  offChainUserProofs: 0
};

const emptyMetrics = {
  anchors: 0,
  verifications: 0,
  errors: 0,
  anchorDurationCount: 0,
  anchorDurationTotalSeconds: 0,
  verifyDurationCount: 0,
  verifyDurationTotalSeconds: 0
};

const contractControls = [
  { icon: ShieldEllipsis, label: "Admin governance", value: "DEFAULT_ADMIN_ROLE" },
  { icon: UploadCloud, label: "Anchoring relayer", value: "ANCHOR_ROLE" },
  { icon: PauseCircle, label: "Emergency pause", value: "PAUSER_ROLE" },
  { icon: Layers3, label: "UUPS upgrades", value: "UPGRADER_ROLE" },
  { icon: ClipboardCheck, label: "Duplicate guard", value: "ProofAlreadyExists" },
  { icon: Blocks, label: "Batch anchoring", value: "storeProofs up to 100" }
];

const apiCapabilities = [
  "Wallet registration and login",
  "JWT session from wallet challenge",
  "File hash anchoring",
  "Public hash verification",
  "Certificate export",
  "Subscription usage",
  "Blockchain status",
  "OTEL metrics"
];

const chartColors = ["#38d996", "#4bb3fd", "#f6c85f", "#f45d7a"];

export default function HomePage() {
  const fileInputRef = useRef(null);
  const [token, setToken] = useState(null);
  const [profile, setProfile] = useState(null);
  const [status, setStatus] = useState(emptyStatus);
  const [insights, setInsights] = useState(emptyInsights);
  const [metrics, setMetrics] = useState(emptyMetrics);
  const [subscription, setSubscription] = useState(null);
  const [proofs, setProofs] = useState([]);
  const [verifyHash, setVerifyHash] = useState("");
  const [verification, setVerification] = useState(null);
  const [onChainHash, setOnChainHash] = useState("");
  const [onChainProof, setOnChainProof] = useState(null);
  const [walletAccount, setWalletAccount] = useState(null);
  const [notice, setNotice] = useState("Connect MetaMask and sign the wallet challenge to load live ProofVault data.");
  const [busy, setBusy] = useState(false);
  const [walletBusy, setWalletBusy] = useState(false);

  const signedIn = Boolean(token?.accessToken);
  const claims = useMemo(() => decodeJwt(token?.accessToken), [token]);
  const walletAddress = token?.walletAddress || claims?.wallet_address || walletAccount;
  const anchorAvg = average(metrics.anchorDurationTotalSeconds, metrics.anchorDurationCount);
  const verifyAvg = average(metrics.verifyDurationTotalSeconds, metrics.verifyDurationCount);
  const sync = syncPercent(insights);
  const subscriptionUsage = subscription
    ? Math.min(100, (Number(subscription.usage || 0) / Math.max(1, Number(subscription.monthlyProofLimit || 1))) * 100)
    : 0;

  useEffect(() => {
    const stored = getStoredToken();
    if (stored) {
      setToken(stored);
      refreshData(stored);
    }
    detectWallet();
  }, []);

  async function detectWallet() {
    if (typeof window === "undefined" || !window.ethereum) return;
    try {
      const accounts = await window.ethereum.request({ method: "eth_accounts" });
      setWalletAccount(accounts?.[0] || null);
    } catch {
      setWalletAccount(null);
    }
  }

  async function refreshData(existingToken = token) {
    if (!existingToken?.accessToken) {
      setNotice("Wallet connection is not the same as API authentication. Sign the wallet challenge to load backend data.");
      return;
    }

    setBusy(true);
    try {
      const [me, chainStatus, chainInsights, otel, currentSubscription, recentProofs] = await Promise.all([
        apiRequest("/api/me"),
        apiRequest("/api/blockchain/status"),
        apiRequest("/api/blockchain/insights"),
        apiRequest("/api/observability/blockchain"),
        apiRequest("/api/subscription"),
        apiRequest("/api/proofs")
      ]);

      setProfile(me);
      setStatus(chainStatus);
      setInsights(chainInsights);
      setMetrics(otel);
      setSubscription(currentSubscription);
      setProofs(Array.isArray(recentProofs) ? recentProofs : []);
      if (!verifyHash && recentProofs?.[0]?.fileHash) {
        setVerifyHash(recentProofs[0].fileHash);
        setOnChainHash(recentProofs[0].fileHash);
      }
      setNotice("Live backend, subscription, blockchain, and OTEL data loaded.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function signInWithWallet() {
    setWalletBusy(true);
    try {
      const walletToken = await beginWalletLogin();
      setToken(walletToken);
      setWalletAccount(walletToken.walletAddress);
      setNotice(`Wallet ${shortHash(walletToken.walletAddress)} authenticated. Loading backend data.`);
      await refreshData(walletToken);
    } catch (error) {
      setNotice(error.message);
    } finally {
      setWalletBusy(false);
    }
  }

  async function uploadProof(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!signedIn) {
      setNotice("Authenticate before anchoring a file.");
      event.target.value = "";
      return;
    }

    const body = new FormData();
    body.append("file", file);
    setBusy(true);
    try {
      const proof = await apiRequest("/api/proofs/upload", { method: "POST", body });
      setProofs((current) => [proof, ...current.filter((item) => item.id !== proof.id)]);
      setVerifyHash(proof.fileHash);
      setOnChainHash(proof.fileHash);
      setNotice(`${file.name} was hashed, recorded, and anchored.`);
      await refreshData();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
      event.target.value = "";
    }
  }

  async function verifyProof() {
    if (!verifyHash.trim()) return;
    setBusy(true);
    try {
      const result = await apiRequest("/api/proofs/verify", {
        method: "POST",
        body: JSON.stringify({ fileHash: verifyHash.trim() })
      });
      setVerification(result);
      setNotice(result.message || "Verification completed.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function lookupOnChainProof() {
    if (!onChainHash.trim()) return;
    if (!signedIn) {
      setNotice("Authenticate to query the on-chain proof endpoint.");
      return;
    }
    setBusy(true);
    try {
      const result = await apiRequest(`/api/blockchain/proofs/${normalizeHash(onChainHash)}`);
      setOnChainProof(result);
      setNotice(result.message || "On-chain lookup completed.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function downloadCertificate(proofId) {
    if (!signedIn || !proofId) return;
    try {
      const response = await apiRequest(`/api/proofs/${proofId}/certificate`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `proofvault-certificate-${proofId}.txt`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setNotice(error.message);
    }
  }

  function signOut() {
    clearSession();
    setToken(null);
    setProfile(null);
    setSubscription(null);
    setProofs([]);
    setStatus(emptyStatus);
    setInsights(emptyInsights);
    setMetrics(emptyMetrics);
    setVerification(null);
    setOnChainProof(null);
    setNotice("Signed out. Connect again to load live backend data.");
  }

  const proofMix = [
    { name: "On-chain", value: Number(insights.onChainTotalProofs || 0) },
    { name: "Indexed", value: Number(insights.offChainTotalProofs || 0) },
    { name: "Your vault", value: Number(insights.offChainUserProofs || 0) },
    { name: "Errors", value: Number(metrics.errors || 0) }
  ];

  const operationMetrics = [
    { name: "anchors", value: Number(metrics.anchors || 0) },
    { name: "verifies", value: Number(metrics.verifications || 0) },
    { name: "errors", value: Number(metrics.errors || 0) }
  ];

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-mark">
          <ShieldCheck size={22} />
          <div>
            <strong>ProofVault</strong>
            <span>Proof-of-existence console</span>
          </div>
        </div>
        <div className="topbar-actions">
          <StatusBadge active={Boolean(walletAccount)} label={walletAccount ? `Wallet ${shortHash(walletAccount)}` : "Wallet not connected"} />
          <StatusBadge active={signedIn} label={signedIn ? "API authenticated" : "API locked"} />
          {signedIn ? (
            <>
              <button className="icon-button" type="button" onClick={() => refreshData()} aria-label="Refresh dashboard">
                {busy ? <Loader2 className="spin" size={18} /> : <RefreshCw size={18} />}
              </button>
              <button className="icon-button" type="button" onClick={signOut} aria-label="Sign out">
                <LogOut size={18} />
              </button>
            </>
          ) : (
            <>
              <button className="primary-button" type="button" onClick={signInWithWallet}>
                {walletBusy ? <Loader2 className="spin" size={18} /> : <WalletCards size={18} />}
                Connect Wallet
              </button>
            </>
          )}
        </div>
      </header>

      <section className="command-center">
        <div className="command-copy">
          <span className="eyebrow-line">
            <Sparkles size={16} />
            Backend and blockchain aligned
          </span>
          <h1>Anchor file fingerprints and verify proof state from one live console.</h1>
          <p>
            Connected to {config.apiBaseUrl} and {config.authBaseUrl}. Wallet auth targets {config.walletChainName} chain ID {config.walletChainId}.
          </p>
          <div className="action-row">
            <button className="primary-button" type="button" onClick={() => fileInputRef.current?.click()} disabled={!signedIn || busy}>
              <FileUp size={18} />
              Anchor File
            </button>
            <button className="secondary-button" type="button" onClick={() => refreshData()} disabled={!signedIn || busy}>
              {busy ? <Loader2 className="spin" size={18} /> : <RefreshCw size={18} />}
              Refresh Data
            </button>
            <input ref={fileInputRef} className="hidden-input" type="file" onChange={uploadProof} />
          </div>
        </div>

        <div className="network-card">
          <div className="card-header">
            <span>Network State</span>
            <StatusBadge active={Boolean(status.connected)} label={status.connected ? "Connected" : "Not connected"} />
          </div>
          <div className="metric-grid">
            <Metric label="Mode" value={status.mode || "n/a"} />
            <Metric label="Network" value={status.network || "n/a"} />
            <Metric label="Chain ID" value={String(status.chainId || config.walletChainId)} />
            <Metric label="Latest Block" value={status.latestBlockNumber ? compactNumber(status.latestBlockNumber) : "pending"} />
          </div>
          <AddressLine label="Contract" value={status.contractAddress || "not configured"} />
          <AddressLine label="Anchor" value={status.anchorAddress || "not configured"} />
          <p className="card-message">{status.message || notice}</p>
        </div>
      </section>

      <section className="notice-strip">
        <ShieldCheck size={18} />
        <span>{notice}</span>
      </section>

      <section className="kpi-grid">
        <Kpi icon={Blocks} label="On-chain proofs" value={compactNumber(insights.onChainTotalProofs)} tone="mint" />
        <Kpi icon={Database} label="Indexed proofs" value={compactNumber(insights.offChainTotalProofs)} tone="sky" />
        <Kpi icon={Fingerprint} label="Your vault" value={compactNumber(insights.offChainUserProofs)} tone="gold" />
        <Kpi icon={Gauge} label="Anchor avg" value={`${anchorAvg.toFixed(3)}s`} tone="rose" />
      </section>

      <section className="main-grid">
        <article className="panel upload-panel">
          <div className="panel-title">
            <div>
              <span>Proof Creation</span>
              <h2>File hash anchoring</h2>
            </div>
            <UploadCloud size={22} />
          </div>
          <div className="drop-zone" onClick={() => signedIn && fileInputRef.current?.click()}>
            <FileUp size={34} />
            <strong>{signedIn ? "Select a file to anchor" : "Authenticate to anchor files"}</strong>
            <span>Backend stores metadata and anchors only cryptographic fingerprints.</span>
          </div>
          <div className="usage-box">
            <div>
              <span>Subscription</span>
              <strong>{subscription?.tier || "Not loaded"}</strong>
            </div>
            <div>
              <span>Usage</span>
              <strong>
                {subscription ? `${subscription.usage}/${formatLimit(subscription.monthlyProofLimit)}` : "pending"}
              </strong>
            </div>
          </div>
          <Progress label="Monthly proof usage" value={subscriptionUsage} tone="gold" />
        </article>

        <article className="panel verify-panel">
          <div className="panel-title">
            <div>
              <span>Public Verify</span>
              <h2>Database-backed hash check</h2>
            </div>
            <FileSearch size={22} />
          </div>
          <textarea
            value={verifyHash}
            onChange={(event) => setVerifyHash(event.target.value)}
            placeholder="Paste a SHA-256 file hash"
            spellCheck={false}
          />
          <button className="primary-button full" type="button" onClick={verifyProof} disabled={busy || !verifyHash.trim()}>
            {busy ? <Loader2 className="spin" size={18} /> : <Search size={18} />}
            Verify Hash
          </button>
          {verification && (
            <ResultCard success={verification.exists} title={verification.exists ? "Proof exists" : "No proof found"}>
              {verification.message}
            </ResultCard>
          )}
        </article>

        <article className="panel verify-panel">
          <div className="panel-title">
            <div>
              <span>On-chain Lookup</span>
              <h2>Smart contract proof state</h2>
            </div>
            <Blocks size={22} />
          </div>
          <textarea
            value={onChainHash}
            onChange={(event) => setOnChainHash(event.target.value)}
            placeholder="Paste a SHA-256 file hash"
            spellCheck={false}
          />
          <button className="secondary-button full" type="button" onClick={lookupOnChainProof} disabled={busy || !onChainHash.trim()}>
            {busy ? <Loader2 className="spin" size={18} /> : <ExternalLink size={18} />}
            Query Chain
          </button>
          {onChainProof && (
            <ResultCard success={onChainProof.exists} title={onChainProof.exists ? "On-chain proof found" : "No on-chain proof"}>
              {onChainProof.metadataHash || onChainProof.message}
            </ResultCard>
          )}
        </article>

        <article className="panel wide">
          <div className="panel-title">
            <div>
              <span>Vault History</span>
              <h2>Recent proofs and certificates</h2>
            </div>
            <FileCheck2 size={22} />
          </div>
          <div className="proof-list">
            {proofs.length ? proofs.map((proof) => (
              <div className="proof-row" key={proof.id}>
                <div className="proof-main">
                  <div className="file-icon">
                    <FileCheck2 size={19} />
                  </div>
                  <div>
                    <strong>{proof.fileName || "Untitled file"}</strong>
                    <span>{shortHash(proof.fileHash)}</span>
                  </div>
                </div>
                <div className="proof-meta">
                  <span>{formatBytes(proof.fileSize)}</span>
                  <span>{proof.network || status.network || "chain"}</span>
                  <span>{formatDate(proof.blockchainTimestamp)}</span>
                </div>
                <div className="row-actions">
                  <button type="button" onClick={() => copyText(proof.fileHash)} aria-label="Copy hash">
                    <Copy size={16} />
                  </button>
                  <button type="button" onClick={() => {
                    setVerifyHash(proof.fileHash);
                    setOnChainHash(proof.fileHash);
                  }} aria-label="Use hash">
                    <Search size={16} />
                  </button>
                  <button type="button" onClick={() => downloadCertificate(proof.id)} aria-label="Download certificate">
                    <ArrowDownToLine size={16} />
                  </button>
                </div>
              </div>
            )) : (
              <div className="empty-state">
                <FileCheck2 size={34} />
                <strong>No proofs loaded</strong>
                <span>Authenticated proof history will appear here.</span>
              </div>
            )}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title">
            <div>
              <span>Proof Mix</span>
              <h2>Chain and index parity</h2>
            </div>
            <Layers3 size={22} />
          </div>
          <div className="donut-wrap">
            <ResponsiveContainer width="100%" height={230}>
              <PieChart>
                <Pie data={proofMix} dataKey="value" innerRadius={60} outerRadius={88} paddingAngle={4}>
                  {proofMix.map((item, index) => (
                    <Cell key={item.name} fill={chartColors[index % chartColors.length]} />
                  ))}
                </Pie>
                <Tooltip content={<ChartTooltip />} />
              </PieChart>
            </ResponsiveContainer>
            <div className="donut-center">
              <strong>{Math.round(sync)}%</strong>
              <span>sync</span>
            </div>
          </div>
          <Progress label="On-chain sync" value={sync} tone="mint" />
        </article>

        <article className="panel">
          <div className="panel-title">
            <div>
              <span>OTEL Pulse</span>
              <h2>Blockchain operation metrics</h2>
            </div>
            <Activity size={22} />
          </div>
          <ResponsiveContainer width="100%" height={230}>
            <BarChart data={operationMetrics}>
              <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: "#7c879a", fontSize: 12 }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: "#7c879a", fontSize: 12 }} />
              <Tooltip content={<ChartTooltip />} />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                {operationMetrics.map((item, index) => (
                  <Cell key={item.name} fill={chartColors[index % chartColors.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <div className="mini-grid">
            <Metric label="Verify avg" value={`${verifyAvg.toFixed(3)}s`} />
            <Metric label="Anchor events" value={compactNumber(metrics.anchorDurationCount)} />
          </div>
        </article>

        <article className="panel">
          <div className="panel-title">
            <div>
              <span>Security Model</span>
              <h2>Smart contract controls</h2>
            </div>
            <LockKeyhole size={22} />
          </div>
          <div className="control-list">
            {contractControls.map(({ icon: Icon, label, value }) => (
              <div className="control-row" key={label}>
                <Icon size={18} />
                <div>
                  <strong>{label}</strong>
                  <span>{value}</span>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel wide">
          <div className="panel-title">
            <div>
              <span>Backend Surface</span>
              <h2>Available product capabilities</h2>
            </div>
            <Zap size={22} />
          </div>
          <div className="capability-grid">
            {apiCapabilities.map((capability) => (
              <div className="capability" key={capability}>
                <BadgeCheck size={18} />
                <span>{capability}</span>
              </div>
            ))}
          </div>
        </article>
      </section>
    </main>
  );
}

function StatusBadge({ active, label }) {
  return <span className={active ? "status-badge active" : "status-badge"}>{label}</span>;
}

function Kpi({ icon: Icon, label, value, tone }) {
  return (
    <article className={`kpi ${tone}`}>
      <Icon size={22} />
      <span>{label}</span>
      <strong>{value ?? "0"}</strong>
    </article>
  );
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value || "n/a"}</strong>
    </div>
  );
}

function AddressLine({ label, value }) {
  return (
    <div className="address-line">
      <span>{label}</span>
      <strong>{shortHash(value)}</strong>
    </div>
  );
}

function Progress({ label, value, tone }) {
  const normalized = Math.round(Math.max(0, Math.min(100, Number(value || 0))));
  return (
    <div className="progress-row">
      <div>
        <span>{label}</span>
        <strong>{normalized}%</strong>
      </div>
      <div className={`track ${tone}`}>
        <span style={{ width: `${normalized}%` }} />
      </div>
    </div>
  );
}

function ResultCard({ success, title, children }) {
  return (
    <div className={success ? "result-card success" : "result-card fail"}>
      {success ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
      <div>
        <strong>{title}</strong>
        <span>{children || "No additional data returned."}</span>
      </div>
    </div>
  );
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      {label && <strong>{label}</strong>}
      {payload.map((item) => (
        <span key={item.name || item.dataKey}>
          {item.name || item.dataKey}: {compactNumber(item.value)}
        </span>
      ))}
    </div>
  );
}

function average(total, count) {
  const safeCount = Number(count || 0);
  return safeCount ? Number(total || 0) / safeCount : 0;
}

function syncPercent(current) {
  const indexed = Number(current.offChainTotalProofs || 0);
  const onChain = Number(current.onChainTotalProofs || 0);
  if (!indexed) return 0;
  return Math.min(100, (onChain / indexed) * 100);
}

function compactNumber(value) {
  const number = Number(value || 0);
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(number);
}

function shortHash(value) {
  if (!value) return "n/a";
  if (value.length <= 18) return value;
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

function normalizeHash(value) {
  const trimmed = value.trim();
  return trimmed.startsWith("0x") ? trimmed : `0x${trimmed}`;
}

function formatBytes(value) {
  const bytes = Number(value || 0);
  if (!bytes) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`;
}

function formatDate(value) {
  if (!value) return "pending";
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatLimit(value) {
  const number = Number(value || 0);
  return number > 1000000 ? "unlimited" : String(number);
}

async function copyText(value) {
  if (!value || typeof navigator === "undefined") return;
  await navigator.clipboard.writeText(value);
}
