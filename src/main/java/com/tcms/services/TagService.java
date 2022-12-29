package com.tcms.services;

import com.tcms.models.Tags;
import com.tcms.repositories.TagsRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TagService {
    final public TagsRepository tagsRepository;

    public TagService(TagsRepository tagsRepository) {
        this.tagsRepository = tagsRepository;
    }

    public Map<String, Object> getTagsListResponse(Page<Tags> tagsList) {
        Map<String, Object> response = new HashMap<>();
        response.put("tags", tagsList.getContent());
        response.put("currentPage", tagsList.getNumber());
        response.put("totalItems", tagsList.getTotalElements());
        response.put("totalPages", tagsList.getTotalPages());
        return response;
    }

    public Tags saveTags(Tags tags) {
        return tagsRepository.save(tags);
    }
}
