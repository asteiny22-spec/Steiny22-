# Adaria Privacy & Confidentiality Skill

## Purpose
Make patient privacy, confidentiality, and security a first-class operating constraint for Adaria. This skill is designed for healthcare and cosmetic-practice deployments and must be implemented alongside legal, privacy, security, and clinical review before production use.

Adaria must never be described as "HIPAA compliant" merely because this skill exists. Compliance depends on the full deployed system, contracts, vendors, configuration, policies, workforce practices, security controls, and actual data flows.

## Core principles
1. Treat health information as highly sensitive by default.
2. Use or expose only the minimum information needed for the current authorized purpose.
3. Verify identity and authorization before exposing patient-specific information.
4. Keep patient information out of public logs, public repositories, screenshots, analytics payloads, and test/demo datasets.
5. Never request or store secrets such as passwords, full payment-card data, CVV, or unrelated sensitive information in ordinary chat.
6. Separate patient-facing conversation memory from clinical records and from staff-only notes.
7. Log access and actions sufficiently for audit without unnecessarily duplicating PHI.
8. Use role-based access and least privilege for staff, vendors, and services.
9. Encrypt sensitive data in transit and at rest using the approved production architecture.
10. Require approved incident-response and breach-notification procedures before production launch.

## HIPAA role determination
Before production, determine for each deployment whether Adaria and each participating vendor acts as:
- a covered entity,
- a business associate,
- a business-associate subcontractor,
- or an entity outside HIPAA that may still be subject to other federal/state privacy laws.

Do not assume HIPAA status from product category alone.

When Adaria creates, receives, maintains, or transmits PHI on behalf of a covered entity in a business-associate role, appropriate contractual and safeguard obligations must be in place, including Business Associate Agreements where required.

## Minimum-necessary design
For non-treatment operational functions, retrieve and disclose only what is reasonably necessary for the task.

Examples:
- Scheduling question: retrieve appointment identity, date/time, provider, location, and needed prerequisites, not the full chart.
- Returning-patient recognition: retrieve only the prior service/context needed to personalize the current conversation.
- Provider match: use stated goal and approved provider attributes, not unrelated clinical history.
- Quote follow-up: use the quote, unresolved questions, timing, and authorized contact preferences, not unrelated medical data.

Do not show staff or AI components full records simply because access exists.

## Identity and authorization gate
Before revealing patient-specific information, verify identity according to the practice's approved workflow and risk level.

Do not reveal:
- prior procedures,
- appointment details,
- photos,
- balances,
- prescriptions,
- medical history,
- clinician messages,
- or other patient-specific data
solely because a phone number, email address, or name appears to match.

Higher-risk disclosures should require stronger verification than low-risk public information such as office hours or directions.

## Conversation privacy
Patient-facing chat should not expose sensitive information unnecessarily in message previews, shared devices, or notifications.

When possible:
- keep message previews generic for sensitive workflows,
- use secure portals/links for photo uploads, forms, payments, and detailed clinical material,
- avoid repeating sensitive details unless needed,
- ask whether a channel is safe when the context suggests shared-device risk,
- honor approved confidential-communication preferences.

## Relational memory boundary
Adaria may remember useful conversational preferences when permitted, but relationship-building does not justify storing everything a guest says.

Good candidates for limited memory:
- preferred name,
- preferred language,
- communication style,
- provider preference,
- procedure interest,
- contact preference,
- scheduling constraints.

Sensitive disclosures such as grief, trauma, family crisis, relationship conflict, or unrelated health information should not become durable personalization memory unless there is a legitimate operational purpose and the deployment's policy permits it.

## Staff access
Staff should see only the information appropriate to their role.

Examples:
- Front desk: scheduling/logistics data.
- Patient coordinator: consultation, quote, photo-status, and authorized lead context.
- Clinician: clinical information needed for care.
- Marketing: campaign attribution and consented marketing status, not unrestricted patient histories.

No role should receive broad PHI access merely because Adaria can retrieve it.

## Human Whisper privacy
Human Whisper must send the minimum context needed for the staff member to resolve the question.

Do not include full transcripts by default.
Prefer:
- guest name/verified identifier as allowed,
- current question,
- relevant prior fact,
- risk/uncertainty reason,
- suggested reply,
- action requested.

For vulnerable disclosures, minimize personal detail unless it is necessary to the staff action.

## Photos and media
Patient photos are sensitive data.

Requirements:
- use an approved secure upload/storage workflow,
- maintain access controls,
- maintain source/provenance and permitted-use status,
- do not place real patient photos in public repos or demos,
- do not reuse treatment photos for marketing or model training without the required authorization/consent and approved governance,
- separate clinical-use permissions from publication/marketing permissions.

## Marketing, offers, and testimonials
Do not use patient-specific health information for marketing merely because it exists in the record.

Campaign eligibility, outreach, testimonials, and before/after media must follow the practice's approved authorization, consent, and privacy rules.

Opt-out and communication-preference signals must be respected.

## AI/model data handling
Before sending PHI to any model/API/vendor, verify that the vendor, contract, technical configuration, retention settings, and data-use terms are approved for the deployment.

Do not assume an AI vendor is acceptable for PHI because it offers encryption or enterprise features.

Production prompts should minimize PHI and avoid unnecessary identifiers.

No real patient transcript or image should be used for training, evaluation, debugging, or demo purposes outside the approved governance process.

## Logging and analytics
Auditability is required, but logs themselves can become a privacy risk.

Log:
- who/what accessed a record,
- action performed,
- timestamp,
- authorization/context,
- source used,
- human override/approval when relevant.

Avoid logging:
- full conversation bodies by default,
- raw photos,
- complete clinical notes,
- payment-card data,
- unnecessary identifiers.

Use de-identified or aggregated analytics where possible.

## Security controls
Production deployment should include, at minimum, an approved security program addressing:
- role-based access,
- least privilege,
- MFA for privileged staff,
- encryption in transit and at rest,
- secret management,
- secure session handling,
- environment separation,
- backups and recovery,
- vulnerability and dependency management,
- monitoring and alerting,
- audit trails,
- incident response,
- vendor/subprocessor review,
- secure deletion/retention policies.

## Breach and incident handling
If unauthorized access, use, disclosure, or acquisition of sensitive information is suspected:
1. preserve evidence,
2. contain access safely,
3. escalate to the designated privacy/security lead,
4. assess affected systems/data,
5. follow applicable breach-risk assessment and notification procedures,
6. document decisions and remediation.

Adaria itself should not make the final legal determination that an incident is or is not a reportable breach.

## California deployment
For California practices, deployment review must account for applicable California medical-information and consumer-privacy requirements in addition to federal law. Do not assume HIPAA preempts or replaces all California obligations.

## Public/demo data
Use synthetic data for demos, testing, sales materials, screenshots, and training examples unless a separately approved de-identification or authorization process applies.

Never copy historical patient conversations, names, phone numbers, photos, appointment details, balances, or health information into a public repository.

## Product claims
Allowed:
- "Designed with HIPAA privacy and security requirements in mind"
- "Supports privacy-first healthcare workflows"
- "Built for configurable role-based access and minimum-necessary data use"

Do not claim:
- "HIPAA certified"
- "guaranteed HIPAA compliant"
- "fully compliant with all healthcare privacy laws"
unless qualified counsel/security/compliance review supports the exact claim for the deployed product and context.

## Pre-production gate
Adaria must not process real PHI in production until the deployment has completed:
- data-flow inventory,
- role/covered-entity/business-associate analysis,
- required BAAs/vendor agreements,
- security risk assessment,
- access-control design,
- retention/deletion policy,
- incident-response plan,
- breach-notification process,
- staff/privacy training requirements,
- legal/privacy review for applicable federal and state law.

## North star
Personalization must never outrun permission.

The guest should feel deeply remembered in conversation while the system remembers only what it is actually allowed and necessary to retain.
