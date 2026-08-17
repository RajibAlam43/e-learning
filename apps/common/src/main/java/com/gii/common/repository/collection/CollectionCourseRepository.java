package com.gii.common.repository.collection;

import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionCourseRepository
    extends JpaRepository<CollectionCourse, CollectionCourseId> {

  List<CollectionCourse> findByCollection_IdOrderByPositionAsc(UUID collectionId);

  List<CollectionCourse> findByCollection_IdIn(List<UUID> collectionIds);

  @Query(
      """
      SELECT cc
      FROM CollectionCourse cc
      JOIN FETCH cc.collection
      JOIN FETCH cc.course
      WHERE cc.collection.id = :collectionId
      ORDER BY cc.position ASC
      """)
  List<CollectionCourse> findByCollection_IdOrderByPositionAscWithCourse(
      @Param("collectionId") UUID collectionId);

  @Query(
      """
      SELECT cc
      FROM CollectionCourse cc
      JOIN FETCH cc.collection
      JOIN FETCH cc.course
      WHERE cc.collection.id IN :collectionIds
      """)
  List<CollectionCourse> findByCollection_IdInWithCourse(
      @Param("collectionIds") List<UUID> collectionIds);

  @Query(
      """
      SELECT cc
      FROM CollectionCourse cc
      JOIN FETCH cc.collection
      JOIN FETCH cc.course
      WHERE cc.course.id IN :courseIds
      """)
  List<CollectionCourse> findByCourse_IdInWithCollectionAndCourse(
      @Param("courseIds") List<UUID> courseIds);

  @Query(
      """
      SELECT cc
      FROM CollectionCourse cc
      JOIN FETCH cc.collection
      JOIN FETCH cc.course c
      WHERE cc.collection.id = :collectionId
      AND c.status = :status
      ORDER BY cc.position ASC
      """)
  List<CollectionCourse> findByCollection_IdOrderByPositionAscWithCourseStatus(
      @Param("collectionId") UUID collectionId, @Param("status") PublishStatus status);

  @Query(
      """
      SELECT cc
      FROM CollectionCourse cc
      JOIN FETCH cc.collection
      JOIN FETCH cc.course c
      WHERE cc.collection.id IN :collectionIds
      AND c.status = :status
      """)
  List<CollectionCourse> findByCollection_IdInWithCourseStatus(
      @Param("collectionIds") List<UUID> collectionIds, @Param("status") PublishStatus status);
}
