---
name: rmslop
description: Clean up and remove the ai slops from the code.
disable-model-invocation: false
---

Clean up the code and focus on removing the AI slops. focus on the code related to your current task.

1. Clean up unnecessary changes.
    - Remove code you wrote for debugging.
    - Clean up the failed attempts.
2. Remove defensive programming and silent fallbacks.
    - Look through the condition expressions, try-except blocks and default values and ask yourself: is that defensive programming?
    - For example, do not use `qtype = row.get("question_type", "narration")` unless there is clear evidence that `question_type` may be missing. If missing data is expected in some cases, log an explicit warning instead of silently substituting a default, with a comment explaining the reason.
3. Write necessary comments to improve readability and remove the useless ones.
    - Good comments:
        - A workaround for a problem -> explain the problem and why the workaround is needed
        - Many tensor operations -> explain the shape of the tensors and perhaps with examples
        - The customized part -> more detailed explanation (e.g. an sft script with a custom loss function)
        - Magic or mysterious code, args or numbers -> explain the logic and the purpose
        - Dataset processing -> describe the format and the useful statistics of the dataset.
    - Useless comments:
        - The docstring for a function with excessive args description for those self-explanatory args.
        - Comment banners with `---` for separating sections of the code.