import "./globals.css";

export const metadata = {
  title: "ProofVault",
  description: "Blockchain proof-of-existence dashboard for creators, legal teams, and professionals."
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
