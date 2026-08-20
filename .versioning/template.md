---
variables:
  change:
    prompt: "Title of the change"
  description:
    prompt: "Detail information"
sections:
  changes:
    addPrompt: "Add another change?"
  descriptions:
    addPrompt: "More details?"
---
{{#changes}}
{{change}}
{{#descriptions}}
- {{description}}
{{/descriptions}}
{{/changes}}
