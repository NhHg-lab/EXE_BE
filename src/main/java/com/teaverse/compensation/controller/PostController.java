package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.CommentRequest;
import com.teaverse.compensation.dto.request.CreatePostRequest;
import com.teaverse.compensation.dto.response.PostResponse;
import com.teaverse.compensation.model.PostType;
import com.teaverse.compensation.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ApiResponse<List<PostResponse>> list(@RequestParam(required = false) PostType type) {
        return ApiResponse.ok(postService.list(type));
    }

    @PostMapping
    public ApiResponse<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok("Post created", postService.create(request));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<PostResponse> like(@PathVariable String id) {
        return ApiResponse.ok("Post liked", postService.like(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<PostResponse> comment(@PathVariable String id, @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok("Comment added", postService.comment(id, request));
    }
}
