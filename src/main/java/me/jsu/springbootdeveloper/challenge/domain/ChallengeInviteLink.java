// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.InviteLinkType;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/*
    챌린지에 친구를 초대하기 위한 링크를 저장하는 도메인 클래스이다.
    초대 링크마다 다른 값은 inviteCode이므로 해당 값만 저장한다.
    한 유저가 동일한 챌린지에 대해 여러 초대 링크를 생성할 수 있다.
    
    확장성:
    - SINGLE_USE: 한 명이 사용하면 즉시 만료
    - TIME_BASED: 일정 시간이 지나면 만료
    - MULTI_USE: 최대 사용 횟수까지 여러 명이 사용 가능 (maxUses가 null이면 무제한)
 */
@Table(
        name = "challenge_invite_link",
        uniqueConstraints = {
                @UniqueConstraint(  // 초대 코드는 반드시 하나만 존재한다.
                        name = "uk_invite_code",
                        columnNames = {"invite_code"}
                )
        },
        indexes = {
                @Index(  // 특정 유저가 생성한 모든 초대 링크를 조회하기 위한 인덱싱이다.
                        name = "idx_user_id_link",
                        columnList = "user_id"
                ),
                @Index(  // 특정 유저가 생성한 유효한 모든 초대 링크를 조회하기 위한 인덱싱이다.
                        name = "idx_user_active",
                        columnList = "user_id, active"
                ),
                @Index(  // 특정 챌린지의 유효한 링크를 조회하기 위한 인덱싱이다.
                        name = "idx_challenge_active",
                        columnList = "challenge_id, active"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeInviteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 같은 챌린지에 여러 초대 링크를 생성할 수 있다.
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)  // 같은 사람이 여러 초대 링크를 생성할 수 있다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;  // 랜덤으로 생성된 초대 코드이다. 8자 길이로 제한되어 있으며, 우리가 줄 링크는 "초대링크 + inviteCode"로 구성된다.

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    private InviteLinkType linkType;  // 링크의 타입이다. 만료 조건을 결정한다.

    @Column(name = "active", nullable = false)
    private boolean active;  // 현재 해당 링크가 사용 가능한지 알려준다.

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // 링크의 만료 시간이다. TIME_BASED 타입의 경우 이 시간이 지나면 만료된다.

    @Column(name = "max_uses")
    private Integer maxUses;  // 최대 사용 횟수이다. MULTI_USE 타입의 경우 이 횟수까지 사용 가능하다. null이면 무제한이다.

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;  // 현재 사용 횟수이다. 링크를 통해 챌린지에 참여한 사람의 수를 나타낸다.

    @Column(name = "used_at")
    private LocalDateTime usedAt;  // 마지막 사용 시간이다. SINGLE_USE 타입의 경우 이 시간이 기록된다.

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChallengeInviteLink(Challenge challenge, User user, String inviteCode, 
                              InviteLinkType linkType, LocalDateTime expiresAt, Integer maxUses) {
        this.challenge = challenge;
        this.user = user;
        this.inviteCode = inviteCode;
        this.linkType = linkType;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.active = true;
        this.usedCount = 0;
    }

    /*
        링크가 만료되었는지 확인한다.
        만료 조건:
        1. active가 false인 경우
        2. TIME_BASED 타입이고 expiresAt이 지난 경우
        3. SINGLE_USE 타입이고 usedCount가 1 이상인 경우
        4. MULTI_USE 타입이고 maxUses가 null이 아니며 usedCount >= maxUses인 경우
        
        @return 만료되었으면 true, 유효하면 false
     */
    public boolean isExpired() {
        // 비활성화된 링크는 만료된 것으로 간주한다.
        if (!active) {
            return true;
        }

        // 시간 기반 만료 체크
        if (linkType == InviteLinkType.TIME_BASED) {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        // 1회용 링크는 한 번 사용되면 만료된다.
        if (linkType == InviteLinkType.SINGLE_USE) {
            return usedCount >= 1;
        }

        // 다중 사용 링크는 최대 사용 횟수를 초과하면 만료된다.
        if (linkType == InviteLinkType.MULTI_USE) {
            if (maxUses != null && usedCount >= maxUses) {
                return true;
            }
            // maxUses가 null이면 무제한 사용 가능하므로 시간 기반 만료만 체크한다.
            return LocalDateTime.now().isAfter(expiresAt);
        }

        return false;
    }

    /*
        링크를 사용할 수 있는지 확인한다.
        isExpired()와 동일한 로직을 사용하지만, 더 명확한 의미를 전달한다.
        
        @return 사용 가능하면 true, 불가능하면 false
     */
    public boolean canBeUsed() {
        return !isExpired();
    }

    /*
        링크를 사용 처리한다.
        링크를 통해 챌린지에 참여한 경우 호출한다.
        사용 횟수를 증가시키고, 1회용 링크의 경우 비활성화한다.
     */
    public void markAsUsed() {
        this.usedCount++;
        this.usedAt = LocalDateTime.now();

        // 1회용 링크는 사용 후 즉시 비활성화한다.
        if (linkType == InviteLinkType.SINGLE_USE) {
            this.active = false;
        }

        // 다중 사용 링크도 최대 사용 횟수에 도달하면 비활성화한다.
        if (linkType == InviteLinkType.MULTI_USE && maxUses != null && usedCount >= maxUses) {
            this.active = false;
        }
    }

    /*
        링크를 수동으로 비활성화한다.
        서비스에서 특정 조건(예: 링크 재발급)에 따라 링크를 비활성화할 때 사용한다.
     */
    public void deactivate() {
        this.active = false;
    }

    /*
        초대 코드 생성 로직이다.
        방 생성 시 호출되며, UUID의 앞 8자리를 사용하여 초대 코드를 생성한다.
     */
    public void generateInviteCode() {
        this.inviteCode = UUID.randomUUID().toString().substring(0, 8);  // UUID의 앞 8자리 사용
    }
}
