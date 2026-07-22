"use client";

import { useEffect, useState } from "react";
import { CheckCircle2, Loader2, ShieldAlert } from "lucide-react";
import { completeLogin } from "../../../lib/proofvault";

export default function OAuthCallbackPage() {
  const [state, setState] = useState({ status: "loading", message: "Securing your ProofVault session" });

  useEffect(() => {
    async function run() {
      try {
        const params = new URLSearchParams(window.location.search);
        const code = params.get("code");
        const oauthState = params.get("state");
        const error = params.get("error");
        if (error) throw new Error(params.get("error_description") || error);
        if (!code || !oauthState) throw new Error("Missing authorization response.");
        await completeLogin(code, oauthState);
        setState({ status: "success", message: "Session sealed. Opening the vault." });
        window.setTimeout(() => window.location.replace("/"), 650);
      } catch (err) {
        setState({ status: "error", message: err.message || "Unable to finish sign in." });
      }
    }
    run();
  }, []);

  const Icon = state.status === "success" ? CheckCircle2 : state.status === "error" ? ShieldAlert : Loader2;

  return (
    <main className="callback-screen">
      <section className="callback-card">
        <Icon className={state.status === "loading" ? "spin" : ""} size={42} />
        <h1>{state.status === "error" ? "Sign In Needs Attention" : "ProofVault Auth"}</h1>
        <p>{state.message}</p>
        {state.status === "error" && (
          <a className="primary-action" href="/">
            Return to dashboard
          </a>
        )}
      </section>
    </main>
  );
}
