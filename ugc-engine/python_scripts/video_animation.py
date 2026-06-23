"""
DEPRECATED — This file is no longer used.

Image-to-video animation and lip sync are now handled by Higgsfield DoP
in orchestrator.py using the official higgsfield_client SDK.

The old approach required:
  - Replicate API (Stable Video Diffusion) for image animation
  - SyncLabs API for lip sync
  - Two separate API calls and credential sets

Higgsfield DoP handles image-to-video in a single call with
cinematic camera controls built in.
"""

# Kept for reference only. See orchestrator.py for the active pipeline.
