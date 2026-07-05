package com.jk.amazon2.posting.event;

import java.time.LocalDate;

public record StatisticsUpdateEvent(Long memberId, LocalDate weekStartDate) {}
