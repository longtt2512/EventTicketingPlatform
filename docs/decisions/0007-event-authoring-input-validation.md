# Event authoring input validation

Status: accepted (explicit user direction, 2026-11-21)

A draft may omit event schedule and other publication requirements, as specified
in decision 0001. When event start or end is supplied on a draft update, each must
be strictly in the future. When both are supplied, end must follow start. The
existing future-start check remains at review/publication to handle elapsed time.
Frontend validation guides correction and backend validation is authoritative.
Ticket sale start may be in the past. Existing sale-window and capacity rules
remain unchanged.

VND inputs show Vietnamese thousands grouping while submitting integer amounts.
Required markers distinguish information needed for review, without making
incomplete drafts unsaveable. Percentage controls and actual non-VND payment
records retain their correct units.
