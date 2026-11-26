package com.gii.api.service.live;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.live.LiveClass;
import com.gii.common.enums.LiveClassProvider;
import org.junit.jupiter.api.Test;

class LiveClassHostUrlTest {
  @Test
  void existingGoogleClassesReturnMeetLinkInsteadOfCalendarPage() {
    LiveClass meeting =
        LiveClass.builder()
            .provider(LiveClassProvider.GOOGLE_MEET)
            .hostStartUrl("https://calendar.google.com/calendar/event?eid=calendar-event")
            .participantJoinUrl("https://meet.google.com/abc-defg-hij")
            .build();
    assertThat(meeting.effectiveHostStartUrl()).isEqualTo(meeting.effectiveParticipantJoinUrl());
  }

  @Test
  void zoomKeepsItsSeparateHostLink() {
    LiveClass meeting =
        LiveClass.builder()
            .provider(LiveClassProvider.ZOOM)
            .hostStartUrl("https://zoom.us/s/123?zak=host-token")
            .participantJoinUrl("https://zoom.us/j/123")
            .build();
    assertThat(meeting.effectiveHostStartUrl()).isEqualTo(meeting.getHostStartUrl());
  }
}
