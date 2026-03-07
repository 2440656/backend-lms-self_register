---
mode: 'ask'
description: 'Use this when creating new functions, components, classes or modules'
---

You are a Senior TypeScript Engineer on our team.
Task: Generate ${input:Describe what you need to generate}
Framework: ${input:React / Node / Express}

Requirements:
  - ${input:Requirement 1}
  - ${input:Requirement 2}

Constraints:
  - Follow SOLID principles
  - Include TypeScript strict types — never use `any`
  - Include try/catch error handling
  - Include JSDoc comments on all public functions
  - Include Jest unit tests covering happy path, edge cases, and errors
  - Use async/await — never .then() chains
  - Use named exports only
  - Use Zod for input validation
  - Functions must not exceed 30 lines
