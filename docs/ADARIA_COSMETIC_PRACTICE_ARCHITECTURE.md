# Adaria Cosmetic Practice Architecture

## Product thesis
Adaria is an AI concierge for high-consideration cosmetic and elective-care practices. It should not behave like a generic chatbot. It should combine fast response, humanized conversation, structured qualification, approved education, operational automation, and human judgment.

The first vertical is cosmetic surgery / medspa because conversations often combine emotional goals, clinical questions, logistics, pricing, provider preference, media, scheduling, quotes, and follow-up.

## Core operating model
Every inbound conversation is processed through five layers:

1. **Identity and context**
   - Preferred name
   - Language
   - New vs returning guest
   - Existing patient record when an authorized integration is available
   - Current provider, prior services, prior quotes, upcoming appointments, and communication preferences when permitted

2. **Intent routing**
   Classify each turn into one or more operational intents:
   - procedure/service inquiry
   - pricing / quote clarification
   - offer / promotion inquiry
   - provider / surgeon selection
   - scheduling
   - rescheduling / cancellation
   - directions / hours / contact information
   - pre-procedure preparation
   - post-procedure recovery / downtime
   - medication question
   - insurance / financing / payment
   - secure photo intake
   - before-and-after gallery request
   - testimonial / review request
   - existing-patient follow-up
   - complaint / service recovery
   - clinical or safety escalation

3. **Knowledge and permissions**
   Answer only from the correct source and at the correct permission level.
   - General office facts: approved practice profile
   - Pricing: current structured pricing or approved ranges
   - Offers: active campaign database with terms and expiration
   - Providers: verified provider profile and practice-authored capability tags
   - Procedure education: clinician-approved knowledge base
   - Prep/recovery: clinician-approved instructions tied to the exact service
   - Medication: exact approved protocol or human escalation; never improvise individualized medication advice
   - Scheduling: live scheduling connector when available
   - Patient-specific history: authorized patient-system connector only
   - Payments: secure payment link / POS connector, never raw card capture inside normal chat

4. **Relational intelligence**
   Apply the Adaria Human Connection Skill:
   - use guest name naturally
   - reflective listening
   - one meaningful open-ended question at a time
   - explore visual goal and desired emotional outcome when appropriate
   - preserve humor, pacing, and continuity
   - never exploit insecurity

5. **Action and next best step**
   Resolve the request, continue discovery, book, send secure upload/payment link, nurture, or request human input.

## Conversation state machine
Suggested states:

- `NEW_INQUIRY`
- `DISCOVERY`
- `EDUCATION`
- `PROVIDER_MATCH`
- `PRICE_AND_FINANCING`
- `PHOTO_INTAKE`
- `CONSULT_READY`
- `CONSULT_SCHEDULED`
- `QUOTE_SENT`
- `QUOTE_FOLLOWUP`
- `DEPOSIT_PENDING`
- `BOOKED`
- `PRE_OP`
- `POST_OP`
- `RETURNING_PATIENT`
- `HUMAN_REVIEW`
- `SERVICE_RECOVERY`
- `CLOSED`

Transitions should be driven by events, not just free-form model judgment.

## First-party data objects
### Guest
- guest_id
- preferred_name
- language / locale
- contact channels and consent state
- new_or_returning
- preferred_provider
- referral_source
- communication_style

### Lead profile
- service_interest
- visual_goal
- desired_feeling
- personal_meaning
- concerns / objections
- timing
- budget / financing interest only when voluntarily supplied or operationally appropriate
- prior_procedure_status when verified or self-reported
- provider_preference
- referral_context
- lead_score
- lead_temperature
- score_reasons
- next_best_action

### Provider profile
- provider_id
- name
- credentials from verified practice source
- procedures offered
- practice-authored strengths / niches
- consultation modalities
- locations
- gallery tags
- approved biography

Do not infer or invent provider superiority. Recommendations should explain the fit criteria and can offer multiple appropriate providers.

### Procedure profile
- procedure_id
- approved names / synonyms
- high-level description
- expected appointment pathway
- current price / approved range
- financing eligibility
- typical downtime information from approved clinical source
- prep instructions
- recovery instructions
- escalation triggers
- related procedures
- gallery tags
- testimonial tags

### Campaign / offer
- campaign_id
- offer_name
- service(s)
- eligibility
- valid_from / valid_until
- locations / providers if restricted
- exact terms
- response script guidance

### Appointment
- appointment_id
- type
- provider
- coordinator
- date/time/timezone
- location or video modality
- confirmation state
- reminder state
- reschedule/cancel policy

### Quote
- quote_id
- guest_id
- services
- provider
- amount / range
- created_at
- expiration if applicable
- deposit requirement
- follow_up_due_at
- status

### Media asset
- asset_id
- media_type: before_after | testimonial | education | instruction
- procedure tags
- provider tags
- concern / goal tags
- consent / publication status
- approved audience
- source

## High-value skills
### 1. Procedure Discovery
Ask what the guest is considering, what concerns them, what they envision, whether they have had the procedure before, and whether they have a preferred or referred provider.

### 2. Provider Match
Ask whether a surgeon/provider was recommended, what drew the guest to that person, and what matters most in choosing a provider. Match only against verified practice-authored attributes.

### 3. Returning Patient Recognition
When an authorized patient integration confirms prior history, acknowledge it naturally. Do not ask the guest to repeat known information. Use prior service history to personalize the next conversation without overstepping into clinical advice.

### 4. Procedure Education
Answer routine questions about process, typical downtime, what to expect, preparation, and recovery only from approved content. Clearly distinguish general information from patient-specific medical instructions.

### 5. Medication Safety Router
Questions about prescriptions, stopping medications, dosing, anesthesia instructions, or individualized medication use are high-risk. Use exact approved instructions when they match the patient/service context; otherwise request human/clinical review.

### 6. Scheduling Guardian
For every scheduled or rescheduled appointment, confirm:
- date
- time
- timezone
- provider / coordinator
- location or video modality
- working join link when applicable
- any prerequisites

Scheduling ambiguity should trigger verification instead of confident guessing.

### 7. Offer Interpreter
Recognize the campaign that generated the inquiry, retrieve current terms, answer questions, verify eligibility, and avoid applying expired or incompatible offers.

### 8. Secure Photo Intake
When photos are required before consultation, explain exactly what views are needed using practice-approved instructions, issue a secure upload request, track completion, and route to the correct patient coordinator/provider workflow. Never treat ordinary chat attachments as the default clinical record.

### 9. Visual Results Matchmaker
Retrieve consent-cleared before-and-after cases by procedure, provider, and guest-stated concern/goal. Do not use facial recognition or infer protected/sensitive traits. The guest chooses what they want to see.

### 10. Testimonial Matchmaker
Retrieve approved testimonials relevant to the guest's concern or decision barrier, such as natural-looking results, recovery experience, staff experience, or financing, without fabricating or cherry-picking claims beyond what the testimonial supports.

### 11. Quote Follow-Up / Lead Rescue
A quote should create a follow-up event. Initial default window for this vertical: configurable 24-48 hours after quote unless the guest chose another timeline. Follow-up should reference the actual unresolved question or motivation rather than send a generic "just checking in" message.

### 12. Human Whisper
When Adaria needs judgment, privately send staff:
- guest name
- current intent
- concise conversation summary
- lead temperature and reasons
- stated goal and desired feeling when relevant
- main objection / unresolved question
- suggested response
- reason for escalation
- actions: Approve | Edit | Take Over

### 13. Patient Coordinator Handoff
A guest becomes `CONSULT_READY` when there is meaningful consultation intent. Handoff packet should include service interest, provider preference, referral source, timing, photo status, price/financing discussion, objections, emotional outcome in the guest's own frame, and complete conversation summary.

### 14. Payment / Deposit Concierge
Support secure deposits, invoices, receipts, and product purchases through an approved payment connector. Chat should never request or store full card data. Payment disputes or ambiguous balances go to a human.

### 15. Directions / Hours / Access
Handle basic calls-to-text questions such as directions, parking, office hours, fax, email, and arrival instructions immediately from verified practice data.

## Lead intelligence
Lead scoring is explainable and event-based.

Potential positive signals:
- asks about consultation availability
- short stated timeframe
- sends requested photos
- confirms provider preference
- asks about deposit / financing
- engages after quote
- returns to continue the same decision

Neutral context signals:
- emotional disclosure
- body-image concern
- language
- demographic traits

Adaria must never increase lead score merely because a guest expresses insecurity, distress, low self-esteem, or vulnerability.

Example score explanation:
`82 / HOT`
- +20 wants consultation within 2 weeks
- +15 requested provider availability
- +15 completed photo intake
- +12 asked about deposit
- +10 responded to quote follow-up
- +10 strong service fit

## Multilingual layer
Initial target languages:
- English
- Spanish
- Tagalog
- Vietnamese
- Chinese text support with Simplified / Traditional output and Mandarin-first conversational design

Every language pack should include:
- human-reviewed terminology
- locale/formality rules
- brand voice examples
- medical/cosmetic terminology glossary
- translation confidence threshold
- escalation phrases
- English staff summaries

## Humanization requirements
The AI should never sound like a form.
- Use names naturally.
- Reflect what the guest said before redirecting.
- Ask one open-ended question at a time.
- Do not immediately push scheduling after every factual question.
- Remember prior answers.
- Adapt length to the guest.
- Use humor only when invited.
- Preserve the same voice after human-assisted replies.

## Safety and privacy architecture
Before production with real patient data:
- use role-based access and least privilege
- segregate development/test data from real patient data
- maintain auditable records of AI and human actions
- use approved secure storage and transport for sensitive information
- avoid putting secrets, payment credentials, or unnecessary patient data into prompts
- do not use real patient conversations or images as model-training material without an explicit approved data-governance process
- require human/clinical review for individualized medical judgment

The old Podium screenshots and historical patient examples should be treated as design references, not copied into a public demo dataset.

## MVP order
### Phase 1
- web/SMS concierge
- human connection
- intent router
- procedure/office knowledge base
- provider profiles
- lead scoring
- human whisper
- scheduling handoff

### Phase 2
- live calendar / CRM integration
- quote follow-up
- offers
- multilingual packs
- secure photo intake
- before/after and testimonial retrieval

### Phase 3
- authorized patient-history integration
- payment/deposit connector
- pre-op/post-op workflows
- conversion feedback loop
- practice analytics

## North-star metric
Adaria should optimize for **comfortable progress to the next appropriate decision**, not maximum messages, maximum emotional disclosure, or maximum sales pressure.
