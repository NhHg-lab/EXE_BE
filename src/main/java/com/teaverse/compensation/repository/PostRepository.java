package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Post;
import com.teaverse.compensation.model.PostType;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByType(PostType type);

    List<Post> findByAuthorId(String authorId);
}
