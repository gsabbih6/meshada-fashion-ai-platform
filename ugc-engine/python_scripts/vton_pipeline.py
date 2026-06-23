"""
DEPRECATED — This file is no longer used.

Virtual try-on is now handled by Higgsfield Soul (text-to-image)
in orchestrator.py using the official higgsfield_client SDK.

The old approach used Replicate's VTON model which required:
  - REPLICATE_API_TOKEN
  - Manual model base images
  - Separate API call per model

Higgsfield handles this in a single call with better consistency.
"""

# Kept for reference only. See orchestrator.py for the active pipeline.
