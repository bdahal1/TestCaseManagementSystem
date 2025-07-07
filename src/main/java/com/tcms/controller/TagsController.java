package com.tcms.controller;

import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Tags;
import com.tcms.repositories.TagsRepository;
import com.tcms.services.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController()
@CrossOrigin()
@RequestMapping("/tags")
public class TagsController {
    private final String defaultSize = "1000";
    private final TagService tagService;
    private final TagsRepository tagsRepository;

    public TagsController(TagService tagService, TagsRepository tagsRepository) {
        this.tagService = tagService;
        this.tagsRepository = tagsRepository;
    }

    @GetMapping("")
    public ResponseEntity<Object> getTags(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<Tags> tagList = tagsRepository.findAll(paging);
        if (tagList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(tagService.getTagsListResponse(tagList));
    }

    @GetMapping(path = "/name/{tagName}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getTagsByTagName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String tagName) {
        Pageable paging = PageRequest.of(page, size);
        Page<Tags> tagsList = tagsRepository.findByTagNameContaining(tagName, paging);
        if (tagsList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(tagService.getTagsListResponse(tagsList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getTagById(@PathVariable int id) {
        Tags tag = tagsRepository.findById(id);
        if (tag == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(tag);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTag(@RequestBody Tags tag) {
        if (tag == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag Info Not Found inside body.\n");
        }
        try {
            tagService.saveTags(tag);
            return ResponseEntity.status(HttpStatus.OK).body(tag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{tagId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTag(@RequestBody Tags tags, @PathVariable int tagId) {
        if (tags == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag Info Not Found inside body.\n");
        }
        try {
            Tags tag = tagsRepository.findById(tagId);
            if (tag == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag Not Found in database.\n");
            }
            tag.setTagName(tags.getTagName() == null ? tag.getTagName() : tags.getTagName());
            tagService.saveTags(tag);
            return ResponseEntity.status(HttpStatus.OK).body(tag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{tagId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteTag(@PathVariable String tagId) {
        if (tagId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("tagId not found for delete operation.\n");
        }
        Tags tags = tagsRepository.findById(Integer.parseInt(tagId));
        if (tags == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided tag id not found for Delete operation!"));
        }
        tagsRepository.deleteById(Integer.parseInt(tagId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Tag Deleted Successfully!"));
    }
}
