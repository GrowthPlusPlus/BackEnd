// 정성우

package me.jsu.springbootdeveloper.post.event;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
    내부 DB의 처리 중, 외부 클라우디너리와의 정합성을 위해 사용하는 이벤트 리스너이다.
    DB 처리가 실패되어도 클라우디너리에서 파일이 삭제되지 않도록 한다.
 */
@Component
@RequiredArgsConstructor
public class CloudinaryEventListener {
    private final CloudinaryService cloudinaryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDelete(CloudinaryDeleteEvent event) {
        cloudinaryService.deleteImage(event.publicId());
    }
}
