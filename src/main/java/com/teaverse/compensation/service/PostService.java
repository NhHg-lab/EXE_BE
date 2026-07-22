package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CommentRequest;
import com.teaverse.compensation.dto.request.CreatePostRequest;
import com.teaverse.compensation.dto.response.PostResponse;
import com.teaverse.compensation.exception.NotFoundException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.Post;
import com.teaverse.compensation.model.PostType;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.repository.PostRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public PostService(PostRepository postRepository, CurrentUserService currentUserService, DtoMapper mapper) {
        this.postRepository = postRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    public List<PostResponse> list(PostType type) {
        List<Post> posts = type == null ? postRepository.findAll() : postRepository.findByType(type);
        return posts.stream()
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(mapper::toPostResponse)
                .toList();
    }

    public PostResponse create(CreatePostRequest request) {
        User user = currentUserService.getCurrentUser();
        Post post = new Post();
        post.setAuthorId(user.getId());
        post.setAuthorName(user.getUsername());
        post.setType(request.type() == null ? PostType.HIGHLIGHT : request.type());
        post.setContent(request.content());
        post.setGame(request.game());
        post.setClanTag(request.clanTag());
        return mapper.toPostResponse(postRepository.save(post));
    }

    public PostResponse like(String id) {
        Post post = findPost(id);
        post.setLikes(post.getLikes() + 1);
        return mapper.toPostResponse(postRepository.save(post));
    }

    public PostResponse comment(String id, CommentRequest request) {
        User user = currentUserService.getCurrentUser();
        Post post = findPost(id);
        post.getComments().add(user.getUsername() + ": " + request.content());
        return mapper.toPostResponse(postRepository.save(post));
    }

    private Post findPost(String id) {
        return postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
    }
}
