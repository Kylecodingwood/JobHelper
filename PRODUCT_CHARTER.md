# Job Assistant — Product Charter

**Status:** Foundational agreement  
**Last updated:** 2026-07-30  
**Owner:** Kyle

## Purpose

Job Assistant is a private, local-first career intelligence system. It helps its owner discover relevant opportunities without missing them, decide which are worth pursuing, prepare high-quality applications, and learn from results over time.

The project must be useful in the owner's real job search *and* serve as a credible, explainable portfolio project.

## Non-negotiable principles

### 1. Multi-source by design

No critical feature may depend on one platform alone. The system must use provider adapters and a common internal job model so that sources can be added, replaced, or disabled independently.

Initial sources may include public APIs, direct job URLs and owner-enabled scraping adapters. LinkedIn is an optional supplemental input, never the system's foundation; each source must be independently disableable and expose failures.

### 2. Code before paid AI

Use deterministic code whenever it can solve the problem: polling, scheduling, parsing, normalization, deduplication, filtering, caching, scoring rules, and notifications.

AI is reserved for work that benefits materially from reasoning or writing: ambiguous job-fit analysis, tailored application drafts, synthesis of evidence, and reflective career strategy. Every AI operation should be cached, observable, optional, and budget-limited.

### 3. Personal-first and private by default

This is a single-user system, built to improve Kyle's own search rather than to become a public SaaS product. Personal data, resumes, applications, preferences, and feedback remain under the user's control. The design should work locally and minimize external data sharing.

Commercial viability is a quality bar, not an immediate business goal: the product should solve a real problem well enough that it could plausibly be offered to others after deliberate policy, security, and product work.

### 4. Learning is a first-class deliverable

The project must teach a complete modern software-development workflow: product discovery, requirements, UX, architecture, data modeling, implementation, testing, observability, deployment, security, documentation, and iteration.

Kyle remains the decision-maker and learns to collaborate with AI as an engineering partner: specify outcomes, review designs, inspect trade-offs, test behavior, and own the final work. AI assistance must not turn the project into opaque generated code.

### 5. Depth over speed

The project may evolve over an extended period. We prefer a well-designed, layered system with meaningful technical challenges over a rushed demo. Scope will grow through deliberate milestones, each leaving a working, documented product increment.

### 6. Real-problem standard

Every major feature must answer a genuine user problem, define success criteria, and withstand skeptical review. The system should be demonstrable with realistic data, explain its decisions, handle failure cases, and have a credible path from prototype to reliable product.

## Product boundaries

- Scraping adapters may run only when explicitly enabled by the owner with recorded access/ToS risk; never automate login, bypass captcha/access controls, or submit applications.
- Do not automatically submit job applications or send outreach without explicit human confirmation.
- Do not present peer anecdotes, model outputs, or match scores as objective career facts.
- Do not use private user data to train models or expose it in a public demo repository.

## Decision test

Before accepting a feature or technical choice, ask:

1. Does it help the owner's job search in a concrete way?
2. Is it source-agnostic or does it create avoidable platform lock-in?
3. Could deterministic code do it more cheaply and reliably than AI?
4. What will Kyle learn by designing, building, and reviewing it?
5. Could we explain and defend the choice in a product or engineering interview?

If the answer to several questions is no, the feature should be redesigned, deferred, or rejected.

## Working definition of success

Within a sustained search, the system should reliably turn raw opportunities into a small, prioritized set of defensible actions; reduce repetitive effort; preserve the evidence behind decisions; and show measurable improvement through the user's feedback and outcomes.
