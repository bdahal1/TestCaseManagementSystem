package com.tcms.repositories;


import com.tcms.models.Tags;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagsRepository extends PagingAndSortingRepository<Tags, Integer>, JpaSpecificationExecutor<Tags> {

    Page<Tags> findAll(Pageable pageable);

    Tags findById(int tagId);

    Page<Tags> findByTagNameContaining(String tagName, Pageable pageable);

    void deleteById(int id);

    Tags save(Tags tags);
}
