package com.gii.common.repository.course;

import com.gii.common.entity.course.MuxWebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MuxWebhookEventRepository extends JpaRepository<MuxWebhookEvent, UUID> {

  @Modifying
  @Query(
      value =
          """
          INSERT INTO mux_webhook_events (id, event_id, event_type, created_at, updated_at)
          VALUES (gen_random_uuid(), :eventId, :eventType, now(), now())
          ON CONFLICT (event_id) DO NOTHING
          """,
      nativeQuery = true)
  int claim(@Param("eventId") String eventId, @Param("eventType") String eventType);
}
