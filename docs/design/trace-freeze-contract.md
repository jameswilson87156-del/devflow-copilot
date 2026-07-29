# Trace Freeze Design Contract

- Preserve Layered Graphite, App Shell, Compact Run Ledger, Evidence Stream, and Inspector.
- Desktop Inspector is a resident complementary column, not a modal.
- Tablet/Mobile Inspector is the only drawer/dialog mode and owns Escape close, focus trap, and background inert.
- Event rows show only order, step type, step name, summary, status, latency, and compact Tool count when present.
- Provider and Model values remain in the header; direct source fields are disclosed in Inspector.
- Review reason is read from a real `HUMAN_REVIEW` AgentStep summary when available.
- QA Fixture screenshots remain test evidence only; final freeze screenshots use real local API + temporary H2.
