"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  ArrowDownToLine,
  BarChart3,
  Blocks,
  CheckCircle2,
  CircuitBoard,
  Copy,
  Database,
  FileCheck2,
  FileUp,
  Fingerprint,
  Gauge,
  KeyRound,
  Link2,
  Loader2,
  LockKeyhole,
  LogOut,
  Radar,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  WalletCards,
  XCircle
} from "lucide-react";
import {
  Area,
  AreaChart,
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
import { apiRequest, beginLogin, clearSession, decodeJwt, getStoredToken, config } from "../lib/proofvault";

const demoStatus = {
  mode: "mock",
  network: "local-foundry",
  connected: true,
  chainId: 31337,
  latestBlockNumber: 924115,
  contractAddress: "0x9f4...A71c",
  anchorAddress: "0xf39...2266",
  message: "Mock chain online"
};

const demoInsights = {
  status: demoStatus,
  onChainTotalProofs: 12840,
  offChainTotalProofs: 12840,
  offChainUserProofs: 84
};

const demoMetrics = {
  anchors: 820,
  verifications: 2410,
  errors: 2,
  anchorDurationCount: 820,
  anchorDurationTotalSeconds: 101.6,
  verifyDurationCount: 2410,
  verifyDurationTotalSeconds: 86.76
};

const demoProofs = [
  {
    id: "pv_9Z4F2A",
    fileName: "client-contract-v7.pdf",
    fileHash: "f4d2b94db19c3187b8267568778f004fbd0c873872d1e2660efddbb8b4f634aa",
    fileSize: 1430900,
    transactionHash: "0x91ac4427ef938f51039c0bdf1e31cc2de61af18ccab62e70d8df10fdcc54c17a",
    network: "local-foundry",
    blockchainTimestamp: new Date(Date.now() - 1000 * 60 * 23).toISOString(),
    createdAt: new Date(Date.now() - 1000 * 60 * 23).toISOString()
  },
  {
    id: "pv_6K81BD",
    fileName: "brand-system-export.zip",
    fileHash: "bc32f0370d5f1b1120217ebf35ce7b612cb52632375a1a6f61bd2406116d8e85",
    fileSize: 9084421,
    transactionHash: "0xb3a2462e23e7360b26f7d791718f7f04cf08fd48eb63240179cd91982215e643",
    network: "local-foundry",
    blockchainTimestamp: new Date(Date.now() - 1000 * 60 * 60 * 9).toISOString(),
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 9).toISOString()
  },
  {
    id: "pv_2QJ9C1",
    fileName: "source-snapshot.tar.gz",
    fileHash: "77a93a4a8e4714542885835084791f8590d58be2f409264aa65668ad8b26d58c",
    fileSize: 3145728,
    transactionHash: "0x59e918d0b5064b7de598853ff005efc04c1af636ae8e72fc393ccb933e2e08d8",
    network: "local-foundry",
    blockchainTimestamp: new Date(Date.now() - 1000 * 60 * 60 * 29).toISOString(),
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 29).toISOString()
  }
];

const flowData = [
  { name: "00:00", proofs: 12, verifies: 28 },
  { name: "04:00", proofs: 19, verifies: 34 },
  { name: "08:00", proofs: 31, verifies: 62 },
  { name: "12:00", proofs: 46, verifies: 90 },
  { name: "16:00", proofs: 68, verifies: 121 },
  { name: "20:00", proofs: 82, verifies: 154 }
];

const COLORS = ["#54f4b4", "#f6c85f", "#67d8ff", "#fb7185"];

export default function HomePage() {
  const fileInputRef = useRef(null);
  const [token, setToken] = useState(null);
  const [profile, setProfile] = useState(null);
  const [status, setStatus] = useState(demoStatus);
  const [insights, setInsights] = useState(demoInsights);
  const [metrics, setMetrics] = useState(demoMetrics);
  const [proofs, setProofs] = useState(demoProofs);
  const [verifyHash, setVerifyHash] = useState(demoProofs[0].fileHash);
  const [verification, setVerification] = useState(null);
  const [notice, setNotice] = useState("Demo telemetry is showing until you sign in and start the backend stack.");
  const [busy, setBusy] = useState(false);

  const signedIn = Boolean(token?.accessToken);
  const claims = useMemo(() => decodeJwt(token?.accessToken), [token]);
  const anchorAvg = average(metrics.anchorDurationTotalSeconds, metrics.anchorDurationCount);
  const verifyAvg = average(metrics.verifyDurationTotalSeconds, metrics.verifyDurationCount);
  const proofDelta = Number(insights.offChainTotalProofs || 0) - Number(insights.onChainTotalProofs || 0);

  useEffect(() => {
    const stored = getStoredToken();
    if (stored) {
      setToken(stored);
      refreshData(stored);
    }
  }, []);

  async function refreshData(existingToken = token) {
    if (!existingToken?.accessToken) {
      setNotice("Sign in to stream live blockchain insights from the ProofVault API.");
      return;
    }
    setBusy(true);
    try {
      const [me, chainStatus, chainInsights, otel, recentProofs] = await Promise.all([
        apiRequest("/api/me"),
        apiRequest("/api/blockchain/status"),
        apiRequest("/api/blockchain/insights"),
        apiRequest("/api/observability/blockchain"),
        apiRequest("/api/proofs")
      ]);
      setProfile(me);
      setStatus(chainStatus);
      setInsights(chainInsights);
      setMetrics(otel);
      setProofs(recentProofs.length ? recentProofs : []);
      setNotice("Live blockchain data loaded from ProofVault API.");
    } catch (err) {
      setNotice(`${err.message} Showing demo analytics until the API is reachable.`);
    } finally {
      setBusy(false);
    }
  }

  async function uploadProof(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!signedIn) {
      setNotice("Sign in before anchoring a new proof.");
      return;
    }

    const body = new FormData();
    body.append("file", file);
    setBusy(true);
    try {
      const created = await apiRequest("/api/proofs/upload", { method: "POST", body });
      setProofs((current) => [created, ...current.filter((proof) => proof.id !== created.id)]);
      setVerifyHash(created.fileHash);
      setNotice(`${file.name} was hashed and anchored successfully.`);
      await refreshData();
    } catch (err) {
      setNotice(err.message);
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
      setNotice(result.message || (result.exists ? "Proof verified." : "Proof not found."));
    } catch (err) {
      setNotice(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function downloadCertificate(proofId) {
    if (!proofId || !signedIn) return;
    try {
      const response = await apiRequest(`/api/proofs/${proofId}/certificate`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `proofvault-certificate-${proofId}.txt`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setNotice(err.message);
    }
  }

  function signOut() {
    clearSession();
    setToken(null);
    setProfile(null);
    setNotice("Signed out. Demo telemetry is visible without a live session.");
  }

  const allocationData = [
    { name: "Anchored", value: Number(insights.onChainTotalProofs || 0) },
    { name: "User Vault", value: Number(insights.offChainUserProofs || 0) },
    { name: "Verifications", value: Number(metrics.verifications || 0) },
    { name: "Errors", value: Number(metrics.errors || 0) }
  ];

  return (
    <main className="shell">
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">
            <CircuitBoard size={16} />
            ProofVault evidence dashboard
          </div>
          <h1>Proof of existence for audit-ready digital work.</h1>
          <p>
            Anchor file fingerprints, verify originality, and monitor chain health through a
            clean professional dashboard.
          </p>
          <div className="hero-actions">
            {signedIn ? (
              <>
                <button className="primary-action" type="button" onClick={() => fileInputRef.current?.click()}>
                  <FileUp size={18} />
                  Anchor File
                </button>
                <button className="ghost-action" type="button" onClick={refreshData}>
                  {busy ? <Loader2 className="spin" size={18} /> : <RefreshCw size={18} />}
                  Refresh
                </button>
              </>
            ) : (
              <button className="primary-action" type="button" onClick={beginLogin}>
                <KeyRound size={18} />
                Sign In
              </button>
            )}
            <input ref={fileInputRef} className="hidden-input" type="file" onChange={uploadProof} />
          </div>
        </div>
        <div className="chain-terminal">
          <div className="terminal-header">
            <span>Vault Network</span>
            <span className={status.connected ? "pill positive" : "pill negative"}>
              {status.connected ? "Connected" : "Disconnected"}
            </span>
          </div>
          <div className="terminal-grid">
            <Metric label="Mode" value={status.mode || "mock"} />
            <Metric label="Network" value={status.network || "local-foundry"} />
            <Metric label="Chain ID" value={String(status.chainId || "31337")} />
            <Metric label="Latest Block" value={compactNumber(status.latestBlockNumber)} />
          </div>
          <div className="hash-strip">
            <span>Contract</span>
            <strong>{shortHash(status.contractAddress || "not deployed")}</strong>
          </div>
          <div className="hash-strip">
            <span>Anchor</span>
            <strong>{shortHash(status.anchorAddress || "mock relayer")}</strong>
          </div>
        </div>
      </section>

      <section className="status-line">
        <div>
          <ShieldCheck size={18} />
          <span>{notice}</span>
        </div>
        <div className="session-chip">
          <WalletCards size={17} />
          {signedIn ? profile?.email || claims?.sub || "Signed in" : "Demo mode"}
          {signedIn && (
            <button type="button" onClick={signOut} aria-label="Sign out">
              <LogOut size={15} />
            </button>
          )}
        </div>
      </section>

      <section className="kpi-grid">
        <Kpi icon={Blocks} label="On-chain proofs" value={compactNumber(insights.onChainTotalProofs)} tone="mint" />
        <Kpi icon={Database} label="Indexed proofs" value={compactNumber(insights.offChainTotalProofs)} tone="gold" />
        <Kpi icon={Fingerprint} label="Your vault" value={compactNumber(insights.offChainUserProofs)} tone="sky" />
        <Kpi icon={Gauge} label="Anchor avg" value={`${anchorAvg.toFixed(3)}s`} tone="rose" />
      </section>

      <section className="dashboard-grid">
        <article className="panel wide">
          <div className="panel-heading">
            <div>
              <span>Proof Activity</span>
              <h2>Anchor and verification velocity</h2>
            </div>
            <BarChart3 size={22} />
          </div>
          <div className="chart-frame">
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={flowData}>
                <defs>
                  <linearGradient id="proofFlow" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#54f4b4" stopOpacity={0.7} />
                    <stop offset="95%" stopColor="#54f4b4" stopOpacity={0.04} />
                  </linearGradient>
                  <linearGradient id="verifyFlow" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#67d8ff" stopOpacity={0.6} />
                    <stop offset="95%" stopColor="#67d8ff" stopOpacity={0.04} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: "#95a3b8", fontSize: 12 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: "#95a3b8", fontSize: 12 }} />
                <Tooltip content={<ChartTooltip />} />
                <Area type="monotone" dataKey="proofs" stroke="#54f4b4" fill="url(#proofFlow)" strokeWidth={2} />
                <Area type="monotone" dataKey="verifies" stroke="#67d8ff" fill="url(#verifyFlow)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <span>Proof Mix</span>
              <h2>Proof distribution</h2>
            </div>
            <Radar size={22} />
          </div>
          <div className="donut-wrap">
            <ResponsiveContainer width="100%" height={230}>
              <PieChart>
                <Pie data={allocationData} dataKey="value" innerRadius={62} outerRadius={92} paddingAngle={4}>
                  {allocationData.map((entry, index) => (
                    <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<ChartTooltip />} />
              </PieChart>
            </ResponsiveContainer>
            <div className="donut-center">
              <strong>{compactNumber(metrics.anchors)}</strong>
              <span>anchors</span>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <span>Synchronization</span>
              <h2>Chain and index parity</h2>
            </div>
            <TrendingUp size={22} />
          </div>
          <div className="settlement-stack">
            <ProgressRow label="On-chain sync" value={syncPercent(insights)} tone="mint" />
            <ProgressRow label="Verification speed" value={Math.max(8, Math.min(100, 100 - verifyAvg * 18))} tone="sky" />
            <ProgressRow label="Error resistance" value={Math.max(0, 100 - Number(metrics.errors || 0) * 8)} tone="gold" />
          </div>
          <div className="delta-card">
            <span>Index delta</span>
            <strong>{proofDelta === 0 ? "Balanced" : `${proofDelta} pending`}</strong>
          </div>
        </article>

        <article className="panel verify-panel">
          <div className="panel-heading">
            <div>
              <span>Public Verify</span>
              <h2>Check any SHA-256 fingerprint</h2>
            </div>
            <Search size={22} />
          </div>
          <textarea
            value={verifyHash}
            onChange={(event) => setVerifyHash(event.target.value)}
            spellCheck={false}
            aria-label="File hash to verify"
          />
          <button className="primary-action full" type="button" onClick={verifyProof}>
            {busy ? <Loader2 className="spin" size={18} /> : <ShieldCheck size={18} />}
            Verify Hash
          </button>
          {verification && (
            <div className={verification.exists ? "verify-result success" : "verify-result fail"}>
              {verification.exists ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
              <div>
                <strong>{verification.exists ? "Proof exists" : "No proof found"}</strong>
                <span>{verification.message || shortHash(verification.transactionHash)}</span>
              </div>
            </div>
          )}
        </article>

        <article className="panel wide proof-table">
          <div className="panel-heading">
            <div>
              <span>Vault History</span>
              <h2>Recent anchored assets</h2>
            </div>
            <FileCheck2 size={22} />
          </div>
          <div className="proof-list">
            {proofs.map((proof) => (
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
                  <span>{proof.network || "local-foundry"}</span>
                  <span>{formatDate(proof.blockchainTimestamp)}</span>
                </div>
                <div className="row-actions">
                  <button type="button" onClick={() => copyText(proof.fileHash)} aria-label="Copy hash">
                    <Copy size={16} />
                  </button>
                  <button type="button" onClick={() => setVerifyHash(proof.fileHash)} aria-label="Verify this hash">
                    <Search size={16} />
                  </button>
                  <button type="button" onClick={() => downloadCertificate(proof.id)} aria-label="Download certificate">
                    <ArrowDownToLine size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel telemetry-panel">
          <div className="panel-heading">
            <div>
              <span>OTEL Pulse</span>
              <h2>Blockchain operation health</h2>
            </div>
            <Activity size={22} />
          </div>
          <ResponsiveContainer width="100%" height={210}>
            <BarChart data={[
              { name: "anchors", value: Number(metrics.anchors || 0) },
              { name: "verifies", value: Number(metrics.verifications || 0) },
              { name: "errors", value: Number(metrics.errors || 0) }
            ]}>
              <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: "#95a3b8", fontSize: 12 }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: "#95a3b8", fontSize: 12 }} />
              <Tooltip content={<ChartTooltip />} />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                <Cell fill="#54f4b4" />
                <Cell fill="#67d8ff" />
                <Cell fill="#fb7185" />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <div className="mini-metrics">
            <Metric label="Verify avg" value={`${verifyAvg.toFixed(3)}s`} />
            <Metric label="Anchor events" value={compactNumber(metrics.anchorDurationCount)} />
          </div>
        </article>
      </section>

      <section className="security-strip">
        <div>
          <LockKeyhole size={18} />
          Raw files never leave the browser for storage. The API hashes uploads, stores metadata, and anchors only fingerprints.
        </div>
        <div>
          <Link2 size={18} />
          {config.apiBaseUrl} connected to {config.authBaseUrl}
        </div>
      </section>
    </main>
  );
}

function Kpi({ icon: Icon, label, value, tone }) {
  return (
    <article className={`kpi ${tone}`}>
      <div>
        <Icon size={21} />
      </div>
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

function ProgressRow({ label, value, tone }) {
  const normalized = Math.round(Math.max(0, Math.min(100, value || 0)));
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
  if (!safeCount) return 0;
  return Number(total || 0) / safeCount;
}

function syncPercent(current) {
  const off = Number(current.offChainTotalProofs || 0);
  const on = Number(current.onChainTotalProofs || 0);
  if (!off) return 100;
  return Math.min(100, (on / off) * 100);
}

function compactNumber(value) {
  const number = Number(value || 0);
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(number);
}

function shortHash(value) {
  if (!value) return "n/a";
  if (value.length <= 14) return value;
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
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

async function copyText(value) {
  if (!value || typeof navigator === "undefined") return;
  await navigator.clipboard.writeText(value);
}
