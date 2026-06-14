---
name: handoff
description: End the current task. Clean up, review, update docs and commit.
disable-model-invocation: false
---

Clean up the code and focus on removing the AI slops. focus on the code related to your current task.

1. Clean up unnecessary changes.
    - Remove code you wrote for debugging.
    - Clean up the failed attempts. 
    - Clean up defensive programming and silent fallbacks.
2. Update the documentation.
   - Update related documents in `docs`. Be concise.
   - Write necessary comments to improve readability and remove the useless ones.
     - Good comments:
         - A workaround for a problem → explain the problem and why the workaround is needed.
         - The customized part → more detailed explanation.
         - Magic or mysterious code, args or numbers → explain the logic and the purpose.
     - Useless comments:
         - The docstring for a function with excessive args description for those self-explanatory args.
         - Comment banners such as `---` for separating sections of the code.
3. Commit the changes. 
   - Commit only your changes, do not commit the changes of other agents.
   - Note we have a pre-commit hook to format the code.