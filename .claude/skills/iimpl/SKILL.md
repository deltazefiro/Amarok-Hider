---
name: iimpl
description: "Use this when you are given a plan **file** and requires you to implement."
---

## Procedure

1. Update your todo list to the following procedure.
   - Todo list: `functions.update_plan` for codex, `TaskCreate` for claude code.
   - For codex only: Also use `create_goal` to create a goal with the same content as the todo list.
2. First spawn multiple explore and/or librarian subagents to gather context. Wait until them return.
3. Design tests or proof for the required verifications.
4. Go and implement the changes.
5. Run the verifications (E2E if specified).
6. Spawn two independent subagents to do the final review. One focuses on code quality and one focuses on correctness.
   - With review or oracle subagents (or codex:rescue if you are claude code)
   - Provide minimal context to reduce bias.
   - Ask the agent to check: code quality (consistency, clarity, etc.) and correctness (whether the changes aligned to plan and whether tests/logs/artifacts that are sufficient to prove the ACs / Verifications).
   - Fix valid issues. Consider balancing the extra complexity introduced and the necessity of the fix.
   - Repeat fix/review loop until remaining comments are invalid or not worth addressing.
7. Clean up unnecessary changes. Remove code you wrote for debugging and clean up the failed attempts.
8. Report a summary of the changes you made, the divergence from the plan, the path to the artifacts and the trouble you met (if any).

## Guidelines

- You are responsible to design proof for the Acceptance Criteria or Verifications. A proof can commonly be a test, but also can be E2E test logs, screenshots, artifacts, etc. When spawning the reviewer, prompt it to check whether the proof you provided are enough to prove the Acceptance Criteria / Verifications are satisfied as well as reviewing the code change.
- Every time you proceed / jump back from the procedure, update your todo list.