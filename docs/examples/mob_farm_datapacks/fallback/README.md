# Fallback test

Enable the `invalid` example beside any valid pack. The invalid descriptor must
produce one warning, while all vanilla targets and every valid descriptor remain
available. A failure of the complete rebuild must restore only the fixed vanilla
catalog rather than leaving a partial snapshot active.
