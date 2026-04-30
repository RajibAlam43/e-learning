package com.gii.common.repository.support;

import com.gii.common.entity.support.Faq;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

  List<Faq> findByIsPublishedTrueOrderByPositionAsc();
}
