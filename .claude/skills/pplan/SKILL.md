---
name: pplan
description: "Use this when user explicitly asks for a plan **file**. Do not use if the user only asks for a plan."
---
Plan for the given task. Do not enter the plan mode with tools.

## Procedure

1. First fan out explore subagents (>=3) to gather context.
2. If the task is unclear, ask the user for a quick clarification.
3. Create a plan file `.plans/<feature_name>-<topic>-<date>.md` and write your plan concisely. Do not include implementation procedures (steps, phases, list of files to change, etc.). Focus on verifications and conceptual approach.
4. Use reviewer/oracle subagent (or codex:rescue if you are claude code) with minimal clean context to independently review the plan file. Revise the plan based on the feedback.
5. Ask the user if the plan is good. User might add `CMT`s in the file to annotate the plan. Discuss with the user and resolve them (be honest about the user's incorrect/inappropriate suggestions and discuss with them). When a CMT asserts a fact, verify it before propagating through the plan — user CMTs can carry mistakes. Remember resolving a CMT perhaps require you to revise the full plan instead of just where the CMT is. Iterate until the plan is good.
6. Once the user is satisfied with the plan, grill the user about the details of the implementation with ask user tool. This is to ensure the user has thought through the implementation and there are no major gaps in the plan.

## Plan File Structure

Write the plan in logseq draft style (multi-level nested bullet points with short sentences).

Section order:

- Goal: a brief description of the motivation and the idea.
- Conceptual Approach: the design the implementer needs. Keep it clear and conceptual.
- Verifications: high-level acceptance criteria. Describe the desired state, not detailed assertions.
- Scope: Can/May use / Cannot use. Keep concise; avoid restating the implementation checklist.
- Implementation Notes: only non-obvious gotchas: cross-component contracts, easy-to-get-wrong wiring, inherited defaults that bake in past failures
- References

## Guidelines

- Explore the current structure before proposing changes. Follow existing patterns.
- Scale each section to its complexity: a few sentences if straightforward, up to 200-300 words if nuanced.
- Keep the plan concise. Conceptual approach only instead of code implementation. A plan file should be no more than 200 lines (unless specified).
- Verifications describe observable proof, not how to check it. Prefer “dataset inspection artifact exists and matches contracts” over line-by-line assertions.
- The user's CMT can be questions. Answer the questions.