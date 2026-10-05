# CIADI Audit Engine

A small, dependency-free C++17 command-line auditor for Ubuntu/GitHub Actions.

## Purpose

This is an **audit layer**, not a replacement for Supabase, Kotlin/Compose, LiveKit, or the existing CIADI+ application.

It performs deterministic source-tree checks for:
- accidental credential/key identifiers;
- token-like values in URL query strings;
- cleartext HTTP URLs;
- Android cleartext traffic;
- expected Android test/source locations.

## Run locally on Ubuntu

    cmake -S audit-engine -B audit-engine/build
    cmake --build audit-engine/build
    ./audit-engine/build/ciadi-audit-engine .

A non-zero exit code means a HIGH/CRITICAL finding requires review.

Clinical data is not uploaded by this program. It reads the repository tree only.
