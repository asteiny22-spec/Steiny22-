# Security Evidence Triage

## Purpose
Collect, preserve, correlate, and classify objective evidence of possible account or repository compromise without guessing identities or treating unexplained events as proof of an attacker.

## Trigger phrases
Run this skill when the user says things like: "run security sweep", "check for unauthorized access", "who changed this", "security anomaly", "account compromise", "someone changed my repo", or "security evidence triage".

## Core rules
1. Evidence first. Separate verified events, plausible explanations, unresolved anomalies, and unsupported hypotheses.
2. Never identify a person from an IP address, geolocation, username, device label, or timing alone.
3. Never access, probe, track, phish, exploit, or monitor another person's account or device.
4. Never store passwords, MFA codes, recovery codes, cookies, API keys, access tokens, session tokens, private keys, full payment data, health records, or message bodies in the evidence ledger.
5. Never store sensitive incident evidence in a public repository. Public GitHub may contain only this generic procedure and non-sensitive tooling.
6. Preserve evidence before remediation when practical, but do not delay urgent containment of a clearly compromised account.
7. Record assistant-initiated actions separately so they are not later misclassified as unexplained activity.

## Evidence classes
- VERIFIED_EXPECTED: event matches a known user or assistant-authorized action.
- VERIFIED_UNAUTHORIZED: user denies the action and independent evidence shows an unauthorized session/token/account performed it.
- UNRESOLVED_ANOMALY: event is real but attribution is not established.
- NORMAL_PLATFORM_BEHAVIOR: event is explained by platform, browser, app, CI, sync, cache, or service behavior.
- INSUFFICIENT_EVIDENCE: claim cannot be supported by available logs.

## Standard sweep
### 1. Establish time window
Use the user's local timezone. Record exact start/end and any reported event times.

### 2. GitHub
- Read current repo metadata and default branch.
- List recent commits in the requested window.
- For suspicious commits, fetch commit metadata and diffs.
- Compare author/committer, timestamp, changed files, and commit purpose against known assistant actions in the conversation.
- Check workflow runs tied to relevant commits and whether CI succeeded or failed.
- Inspect PRs/issues/comments only when relevant.
- Flag commits that cannot be reconciled with known user or assistant actions.
- Do not treat GitHub username attribution as proof of the physical operator if credentials or tokens may be shared/compromised.

### 3. Gmail security signals
Search for recent messages containing or sent by providers with terms such as:
- security alert
- new sign-in
- new login
- password changed
- recovery email
- verification code
- two-factor / 2FA
- passkey
- new device
- OAuth / app access
- personal access token / PAT
- SSH key
- GitHub security
- Google security
- Samsung account
- Microsoft account
Read only messages necessary to classify the event. Record metadata and concise findings, not full sensitive message bodies.

### 4. Connected-service evidence
When a connector exposes security/session/audit data, inspect it. If the connector does not expose that surface, state the limitation and ask for an export or screenshot rather than pretending access exists.

### 5. Device/account evidence supplied by user
Accept screenshots/exports of account sessions, security logs, device lists, sign-in history, OAuth apps, tokens, SSH keys, browser extensions, VPNs, device-admin apps, accessibility services, and malware scans. Correlate by timestamp.

### 6. Correlation
For each event, record:
- timestamp and timezone
- service/account
- event/action
- evidence source
- actor/account label shown by provider
- device/location/IP if provider actually supplies it
- known explanation
- confidence
- classification
- recommended next action

## IP address handling
- Treat IP as a network indicator, not an identity.
- Note VPN, cellular carrier NAT, shared Wi-Fi, proxies, IPv6 privacy addresses, and cloud infrastructure as confounders.
- Compare repeated IPs only when supplied by the service's own audit data.
- Do not use public IP-tracker repositories to deanonymize or pursue a person.

## Containment checklist
If evidence indicates likely compromise:
1. Preserve/export security logs and screenshots.
2. Change the primary email password from a trusted device.
3. Revoke unknown sessions.
4. Rotate GitHub PATs, SSH keys, deploy keys, OAuth/GitHub Apps, and webhooks as applicable.
5. Enable passkeys or strong MFA.
6. Review recovery email/phone settings.
7. Revoke app passwords and suspicious third-party access.
8. Re-check after containment for recurrence.

## Evidence ledger
Store sensitive incident notes only in a private workspace/database. Suggested fields:
- Event
- Timestamp
- Service
- Evidence Source
- Classification
- Confidence
- Known Explanation
- Follow-up
- Resolved

## Output format
Start with a short verdict:
- "No unexplained account changes found in the inspected window"
- "X unresolved anomalies remain"
- "Evidence supports likely account compromise"

Then summarize only the highest-value events and next containment step. Do not amplify unsupported claims.
