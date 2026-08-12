# Adaria Lead & Revenue Intelligence Skill

## Purpose
Turn conversation activity into explainable lead priority, timely follow-up, and measurable revenue outcomes without using emotional vulnerability as sales leverage.

## Lead temperature
Maintain both:
- numeric score: 0-100
- label: HOT | WARM | COLD

Thresholds are configurable by practice.

Every score change must record reasons and source events.

## Scorable events
Examples of positive intent signals:
- asks to book a consultation
- states a near-term timeframe
- chooses or asks for a provider
- completes requested photo intake
- asks about financing, deposit, or payment logistics
- engages with a quote
- returns after prior inquiry
- responds to follow-up
- confirms availability

Examples of negative or cooling signals:
- outside service/location eligibility
- explicitly not interested
- long inactivity after multiple consented follow-ups
- requests a service the practice does not offer

Do not use as positive scoring signals:
- insecurity
- shame
- distress
- loneliness
- low self-esteem
- protected traits
- sensitive health facts unrelated to operational fit

## Score explanation
Staff must be able to see why a lead is hot/warm/cold.

Example:
`86 HOT`
- +20 consultation requested within 14 days
- +15 preferred surgeon identified
- +15 secure photos received
- +12 asked about deposit
- +12 responded to quote follow-up
- +12 returning engaged guest

## Next-best-action engine
Do not merely score. Recommend the next operational step:
- answer unresolved question
- send provider information
- send approved before/after examples
- request secure photos
- offer consultation times
- send financing information
- ask human for exception/clinical answer
- schedule quote follow-up
- stop follow-up when guest opts out

## Quote rescue
When a quote is created:
- create follow-up_due_at
- default to practice-configurable 24-48 hours unless guest preference differs
- store unresolved concerns
- use personalized follow-up based on the quote conversation
- do not repeatedly chase a guest who is disengaged or has opted out

## Campaign attribution
When a guest arrives from a promotion, ad, landing page, text blast, referral, or review/gallery page, preserve the source and campaign ID when available.

Report:
- inquiries
- qualified leads
- consults scheduled
- photo intake completion
- quote rate
- deposit/book rate
- conversion by campaign
- conversion by provider/service
- recovery from follow-up

## Patient coordinator priority queue
Surface a queue with:
- guest name
- lead temperature
- service interest
- provider preference
- time since last guest message
- unresolved question
- quote age
- next-best action
- escalation status

Prioritize responsiveness and guest need, not just dollar value.

## Conversation-to-outcome learning
When downstream outcomes are available, attach them to the conversation:
- consult booked
- consult attended
- quote issued
- deposit paid
- procedure/service booked
- canceled
- no-show
- no decision

Use outcomes to evaluate scoring rules and workflow effectiveness. Do not silently let the model redefine scoring logic without review.

## Quality metrics
Track:
- first response time
- unanswered message age
- handoff latency
- consultation booking rate
- quote follow-up completion
- recovered lead rate
- opt-out rate
- scheduling error rate
- human-edit rate on AI drafts
- conversion by source/service/provider

Do not optimize only for revenue. Include guest satisfaction/service recovery and safety escalations in quality review.
