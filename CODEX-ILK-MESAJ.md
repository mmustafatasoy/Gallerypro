You are continuing the Gallerypro Android app (Kotlin + Jetpack Compose) in this repository.

Before doing anything, read AGENTS.md and DEVAM-NOTU.md from start to finish. They are written in Turkish; they are the single source of truth. Follow only the plan in them and do not change anything outside the task.

Rules (summary; details in the notes):
- Reply to me in Turkish. Commit messages in Turkish. Every on-screen text goes to res/values (Turkish) and res/values-en (English); no hardcoded strings.
- Compose foundation only (no Material3). Glass only via Modifier.glass, colors/sizes only from designsystem/Tokens.kt.
- Never add the INTERNET permission. No new libraries and no version upgrades without asking me first.
- Do not touch the CLIP model files, scripts/models/ or the GitHub workflows unless the task requires it.
- Clamp animated Dp/Float values before padding/size/alpha (coerceAtLeast(0.dp), coerceIn(0f, 1f)).
- You probably cannot build Android locally; GitHub Actions builds and tests every push. A task is done only when Actions is green.

Workflow for each task:
1. Create a new branch named asama-…
2. Implement the task, add unit tests for pure logic in app/src/test/.
3. Push, wait for GitHub Actions to be green, fix only the failing lines if it is red.
4. Update DEVAM-NOTU.md (status table, what was done, what was postponed).
5. Open a pull request to main. Do not merge; I merge it myself.

Next task: "5b-2 Benzer Fotoğraflar" (Similar Photos), described in DEVAM-NOTU.md under "Hemen yapılacak", item 3.

If a decision is not covered by the notes (license, new library, design change), stop, give me two options in Turkish and wait for my answer.
