---
description: Execute the implementation plan by processing and executing all tasks defined in tasks.md
---

## User Input

$ARGUMENTS

You **MUST** consider the user input before proceeding (if not empty).

## Outline

1. **Parse user input for feature number**:
   - Check if user provided a feature number (e.g., "7", "implement feature 5", "feature number 3")
   - Extract the numeric value if present
   - If no feature number provided, set it to empty

2. **Run prerequisite check with feature detection**:
   - If feature number was provided run: `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks --feature-number NUMBER`
   - If no feature number provided run: `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`
   - Parse the JSON response to get FEATURE_DIR, AVAILABLE_DOCS, NEEDS_CONFIRMATION, and DETECTED_FEATURE
   - All paths must be absolute

3. **Handle feature confirmation if needed**:
   - If NEEDS_CONFIRMATION is true AND user did NOT explicitly provide a feature number in step 1:
     - Extract feature number and name from DETECTED_FEATURE (e.g., "007-action-sleep-scheduling")
     - Check if the feature has a COMPLETE.md file (feature is already complete)
     - If complete: Display "Feature NUMBER (NAME) is already marked as complete. Do you want to re-implement it?"
     - If not complete: Display "Auto-detected incomplete feature: NUMBER - NAME. Do you want to start implementing this feature?"
     - Wait for user confirmation (yes/no)
     - If user says "no" or "cancel" or "stop", halt execution
     - If user says "yes" or "proceed" or "continue", proceed to step 4
   - Otherwise (user explicitly provided feature number), proceed directly to step 4

4. **Check checklists status** (if FEATURE_DIR/checklists/ exists):
   - Scan all checklist files in the checklists/ directory
   - For each checklist, count total items, completed items, and incomplete items
   - Create a status table showing the checklist status
   - Calculate overall status: PASS if all complete, FAIL if any incomplete
   - If any checklist is incomplete, ask user for confirmation to proceed
   - If all checklists are complete, automatically proceed to step 5

5. Load and analyze the implementation context:
   - **REQUIRED**: Read tasks.md for the complete task list and execution plan
   - **REQUIRED**: Read plan.md for tech stack, architecture, and file structure
   - **IF EXISTS**: Read data-model.md for entities and relationships
   - **IF EXISTS**: Read contracts/ for API specifications and test requirements
   - **IF EXISTS**: Read research.md for technical decisions and constraints
   - **IF EXISTS**: Read quickstart.md for integration scenarios

6. **Project Setup Verification**:
   - Create/verify ignore files based on actual project setup
   - Check for git repository, Docker, ESLint, Prettier, etc.
   - Append missing critical patterns only to existing files
   - Create full pattern sets for missing ignore files

7. Parse tasks.md structure and extract task phases, dependencies, details, and execution flow

8. Execute implementation following the task plan with phase-by-phase execution

9. Implementation execution rules: Setup first, Tests before code, Core development, Integration work, Polish and validation

10. Progress tracking and error handling: Report progress, halt on failures, mark completed tasks as [X] in tasks file

11. Completion validation: Verify all tasks completed, features match spec, tests pass, report final status

Note: This command assumes a complete task breakdown exists in tasks.md. If tasks are incomplete or missing, suggest running `/speckit.tasks` first to regenerate the task list.
